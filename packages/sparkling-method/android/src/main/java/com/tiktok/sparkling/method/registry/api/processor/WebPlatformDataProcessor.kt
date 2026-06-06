// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.api.processor

import com.tiktok.sparkling.method.registry.api.Utils
import com.tiktok.sparkling.method.registry.api.map
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
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class WebPlatformDataProcessor : IPlatformDataProcessor<JSONObject> {
    override fun matchPlatformType(platformType: BridgePlatformType) = platformType == BridgePlatformType.WEB

    var context: IBridgeContext? = null

    override fun transformPlatformDataToMap(
        params: JSONObject,
        clazz: Class<out IDLBridgeMethod>,
    ): Map<String, Any?>? {
        val pool =
            (IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(context?.containerID))?.BRIDGE_ANNOTATION_MAP?.get(
                clazz,
            )
        if (pool != null) {
            return WebProcessorForMap.getJavaOnlyMapParams(params, pool)
        } else {
            val modelClazz = IDLProxyClient.retrieveParamModel(clazz) ?: return null
            return getJsonObjectParams(params, modelClazz)
        }
    }

    private fun getJsonObjectParams(
        params: JSONObject,
        clazz: Class<out IDLMethodBaseParamModel>,
    ): Map<String, Any?>? {
        val classMap = preCheck(clazz, params) ?: return null
        return params.mapValues {
            val annotation = classMap[it.first]?.second
            val value = it.second
            convertValueWithAnnotation(value, annotation)
        }
    }

    private fun preCheck(
        clazz: Class<out IDLMethodBaseModel>?,
        params: JSONObject,
    ): HashMap<String, Pair<Method, IDLMethodParamField>>? {
        val classMap =
            clazz?.declaredMethods?.fold(hashMapOf<String, Pair<Method, IDLMethodParamField>>()) { map, method ->
                val annotation = method.getAnnotation(IDLMethodParamField::class.java)
                if (annotation != null) {
                    map[annotation.keyPath] = Pair<Method, IDLMethodParamField>(method, annotation)
                }
                map
            } ?: return null
        // Init default value.
        classMap.filter { !params.has(it.key) && it.value.second.defaultValue.type != DefaultType.NONE }.forEach {
            val annotation = it.value.second
            params.put(it.key, parseStringByReturnType(it.value.first, annotation))
        }
        checkValue(classMap, params)
        return classMap
    }

    private fun checkValue(
        classMap: HashMap<String, Pair<Method, IDLMethodParamField>>,
        params: JSONObject,
    ) {
        // Check value.
        classMap.forEach {
            val field = it.value.second
            val method = it.value.first
            val value = params.opt(it.key)
            if (field.required && (value == null || value == JSONObject.NULL)) {
                throw IllegalInputParamException("${it.key} param is missing from input")
            }
            when (method.returnType) {
                String::class.java -> {
                    if (value != null && value != JSONObject.NULL && value !is String) {
                        throw IllegalInputParamException("${it.key} param has wrong declared type. except string,but ${value.javaClass}")
                    }
                }

                Number::class.java -> {
                    if (value != null && value != JSONObject.NULL && value !is Number) {
                        throw IllegalInputParamException("${it.key} param has wrong declared type. except number,but ${value.javaClass}")
                    }
                }

                java.lang.Boolean::class.java, Boolean::class.java -> {
                    if (value != null && value != JSONObject.NULL && value !is Boolean) {
                        throw IllegalInputParamException("${it.key} param has wrong declared type. except boolean,but ${value.javaClass}")
                    }
                }

                List::class.java -> {
                    if (value != null && value != JSONObject.NULL && value !is JSONArray) {
                        throw IllegalInputParamException("${it.key} param has wrong declared type. except List ,but ${value.javaClass}")
                    }
                }

                Map::class.java -> {
                    if (value != null && value != JSONObject.NULL && value !is JSONObject) {
                        throw IllegalInputParamException("${it.key} param has wrong declared type. except Map ,but ${value.javaClass}")
                    }
                }
            }
            if (value != null && value != JSONObject.NULL && field.isEnum) {
                when (method.returnType) {
                    String::class.java -> {
                        val stringEnum = method.getAnnotation(IDLMethodStringEnum::class.java)
                        val option = stringEnum.option
                        if (!option.contains(value)) {
                            throw IllegalInputParamException("${it.key} has wrong type.should be one of ${option.asList()} but got $value")
                        }
                    }

                    Number::class.java -> {
                        val stringEnum = method.getAnnotation(IDLMethodIntEnum::class.java)
                        val option = stringEnum.option
                        if (!option.contains(getInt(value))) {
                            throw IllegalInputParamException("${it.key} has wrong value.should be one of ${option.asList()} but got $value")
                        }
                    }

                    Map::class.java -> {
                        val stringEnum = method.getAnnotation(IDLMethodStringEnum::class.java)
                        if (stringEnum != null) {
                            val option = stringEnum.option
                            (value as JSONObject).mapValues { item ->
                                if (!option.contains(item.second)) {
                                    throw IllegalInputParamException("${it.key} has wrong value.should be one of ${option.asList()} but got $value")
                                }
                            }
                        } else {
                            val intEnum = method.getAnnotation(IDLMethodIntEnum::class.java)
                            if (intEnum != null) {
                                val option = intEnum.option
                                (value as JSONObject).mapValues { item ->
                                    if (!option.contains(getInt(item.second))) {
                                        throw IllegalInputParamException("${it.key} has wrong value.should be one of ${option.asList()} but got $value")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * front end give 1 it may receive 1.0!
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

    private fun <R> JSONObject.mapValues(op: (Pair<String, Any>) -> R): Map<String, R> {
        val map = hashMapOf<String, R>()
        this.keys().forEach {
            map[it] = op(Pair(it, this.opt(it)))
        }
        return map
    }

    private fun proxyValue(
        clazz: Class<out IDLMethodBaseModel>?,
        map: JSONObject,
    ): Any? {
        if (clazz == null) return null
        preCheck(clazz, map) ?: return null
        return Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz)) { _, method, _ ->
            if (method.name == "toJSON") {
                return@newProxyInstance getMapWithDefault(clazz, map)
            }
            if (method.name == "toString") {
                return@newProxyInstance map.toString()
            }
            val annotation = method.getAnnotation(IDLMethodParamField::class.java)
            val value = map.opt(annotation.keyPath)
            return@newProxyInstance convertValueWithAnnotation(value, annotation)
        }
    }

    private fun convertValueWithAnnotation(
        value: Any?,
        annotation: IDLMethodParamField?,
    ) = if (isNestClass(value, annotation)) {
        proxyValue(annotation?.nestedClassType?.java, value as JSONObject)
    } else if (isNestListClass(value, annotation)) {
        (value as JSONArray).map {
            proxyValue(annotation?.nestedClassType?.java, it as JSONObject)
        }
    } else {
        when (value) {
            is JSONArray -> {
                Utils.jsonToList(value)
            }

            is JSONObject -> {
                Utils.jsonToMap(value)
            }

            JSONObject.NULL -> {
                null
            }

            else -> {
                value
            }
        }
    }

    private fun isNestClass(
        value: Any?,
        annotation: IDLMethodParamField?,
    ) = value is JSONObject && annotation?.nestedClassType != IDLMethodBaseModel.Default::class

    private fun isNestListClass(
        value: Any?,
        annotation: IDLMethodParamField?,
    ) = value is JSONArray && annotation?.nestedClassType != IDLMethodBaseModel.Default::class

    private fun getMapWithDefault(
        clazz: Class<out IDLMethodBaseModel>?,
        json: JSONObject,
    ): JSONObject {
        val methods = clazz?.declaredMethods?.filter { it.getAnnotation(IDLMethodParamField::class.java)?.isGetter == true }
        return methods?.fold(JSONObject()) { acc, method ->
            val annotation = method.getAnnotation(IDLMethodParamField::class.java)

            val value = json.opt(annotation.keyPath)

            // Init default value.
            if ((value == null || value == JSONObject.NULL) && annotation.defaultValue.type != DefaultType.NONE) {
                val defaultValue = parseStringByReturnType(method, annotation)
                json.put(annotation.keyPath, defaultValue)
            }
            val nestedClassType = annotation.nestedClassType
            acc.put(
                annotation.keyPath,
                if (annotation.nestedClassType != IDLMethodBaseModel.Default::class && value is JSONObject) {
                    getMapWithDefault(nestedClassType.java, value)
                } else if (annotation.nestedClassType != IDLMethodBaseModel.Default::class && value is JSONArray) {
                    value.map { getMapWithDefault(nestedClassType.java, it as JSONObject) }
                } else {
                    json.opt(annotation.keyPath)
                },
            )
            acc
        } ?: JSONObject()
    }

    private fun MutableMap<String, Any>.initDefaultValue(getters: List<Method>) {
        val defaultMethods = getters.filter { it.getAnnotation(IDLMethodParamField::class.java).defaultValue.type != DefaultType.NONE }
        defaultMethods.forEach {
            val annotation = it.getAnnotation(IDLMethodParamField::class.java)
            put(
                annotation.keyPath,
                parseStringByReturnType(it, annotation),
            )
        }
    }

    private fun getJsonWithDefault(
        clazz: Class<out IDLMethodBaseModel>?,
        map: JSONObject,
    ): JSONObject? {
        val methods = clazz?.declaredMethods?.filter { it.getAnnotation(IDLMethodParamField::class.java)?.isGetter == true }
        return methods?.fold(JSONObject()) { acc, method ->
            val annotation = method.getAnnotation(IDLMethodParamField::class.java)

            val value = map.opt(annotation.keyPath)

            // Init default value.
            if ((value == null || value == JSONObject.NULL) && annotation.defaultValue.type != DefaultType.NONE) {
                val defaultValue = parseStringByReturnType(method, annotation)
                map.put(annotation.keyPath, defaultValue)
            }

            acc.put(
                annotation.keyPath,
                if (annotation.nestedClassType != IDLMethodBaseModel.Default::class && value != null && value != JSONObject.NULL) {
                    val nestedClassType = annotation.nestedClassType
                    getJsonWithDefault(nestedClassType.java, value as JSONObject)
                } else {
                    map.opt(annotation.keyPath)
                },
            )
            acc
        }
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

    override fun transformMapToPlatformData(
        params: Map<String, Any?>,
        clazz: Class<out IDLBridgeMethod>,
    ): JSONObject = Utils.mapToJSON(params)
}
