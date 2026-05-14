// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.api.processor

import com.tiktok.sparkling.method.registry.api.Utils
import com.tiktok.sparkling.method.registry.api.map
import com.tiktok.sparkling.method.registry.core.IDLAnnotationData
import com.tiktok.sparkling.method.registry.core.IDLAnnotationModel
import com.tiktok.sparkling.method.registry.core.IDLParamField
import com.tiktok.sparkling.method.registry.core.annotation.DefaultType
import com.tiktok.sparkling.method.registry.core.exception.IllegalInputParamException
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Proxy

/**
 * @since 2021/10/13
 * @desc
 */
object WebProcessorForMap {
    fun getJavaOnlyMapParams(
        params: JSONObject,
        clazz: IDLAnnotationData,
    ): Map<String, Any?>? {
        val classMap = preCheck(clazz.methodParamModel, params) ?: return null
        return params.mapValues {
            val annotation = classMap.stringModel[it.first]
            val value = it.second
            convertValueWithAnnotation(value, annotation, clazz)
        }
    }

    private fun <R> JSONObject.mapValues(op: (Pair<String, Any>) -> R): Map<String, R> {
        val map = hashMapOf<String, R>()
        this.keys().forEach {
            map[it] = op(Pair(it, this.opt(it)))
        }
        return map
    }

    @Throws(IllegalInputParamException::class)
    private fun proxyValue(
        clazz: Class<out IDLMethodBaseModel>?,
        map: JSONObject,
        data: IDLAnnotationData,
    ): Any? {
        if (clazz == null) return null
        val clazzMap = preCheck(data.models[clazz], map) ?: return null
        return Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz)) { _, method, _ ->
            if (method.name == "toJSON") {
                return@newProxyInstance getMapWithDefault(map, data.models[clazz], data)
            }
            if (method.name == "toString") {
                return@newProxyInstance map.toString()
            }
            val annotation = clazzMap.methodModel[method]
            val value = map.opt(annotation?.keyPath)
            return@newProxyInstance convertValueWithAnnotation(value, annotation, data)
        }
    }

    private fun convertValueWithAnnotation(
        value: Any?,
        annotation: IDLParamField?,
        data: IDLAnnotationData,
    ): Any? {
        val result =
            if (isNestClass(value, annotation)) {
                proxyValue(annotation?.nestedClassType?.java, value as JSONObject, data)
            } else if (isNestListClass(value, annotation)) {
                (value as JSONArray).map {
                    proxyValue(annotation?.nestedClassType?.java, it as JSONObject, data)
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
        return result
    }

    private fun checkValue(
        classMap: IDLAnnotationModel,
        params: JSONObject,
    ) {
        /**
         * check value
         */
        classMap.stringModel.forEach {
            val field = it.key
            val method = it.value
            val value = params.opt(field)
            if (method.required && (value == null || value == JSONObject.NULL)) {
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
            if (value != null && value != JSONObject.NULL && method.isEnum) {
                when (method.returnType) {
                    String::class.java -> {
                        val option = method.stringEnum
                        if (!option.contains(value)) {
                            throw IllegalInputParamException("${it.key} has wrong type.should be one of $option but got $value")
                        }
                    }

                    Number::class.java -> {
                        val option = method.intEnum
                        if (!option.contains(getInt(value))) {
                            throw IllegalInputParamException("${it.key} has wrong value.should be one of $option but got $value")
                        }
                    }

                    Map::class.java -> {
                        val stringEnum = method.stringEnum
                        if (stringEnum.isNotEmpty()) {
                            (value as JSONObject).mapValues { item ->
                                if (!stringEnum.contains(item.second)) {
                                    throw IllegalInputParamException("${it.key} has wrong value.should be one of $stringEnum but got $value")
                                }
                            }
                        } else {
                            val intEnum = method.intEnum
                            if (intEnum.isNotEmpty()) {
                                (value as JSONObject).mapValues { item ->
                                    if (!intEnum.contains(item.second)) {
                                        throw IllegalInputParamException("${it.key} has wrong value.should be one of $intEnum but got $value")
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
    ) = value is JSONObject && annotation?.nestedClassType != IDLMethodBaseModel.Default::class

    private fun isNestListClass(
        value: Any?,
        annotation: IDLParamField?,
    ) = value is JSONArray && annotation?.nestedClassType != IDLMethodBaseModel.Default::class

    private fun preCheck(
        classMap: IDLAnnotationModel?,
        map: JSONObject,
    ): IDLAnnotationModel? {
        /**
         * init default value
         */
        if (classMap == null) return null
        classMap.stringModel.filter { (map.opt(it.key) == null || map.opt(it.key) == JSONObject.NULL) && it.value.defaultValue.type != DefaultType.NONE }.forEach {
            val idlParamField = it.value
            map.put(it.key, parseStringByReturnType(idlParamField.returnType, idlParamField))
        }
        checkValue(classMap, map)
        return classMap
    }

    private fun parseStringByReturnType(
        returnType: Class<*>,
        annotation: IDLParamField,
    ) = when (returnType) {
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
        map: JSONObject,
        model: IDLAnnotationModel?,
        data: IDLAnnotationData,
    ): JSONObject? {
        if (model == null) return null
        val stringModel = model.stringModel
        return JSONObject(
            stringModel.mapValues {
                val value = map.opt(it.value.keyPath)
                /**
                 * init default value
                 */
                if ((value == null || value == JSONObject.NULL) && it.value.defaultValue.type != DefaultType.NONE) {
                    val defaultValue = parseStringByReturnType(it.value.returnType, it.value)
                    map.put(it.value.keyPath, defaultValue)
                }

                if (it.value.nestedClassType != IDLMethodBaseModel.Default::class && value is JSONObject) {
                    getMapWithDefault(value, data.models[it.value.nestedClassType.java]!!, data)
                } else if (it.value.nestedClassType != IDLMethodBaseModel.Default::class && value is JSONArray) {
                    value.map { v -> getMapWithDefault(v as JSONObject, data.models[it.value.nestedClassType.java]!!, data) }
                } else {
                    map.opt(it.value.keyPath)
                }
            },
        )
    }
}
