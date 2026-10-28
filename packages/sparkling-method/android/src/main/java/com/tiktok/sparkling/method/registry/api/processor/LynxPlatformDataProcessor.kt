// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.api.processor

import com.tiktok.sparkling.method.registry.api.Utils
import com.lynx.react.bridge.ReadableArray
import com.lynx.react.bridge.ReadableMap
import com.tiktok.sparkling.method.registry.core.DefaultIDLParamModelReader
import com.tiktok.sparkling.method.registry.core.IBridgeContext
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IBridgePlatformType
import com.tiktok.sparkling.method.registry.core.IDLParamField
import com.tiktok.sparkling.method.registry.core.IDLParamModelReader
import com.tiktok.sparkling.method.registry.core.annotation.DefaultType
import com.tiktok.sparkling.method.registry.core.interfaces.IPlatformDataProcessor
import com.tiktok.sparkling.method.registry.core.exception.IllegalInputParamException
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseParamModel
import org.json.JSONObject
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

/**
 * Binds a Lynx [ReadableMap] to a method's param model, and renders a result map back as one.
 *
 * Every annotation read goes through [reader], so a host whose method classes carry their own
 * annotation set supplies a reader rather than a copy of this class. The protected members are the
 * remaining host seams: which sentinel a cached field may carry and how a plain value is converted.
 */
open class LynxPlatformDataProcessor(
    private val reader: IDLParamModelReader = DefaultIDLParamModelReader,
) : IPlatformDataProcessor<ReadableMap> {
    var context: IBridgeContext? = null

    private val cachedBinding: LynxDataProcessorForMap by lazy {
        LynxDataProcessorForMap(::isCachedDefaultNestedType, ::convertValue)
    }

    override fun matchPlatformType(platformType: IBridgePlatformType) = platformType == BridgePlatformType.LYNX

    @Throws(IllegalInputParamException::class)
    override fun transformPlatformDataToMap(
        params: ReadableMap,
        clazz: Class<out IDLBridgeMethod>,
    ): Map<String, Any?>? {
        val pool = reader.findAnnotationData(context?.containerID, clazz)
        return if (pool != null) {
            cachedBinding.getJavaOnlyMapParams(params.toHashMap(), pool)
        } else {
            println("idl ReadableMap->Map. no cache")
            val modelClazz = reader.retrieveParamModel(clazz) ?: return null
            getJavaOnlyMapParams(params.toHashMap(), modelClazz)
        }
    }

    /**
     * Whether a field read from the annotation cache has no nested model.
     *
     * Separate from [IDLParamModelReader.isDefaultNestedType] because the cache may hold fields
     * declared with annotations the reader does not read, which carry their own sentinel.
     */
    protected open fun isCachedDefaultNestedType(type: KClass<out IDLMethodBaseModel>): Boolean =
        type == IDLMethodBaseModel.Default::class

    /** Converts a value that is not a nested model. */
    protected open fun convertValue(value: Any?): Any? = Utils.getValue(value)

    private fun getJavaOnlyMapParams(
        params: HashMap<String, Any>,
        clazz: Class<out IDLMethodBaseParamModel>,
    ): Map<String, Any?>? {
        val classMap = preCheck(clazz, params) ?: return null
        return params.mapValues {
            val annotation = classMap[it.key]?.second
            val value = it.value
            convertValueWithAnnotation(value, annotation)
        }
    }

    @Throws(IllegalInputParamException::class)
    private fun proxyValue(
        clazz: Class<out IDLMethodBaseModel>?,
        map: HashMap<String, Any>,
    ): Any? {
        if (clazz == null) return null
        preCheck(clazz, map) ?: return null
        return Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz)) { _, method, _ ->
            if (method.name == "toJSON") {
                val fold = getMapWithDefault(clazz, map)
                return@newProxyInstance JSONObject(fold)
            }
            if (method.name == "toString") {
                return@newProxyInstance map.toString()
            }
            val annotation = requireParamField(method)
            val value = map[annotation.keyPath]
            return@newProxyInstance convertValueWithAnnotation(value, annotation)
        }
    }

    private fun convertValueWithAnnotation(
        value: Any?,
        annotation: IDLParamField?,
    ): Any? {
        val result =
            if (isNestClass(value, annotation)) {
                proxyValue(annotation?.nestedClassType?.java, (value as ReadableMap).toHashMap()/* = java.util.HashMap<kotlin.String, kotlin.Any> */)
            } else if (isNestListClass(value, annotation)) {
                (value as List<*>).map {
                    proxyValue(annotation?.nestedClassType?.java, (it as ReadableMap).toHashMap())
                }
            } else {
                convertValue(value)
            }
        return result
    }

    private fun checkValue(
        classMap: HashMap<String, Pair<Method, IDLParamField>>,
        params: HashMap<String, Any>,
    ) {
        // Check value.
        classMap.forEach {
            val field = it.value.second
            val method = it.value.first
            val value = params[it.key]
            if (field.required && value == null) {
                throw IllegalInputParamException("${it.key} param is missing from input")
            }
            when (method.returnType) {
                String::class.java -> {
                    if (value != null && value !is String) {
                        throw IllegalInputParamException("${it.key} param has wrong declared type. except string,but ${value.javaClass}")
                    }
                }

                Number::class.java -> {
                    if (value != null && value !is Number) {
                        throw IllegalInputParamException("${it.key} param has wrong declared type. except number,but ${value.javaClass}")
                    }
                }

                java.lang.Boolean::class.java, Boolean::class.java -> {
                    if (value != null && value !is Boolean) {
                        throw IllegalInputParamException("${it.key} param has wrong declared type. except boolean,but ${value.javaClass}")
                    }
                }

                List::class.java -> {
                    if (value != null && value !is List<*>) {
                        throw IllegalInputParamException("${it.key} param has wrong declared type. except List ,but ${value.javaClass}")
                    }
                }

                Map::class.java -> {
                    if (value != null && value !is Map<*, *>) {
                        throw IllegalInputParamException("${it.key} param has wrong declared type. except Map ,but ${value.javaClass}")
                    }
                }
            }
            if (value != null && field.isEnum) {
                when (method.returnType) {
                    String::class.java -> {
                        val option = requireStringEnumOptions(method)
                        if (!option.contains(value)) {
                            throw IllegalInputParamException("${it.key} has wrong type.should be one of ${option.asList()} but got $value")
                        }
                    }

                    Number::class.java -> {
                        val option = requireIntEnumOptions(method)
                        if (!option.contains(getInt(value))) {
                            throw IllegalInputParamException("${it.key} has wrong value.should be one of ${option.asList()} but got $value")
                        }
                    }

                    Map::class.java -> {
                        val stringOption = reader.readStringEnumOptions(method)
                        if (stringOption != null) {
                            val option = stringOption
                            if ((value as Map<String, Any>).any { item -> !option.contains(item.value) }) {
                                throw IllegalInputParamException("${it.key} has wrong type.should be one of ${option.asList()} but got $value")
                            }
                        } else {
                            val intOption = reader.readIntEnumOptions(method)
                            if (intOption != null) {
                                val option = intOption
                                if ((value as Map<String, Any>).any { item -> !option.contains(getInt(item.value)) }) {
                                    throw IllegalInputParamException("${it.key} has wrong value.should be one of ${option.asList()} but got $value")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * front end give 1 but lynx receive 1.0!
     * let 1.0==1
     */
    private fun getInt(data: Any?): Int {
        if (data is Number) {
            return data.toInt()
        }
        if (data == null) {
            throw IllegalInputParamException("the key is null")
        }
        throw IllegalInputParamException("the key is not a number")
    }

    private fun isNestClass(
        value: Any?,
        annotation: IDLParamField?,
    ) = value is Map<*, *> && !reader.isDefaultNestedType(annotation?.nestedClassType)

    private fun isNestListClass(
        value: Any?,
        annotation: IDLParamField?,
    ) = value is List<*> && !reader.isDefaultNestedType(annotation?.nestedClassType)

    private fun preCheck(
        clazz: Class<out IDLMethodBaseModel>?,
        map: HashMap<String, Any>,
    ): HashMap<String, Pair<Method, IDLParamField>>? {
        val classMap =
            clazz?.declaredMethods?.fold(hashMapOf<String, Pair<Method, IDLParamField>>()) { _map, method ->
                val annotation = reader.readParamField(method)
                if (annotation != null) {
                    _map[annotation.keyPath] = Pair<Method, IDLParamField>(method, annotation)
                }
                _map
            } ?: return null
        // Init default value.
        classMap.filter { map[it.key] == null && it.value.second.defaultValue.type != DefaultType.NONE }.forEach {
            val annotation = it.value.second
            map[it.key] = parseStringByReturnType(it.value.first, annotation)
        }
        checkValue(classMap, map)
        return classMap
    }

    private fun parseStringByReturnType(
        method: Method,
        annotation: IDLParamField,
    ) = when (method.returnType) {
        Number::class.java -> {
            when (annotation.defaultValue.type) {
                DefaultType.DOUBLE -> annotation.defaultValue.doubleValue
                DefaultType.LONG -> annotation.defaultValue.longValue
                DefaultType.INT -> annotation.defaultValue.intValue
                else -> annotation.defaultValue.intValue
            }
        }

        Boolean::class.java, java.lang.Boolean::class.java -> {
            annotation.defaultValue.boolValue
        }

        else -> {
            annotation.defaultValue.stringValue
        }
    }

    private fun getMapWithDefault(
        clazz: Class<out IDLMethodBaseModel>?,
        map: HashMap<String, Any>,
    ): HashMap<String, Any?>? {
        val methods = clazz?.declaredMethods?.filter { reader.readParamField(it)?.isGetter == true }
        return methods?.fold(hashMapOf()) { acc, method ->
            val annotation = requireParamField(method)
            val value = map[annotation.keyPath]
            // Init default value.
            if (value == null && annotation.defaultValue.type != DefaultType.NONE) {
                val defaultValue = parseStringByReturnType(method, annotation)
                map[annotation.keyPath] = defaultValue
            }

            acc[annotation.keyPath] =
                if (!reader.isDefaultNestedType(annotation.nestedClassType) && value is ReadableMap) {
                    val nestedClassType = annotation.nestedClassType
                    getMapWithDefault(nestedClassType.java, value.toHashMap())
                } else if (!reader.isDefaultNestedType(annotation.nestedClassType) && value is ReadableArray) {
                    value.toArrayList().map { getMapWithDefault(annotation.nestedClassType.java, (it as ReadableMap).toHashMap()) }
                } else {
                    map[annotation.keyPath]
                }
            acc
        }
    }

    /*
     * The three reads below fail the way reading a missing annotation directly does: with a
     * NullPointerException, which the caller reports as an unknown error.
     */

    private fun requireParamField(method: Method): IDLParamField {
        return reader.readParamField(method)
            ?: throw NullPointerException("${method.name} carries no param field annotation")
    }

    private fun requireStringEnumOptions(method: Method): Array<out String> {
        return reader.readStringEnumOptions(method)
            ?: throw NullPointerException("${method.name} carries no string enum annotation")
    }

    private fun requireIntEnumOptions(method: Method): IntArray {
        return reader.readIntEnumOptions(method)
            ?: throw NullPointerException("${method.name} carries no int enum annotation")
    }

    override fun transformMapToPlatformData(
        params: Map<String, Any?>,
        clazz: Class<out IDLBridgeMethod>,
    ): ReadableMap = Utils.convertMapToReadableMap(params)
}
