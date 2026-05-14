// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.api.processor

import com.tiktok.sparkling.method.registry.api.Utils
import com.lynx.react.bridge.ReadableArray
import com.lynx.react.bridge.ReadableMap
import com.tiktok.sparkling.method.registry.core.IBridgeContext
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.IDLMethodRegistryCacheManager
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IDLProxyClient
import com.tiktok.sparkling.method.registry.core.annotation.DefaultType
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodIntEnum
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamField
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodStringEnum
import com.tiktok.sparkling.method.registry.core.interfaces.IPlatformDataProcessor
import com.tiktok.sparkling.method.registry.core.exception.IllegalInputParamException
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseParamModel
import org.json.JSONObject
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class LynxPlatformDataProcessor : IPlatformDataProcessor<ReadableMap> {
    var context: IBridgeContext? = null

    override fun matchPlatformType(platformType: BridgePlatformType) = platformType == BridgePlatformType.LYNX

    @Throws(IllegalInputParamException::class)
    override fun transformPlatformDataToMap(
        params: ReadableMap,
        clazz: Class<out IDLBridgeMethod>,
    ): Map<String, Any?>? {
        val pool = (IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(context?.containerID))?.BRIDGE_ANNOTATION_MAP?.get(clazz)
        return if (pool != null) {
            LynxDataProcessorForMap.getJavaOnlyMapParams(params.toHashMap(), pool)
        } else {
            println("idl ReadableMap->Map. no cache")
            val modelClazz = IDLProxyClient.retrieveParamModel(clazz) ?: return null
            getJavaOnlyMapParams(params.toHashMap(), modelClazz)
        }
    }

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
            val annotation = method.getAnnotation(IDLMethodParamField::class.java)
            val value = map[annotation.keyPath]
            return@newProxyInstance convertValueWithAnnotation(value, annotation)
        }
    }

    private fun convertValueWithAnnotation(
        value: Any?,
        annotation: IDLMethodParamField?,
    ): Any? {
        val result =
            if (isNestClass(value, annotation)) {
                proxyValue(annotation?.nestedClassType?.java, (value as ReadableMap).toHashMap()/* = java.util.HashMap<kotlin.String, kotlin.Any> */)
            } else if (isNestListClass(value, annotation)) {
                (value as List<*>).map {
                    proxyValue(annotation?.nestedClassType?.java, (it as ReadableMap).toHashMap())
                }
            } else {
                Utils.getValue(value)
            }
        return result
    }

    private fun checkValue(
        classMap: HashMap<String, Pair<Method, IDLMethodParamField>>,
        params: HashMap<String, Any>,
    ) {
        /**
         * check value
         */
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
                        val stringEnum = method.getAnnotation(IDLMethodStringEnum::class.java)
                        val option = stringEnum.option
                        if (!option.contains(value)) {
                            throw IllegalInputParamException("${it.key} has wrong type.should be one of ${option.asList()} but got $value")
                        }
                    }

                    Number::class.java -> {
                        val intEnum = method.getAnnotation(IDLMethodIntEnum::class.java)
                        val option = intEnum.option
                        if (!option.contains(getInt(value))) {
                            throw IllegalInputParamException("${it.key} has wrong value.should be one of ${option.asList()} but got $value")
                        }
                    }

                    Map::class.java -> {
                        val stringEnum = method.getAnnotation(IDLMethodStringEnum::class.java)
                        if (stringEnum != null) {
                            val option = stringEnum.option
                            if ((value as Map<String, Any>).any { item -> !option.contains(item.value) }) {
                                throw IllegalInputParamException("${it.key} has wrong type.should be one of ${option.asList()} but got $value")
                            }
                        } else {
                            val intEnum = method.getAnnotation(IDLMethodIntEnum::class.java)
                            if (intEnum != null) {
                                val option = intEnum.option
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
        annotation: IDLMethodParamField?,
    ) = value is Map<*, *> && annotation?.nestedClassType != IDLMethodBaseModel.Default::class

    private fun isNestListClass(
        value: Any?,
        annotation: IDLMethodParamField?,
    ) = value is List<*> && annotation?.nestedClassType != IDLMethodBaseModel.Default::class

    private fun preCheck(
        clazz: Class<out IDLMethodBaseModel>?,
        map: HashMap<String, Any>,
    ): HashMap<String, Pair<Method, IDLMethodParamField>>? {
        val classMap =
            clazz?.declaredMethods?.fold(hashMapOf<String, Pair<Method, IDLMethodParamField>>()) { _map, method ->
                val annotation = method.getAnnotation(IDLMethodParamField::class.java)
                if (annotation != null) {
                    _map[annotation.keyPath] = Pair<Method, IDLMethodParamField>(method, annotation)
                }
                _map
            } ?: return null
        /**
         * init default value
         */
        classMap.filter { map[it.key] == null && it.value.second.defaultValue.type != DefaultType.NONE }.forEach {
            val annotation = it.value.second
            map[it.key] = parseStringByReturnType(it.value.first, annotation)
        }
        checkValue(classMap, map)
        return classMap
    }

    private fun parseStringByReturnType(
        method: Method,
        annotation: IDLMethodParamField,
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
        val methods = clazz?.declaredMethods?.filter { it.getAnnotation(IDLMethodParamField::class.java)?.isGetter == true }
        return methods?.fold(hashMapOf()) { acc, method ->
            val annotation = method.getAnnotation(IDLMethodParamField::class.java)
            val value = map[annotation.keyPath]
            /**
             * init default value
             */
            if (value == null && annotation.defaultValue.type != DefaultType.NONE) {
                val defaultValue = parseStringByReturnType(method, annotation)
                map[annotation.keyPath] = defaultValue
            }

            acc[annotation.keyPath] =
                if (annotation.nestedClassType != IDLMethodBaseModel.Default::class && value is ReadableMap) {
                    val nestedClassType = annotation.nestedClassType
                    getMapWithDefault(nestedClassType.java, value.toHashMap())
                } else if (annotation.nestedClassType != IDLMethodBaseModel.Default::class && value is ReadableArray) {
                    value.toArrayList().map { getMapWithDefault(annotation.nestedClassType.java, (it as ReadableMap).toHashMap()) }
                } else {
                    map[annotation.keyPath]
                }
            acc
        }
    }

    override fun transformMapToPlatformData(
        params: Map<String, Any?>,
        clazz: Class<out IDLBridgeMethod>,
    ): ReadableMap = Utils.convertMapToReadableMap(params)
}
