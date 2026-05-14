// Copyright (c) 2023 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

import com.tiktok.sparkling.method.registry.api.BridgeSettings
import com.tiktok.sparkling.method.registry.api.util.ThreadPool
import com.tiktok.sparkling.method.protocol.impl.errors.JSBErrorReportModel
import com.tiktok.sparkling.method.protocol.utils.LogUtils
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodIntEnum
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodName
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamField
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamModel
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodResultModel
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodStringEnum
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import java.util.concurrent.ConcurrentHashMap

/**
 */
class IDLMethodRegistryCache {
    private val TAG = "IDLMethodRegistryCache"
    private val cache: MutableMap<Class<out IDLBridgeMethod>, String> = mutableMapOf()
    val BRIDGE_ANNOTATION_MAP: ConcurrentHashMap<Class<out IDLBridgeMethod>, IDLAnnotationData> = ConcurrentHashMap()
    val BRIDGE_RESULT_MAP: ConcurrentHashMap<Class<*>, Class<*>> = ConcurrentHashMap()

    fun find(clazz: Class<out IDLBridgeMethod>): String =
        try {
            var name = cache[clazz]
            if (name == null) {
                ThreadPool.runInBackGround(Runnable { addAnnotationCache(clazz) })
                if (!BridgeSettings.bridgeRegistryOptimize) {
                    clazz.newInstance().run {
                        cache.put(clazz, this.name)
                    }
                    name = cache[clazz]
                } else {
                    clazz.superclass.declaredFields
                        .find {
                            it.getAnnotation(IDLMethodName::class.java) != null
                        }?.getAnnotation(IDLMethodName::class.java)
                        ?.name
                        ?.let {
                            cache[clazz] = it
                            name = cache[clazz]
                        }
                }
            }
            name ?: ""
        } catch (e: Throwable) {
            val errorMsg = "IDLMethodRegistryCache.find failed,clazz == ${clazz.name}, e == ${e.message}"
            LogUtils.e(TAG, errorMsg)
            JSBErrorReportModel.putGlobalExtension("register_error_${clazz.name}", errorMsg)
            ""
        }

    private fun addAnnotationCache(clazz: Class<out IDLBridgeMethod>) {
//        println("run in ${Thread.currentThread().name}")
        val annotationData = annotationCache(clazz)
        if (annotationData != null) {
            BRIDGE_ANNOTATION_MAP[clazz] = annotationData
            BRIDGE_RESULT_MAP[annotationData.resultClass] = clazz
        } else {
            LogUtils.e(TAG, "IDLMethodRegistryCache.addAnnotationCache failed, clazz == $clazz")
        }
    }

    fun getAnnotationDataByResultClass(resultModelClazz: Class<*>): IDLAnnotationData? {
        val clazz = BRIDGE_RESULT_MAP[resultModelClazz] ?: return null
        return BRIDGE_ANNOTATION_MAP[clazz]
    }

    private fun annotationCache(clazz: Class<out IDLBridgeMethod>): IDLAnnotationData? {
        val paramModelClass = findModelClassByAnnotation(clazz, IDLMethodParamModel::class.java) ?: return null
        val resultModelClass = findModelClassByAnnotation(clazz, IDLMethodResultModel::class.java) ?: return null
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

    private fun findModelClassByAnnotation(
        clazz: Class<out IDLBridgeMethod>,
        annotation: Class<out Annotation>,
    ): Class<*>? =
        try {
            clazz.superclass.declaredClasses.find { it.getAnnotation(annotation) != null }
                ?: clazz.declaredClasses.find { it.getAnnotation(annotation) != null }
        } catch (e: Throwable) {
            LogUtils.e(TAG, "findModelClassByAnnotation failed, class == ${clazz.name}, e == ${e.message}")
            null
        }

    private fun getIDLParamField(
        paramModelClass: Class<*>,
        models: HashMap<Class<out IDLMethodBaseModel>, IDLAnnotationModel>,
    ): IDLAnnotationModel {
        val methods = paramModelClass.declaredMethods
        return methods.fold(IDLAnnotationModel()) { acc, method ->
            val annotation = method.getAnnotation(IDLMethodParamField::class.java)
            if (annotation != null) {
                val defaultValue = annotation.defaultValue
                val stringEnum = method.getAnnotation(IDLMethodStringEnum::class.java)
                val intEnum = method.getAnnotation(IDLMethodIntEnum::class.java)
                val nestedClassType = annotation.nestedClassType
                val keyPath = annotation.keyPath
                if (nestedClassType != IDLMethodBaseModel.Default::class) {
                    if (!models.containsKey(nestedClassType.java) && !acc.stringModel.containsKey(keyPath)) {
                        models[nestedClassType.java] = getIDLParamField(nestedClassType.java, models)
                    }
                }

                val idlParamField =
                    IDLParamField(
                        annotation.required,
                        keyPath,
                        nestedClassType,
                        annotation.primitiveClassType,
                        annotation.isEnum,
                        annotation.isGetter,
                        IDLDefaultValue(
                            defaultValue.type,
                            defaultValue.doubleValue,
                            defaultValue.stringValue,
                            defaultValue.intValue,
                            defaultValue.boolValue,
                            defaultValue.longValue,
                        ),
                        method.returnType,
                        stringEnum?.option?.toList() ?: listOf(),
                        intEnum?.option?.toList() ?: listOf(),
                    )
                acc.methodModel[method] = idlParamField
                if (annotation.isGetter) {
                    acc.stringModel[keyPath] = idlParamField
                }
            }
            acc
        }
    }
}
