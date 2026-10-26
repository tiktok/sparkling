// Copyright (c) 2023 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

import com.tiktok.sparkling.method.protocol.utils.LogUtils
import com.tiktok.sparkling.method.registry.api.util.ThreadPool
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodIntEnum
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodName
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamField
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamModel
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodResultModel
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodStringEnum
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Reads a method class once through reflection and remembers it: the name it answers to, and the
 * shape of its param and result models.
 *
 * Every read of an annotation goes through a protected seam, because a host can carry method
 * classes annotated with its own annotation set. TikTok does: its prebuilt method classes name
 * `com.bytedance.sdk.xbridge.registry.core.annotation` in their bytecode, so those classes are
 * invisible to the annotations this class reads, and the TikTok subclass reads both.
 */
open class IDLMethodRegistryCache {

    protected open val TAG = "IDLMethodRegistryCache"

    private val cache: MutableMap<Class<out IDLBridgeMethod>, String> = ConcurrentHashMap()

    val BRIDGE_ANNOTATION_MAP: ConcurrentHashMap<Class<out IDLBridgeMethod>, IDLAnnotationData> =
        ConcurrentHashMap()

    val BRIDGE_RESULT_MAP: ConcurrentHashMap<Class<*>, Class<*>> = ConcurrentHashMap()

    /**
     * The name [clazz] answers to, or an empty string when it cannot be resolved at all.
     *
     * Reading the name also schedules the model shapes to be read, since a class asked for its
     * name is about to be registered and will be asked to bind a payload soon after.
     */
    fun find(clazz: Class<out IDLBridgeMethod>): String {
        return try {
            var name = cache[clazz]
            if (name == null) {
                cacheMethodClass(clazz)
                name = readMethodName(clazz)
                if (name != null) {
                    cache[clazz] = name
                }
            }
            if (name.isNullOrEmpty()) {
                name = onNameUnresolved(clazz)
                if (!name.isNullOrEmpty()) {
                    cache[clazz] = name
                }
            }
            name ?: ""
        } catch (e: Throwable) {
            val errorMsg = "$TAG.find failed,clazz == ${clazz.name}, e == ${e.message}"
            LogUtils.e(TAG, errorMsg)
            onFindFailed(clazz, errorMsg, e)
            ""
        }
    }

    /**
     * Reads and stores the model shapes of [clazz] on the configured background executor,
     * including when a caller uses the shared base cache directly.
     */
    open fun cacheMethodClass(clazz: Class<out IDLBridgeMethod>) {
        ThreadPool.runInBackGround(Runnable { addAnnotationCache(clazz) })
    }

    /** Everything known about the method class that produces [resultModelClazz]. */
    fun getAnnotationDataByResultClass(resultModelClazz: Class<*>): IDLAnnotationData? {
        val clazz = BRIDGE_RESULT_MAP[resultModelClazz] ?: return null
        return BRIDGE_ANNOTATION_MAP[clazz]
    }

    protected fun addAnnotationCache(clazz: Class<out IDLBridgeMethod>) {
        val annotationData = annotationCache(clazz)
        if (annotationData != null) {
            BRIDGE_ANNOTATION_MAP[clazz] = annotationData
            BRIDGE_RESULT_MAP[annotationData.resultClass] = clazz
        } else {
            LogUtils.e(TAG, "$TAG.addAnnotationCache failed, clazz == $clazz")
        }
    }

    /** The name declared on [clazz], read off the annotated field the code generator emits. */
    protected open fun readMethodName(clazz: Class<out IDLBridgeMethod>): String? {
        return readNameFrom(clazz, IDLMethodName::class.java) { it.name }
    }

    /**
     * Last resort when no annotation carried a name. Returns null here; TikTok instantiates the
     * class and asks it.
     */
    protected open fun onNameUnresolved(clazz: Class<out IDLBridgeMethod>): String? = null

    /** Called after [find] has already logged. TikTok also reports the failure. */
    protected open fun onFindFailed(
        clazz: Class<out IDLBridgeMethod>,
        errorMsg: String,
        e: Throwable,
    ) = Unit

    /** Whether [type] is the sentinel meaning "this field holds no nested model". */
    protected open fun isDefaultNestedType(type: KClass<out IDLMethodBaseModel>): Boolean {
        return type == IDLMethodBaseModel.Default::class
    }

    protected open fun findParamModelClass(clazz: Class<out IDLBridgeMethod>): Class<*>? {
        return findModelClassByAnnotation(clazz, IDLMethodParamModel::class.java)
    }

    protected open fun findResultModelClass(clazz: Class<out IDLBridgeMethod>): Class<*>? {
        return findModelClassByAnnotation(clazz, IDLMethodResultModel::class.java)
    }

    /** Describes one getter of a model class, or null when it carries no field annotation. */
    protected open fun readParamField(method: Method): IDLParamField? {
        val annotation = method.getAnnotation(IDLMethodParamField::class.java) ?: return null
        return DefaultIDLParamModelReader.toIDLParamField(
            annotation,
            method.returnType,
            method.getAnnotation(IDLMethodStringEnum::class.java)?.option?.toList() ?: listOf(),
            method.getAnnotation(IDLMethodIntEnum::class.java)?.option?.toList() ?: listOf(),
        )
    }

    /**
     * Finds the nested class of [clazz] carrying [annotation], looking at the generated superclass
     * before the class itself.
     */
    protected fun findModelClassByAnnotation(
        clazz: Class<out IDLBridgeMethod>,
        annotation: Class<out Annotation>,
    ): Class<*>? {
        return try {
            clazz.superclass.declaredClasses.find { it.getAnnotation(annotation) != null }
                ?: clazz.declaredClasses.find { it.getAnnotation(annotation) != null }
        } catch (e: Throwable) {
            LogUtils.e(TAG, "findModelClassByAnnotation failed, class == ${clazz.name}, e == ${e.message}")
            null
        }
    }

    /** Reads [annotationClass] off a field of the generated superclass, then of [clazz] itself. */
    protected fun <T : Annotation> readNameFrom(
        clazz: Class<out IDLBridgeMethod>,
        annotationClass: Class<T>,
        nameOf: (T) -> String,
    ): String? {
        val fromSuperclass = clazz.superclass.declaredFields
            .find { it.getAnnotation(annotationClass) != null }
            ?.getAnnotation(annotationClass)
        val annotation = fromSuperclass
            ?: clazz.declaredFields
                .find { it.getAnnotation(annotationClass) != null }
                ?.getAnnotation(annotationClass)
        return annotation?.let(nameOf)
    }

    private fun annotationCache(clazz: Class<out IDLBridgeMethod>): IDLAnnotationData? {
        val paramModelClass = findParamModelClass(clazz) ?: return null
        val resultModelClass = findResultModelClass(clazz) ?: return null
        val models = HashMap<Class<out IDLMethodBaseModel>, IDLAnnotationModel>()
        val paramsModels = getIDLParamField(paramModelClass, models)
        val resultModels = getIDLParamField(resultModelClass, models)
        return IDLAnnotationData(
            paramModelClass,
            resultModelClass,
            paramsModels,
            resultModels,
            models,
        )
    }

    private fun getIDLParamField(
        paramModelClass: Class<*>,
        models: HashMap<Class<out IDLMethodBaseModel>, IDLAnnotationModel>,
    ): IDLAnnotationModel {
        return paramModelClass.declaredMethods.fold(IDLAnnotationModel()) { acc, method ->
            val idlParamField = readParamField(method)
            if (idlParamField != null) {
                val nestedClassType = idlParamField.nestedClassType
                val keyPath = idlParamField.keyPath
                if (!isDefaultNestedType(nestedClassType)) {
                    if (!models.containsKey(nestedClassType.java) && !acc.stringModel.containsKey(keyPath)) {
                        models[nestedClassType.java] = getIDLParamField(nestedClassType.java, models)
                    }
                }
                acc.methodModel[method] = idlParamField
                if (idlParamField.isGetter) {
                    acc.stringModel[keyPath] = idlParamField
                }
            }
            acc
        }
    }
}
