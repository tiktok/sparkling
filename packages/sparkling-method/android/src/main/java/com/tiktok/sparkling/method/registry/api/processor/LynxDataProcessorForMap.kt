// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.api.processor

import com.tiktok.sparkling.method.registry.core.IDLAnnotationData
import com.tiktok.sparkling.method.registry.core.IDLAnnotationModel
import com.tiktok.sparkling.method.registry.core.IDLParamField
import com.tiktok.sparkling.method.registry.core.annotation.DefaultType
import com.tiktok.sparkling.method.registry.api.Utils
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import com.lynx.react.bridge.ReadableArray
import com.lynx.react.bridge.ReadableMap
import com.tiktok.sparkling.method.registry.core.exception.IllegalInputParamException
import org.json.JSONObject
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

/**
 * @author txr
 * @since 2021/10/13
 * @desc
 *
 * Binds a Lynx map through what the annotation cache holds for a method.
 *
 * Deviation from open source: a class rather than an object, so that each
 * [LynxPlatformDataProcessor] passes in its own rules instead of a host replacing global ones.
 *
 * @param isDefaultNestedType whether a field's nested type is the "no nested model" sentinel.
 * @param convertValue turns a value that is not a nested model into a plain one.
 */
class LynxDataProcessorForMap(
    private val isDefaultNestedType: (KClass<out IDLMethodBaseModel>) -> Boolean = {
        it == IDLMethodBaseModel.Default::class
    },
    private val convertValue: (Any?) -> Any? = Utils::getValue,
) {

    fun getJavaOnlyMapParams(params: HashMap<String, Any>, clazz: IDLAnnotationData): Map<String, Any?>? {
        val classMap = preCheck(clazz.methodParamModel, params) ?: return null
        return params.mapValues {
            val annotation = classMap.stringModel[it.key]
            val value = it.value
            convertValueWithAnnotation(value, annotation, clazz)
        }
    }

    @Throws(IllegalInputParamException::class)
    private fun proxyValue(clazz: Class<out IDLMethodBaseModel>?, map: HashMap<String, Any>, data: IDLAnnotationData): Any? {
        if (clazz == null) return null
        val clazzMap = preCheck(data.models[clazz], map) ?: return null
        return Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz)) { _, method, _ ->
            if (method.name == "toJSON") {
                val fold = getMapWithDefault(map, data.models[clazz], data)
                return@newProxyInstance JSONObject(fold)
            }
            if (method.name == "toString") {
                return@newProxyInstance map.toString()
            }
            val annotation = clazzMap.methodModel[method]
            val value = map[annotation?.keyPath]
            return@newProxyInstance convertValueWithAnnotation(value, annotation, data)
        }
    }

    private fun convertValueWithAnnotation(value: Any?, annotation: IDLParamField?, data: IDLAnnotationData): Any? {
        val result = if (isNestClass(value, annotation)) {
            proxyValue(annotation?.nestedClassType?.java, (value as ReadableMap).toHashMap(), data)
        } else if (isNestListClass(value, annotation)) {
            (value as List<*>).map {
                if (it is ReadableMap) {
                    proxyValue(annotation?.nestedClassType?.java, (it as ReadableMap).toHashMap(), data)
                } else {
                    convertValue(it)
                }
            }
        } else {
            convertValue(value)
        }
        return result
    }

    private fun checkValue(classMap: IDLAnnotationModel, params: HashMap<String, Any>) {
        /**
         * check value
         */
        classMap.stringModel.forEach {
            val field = it.key
            val method = it.value
            val value = params[field]
            if (method.required && value == null) {
                throw IllegalInputParamException("${it.key} param is missing from input")
            }
            when (method.returnType) {
                String::class.java -> {
                    if (value != null && value !is String) {
                        throw  IllegalInputParamException("${it.key} param has wrong declared type. except string,but ${value.javaClass}")
                    }
                }
                Number::class.java -> {
                    if (value != null && value !is Number) {
                        throw  IllegalInputParamException("${it.key} param has wrong declared type. except number,but ${value.javaClass}")
                    }
                }
                java.lang.Boolean::class.java, Boolean::class.java -> {
                    if (value != null && value !is Boolean) {
                        throw  IllegalInputParamException("${it.key} param has wrong declared type. except boolean,but ${value.javaClass}")
                    }
                }
                List::class.java -> {
                    if (value != null && value !is List<*>) {
                        throw  IllegalInputParamException("${it.key} param has wrong declared type. except List ,but ${value.javaClass}")
                    }
                }
                Map::class.java -> {
                    if (value != null && value !is Map<*, *>) {
                        throw  IllegalInputParamException("${it.key} param has wrong declared type. except Map ,but ${value.javaClass}")
                    }
                }
            }
            if (value != null && method.isEnum) {
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
                            if ((value as Map<String, Any>).any { item -> !stringEnum.contains(item.value) }) {
                                throw IllegalInputParamException("${it.key} has wrong type.should be one of $stringEnum but got $value")
                            }
                        } else {
                            val intEnum = method.intEnum
                            if (intEnum.isNotEmpty()) {
                                if ((value as Map<String, Any>).any { item -> !intEnum.contains(getInt(item.value)) }) {
                                    throw IllegalInputParamException("${it.key} has wrong value.should be one of $intEnum but got $value")
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


    private fun isNestClass(value: Any?, annotation: IDLParamField?) = value is Map<*, *> && annotation?.nestedClassType?.let { !isDefaultNestedType(it) } ?: true

    private fun isNestListClass(value: Any?, annotation: IDLParamField?) =
        value is List<*> && annotation?.nestedClassType?.let { !isDefaultNestedType(it) } ?: true


    private fun preCheck(classMap: IDLAnnotationModel?, map: HashMap<String, Any>): IDLAnnotationModel? {
        /**
         * init default value
         */
        if (classMap == null) return null
        classMap.stringModel.filter { map[it.key] == null && it.value.defaultValue.type != DefaultType.NONE }.forEach {
            val idlParamField = it.value
            map[it.key] = parseStringByReturnType(idlParamField.returnType, idlParamField)
        }
        checkValue(classMap, map)
        return classMap
    }

    private fun parseStringByReturnType(returnType: Class<*>, annotation: IDLParamField) = when (returnType) {
        Number::class.java -> when (annotation.defaultValue.type) {
            DefaultType.DOUBLE -> annotation.defaultValue.doubleValue
            DefaultType.LONG -> annotation.defaultValue.longValue
            DefaultType.INT -> annotation.defaultValue.intValue
            else -> annotation.defaultValue.intValue
        }
        Boolean::class.java, java.lang.Boolean::class.java -> annotation.defaultValue.boolValue
        else -> annotation.defaultValue.stringValue
    }

    private fun getMapWithDefault(map: HashMap<String, Any>, model: IDLAnnotationModel?, data: IDLAnnotationData): Map<String, Any?>? {
        if (model == null) return null
        val stringModel = model.stringModel
        return stringModel.mapValues {
            val value = map[it.value.keyPath]
            /**
             * init default value
             */
            if (value == null && it.value.defaultValue.type != DefaultType.NONE) {
                val defaultValue = parseStringByReturnType(it.value.returnType, it.value)
                map[it.value.keyPath] = defaultValue
            }

            if (!isDefaultNestedType(it.value.nestedClassType) && value is ReadableMap) {
                getMapWithDefault(value.toHashMap(), data.models[it.value.nestedClassType.java]!!, data)
            } else if (!isDefaultNestedType(it.value.nestedClassType) && value is ReadableArray) {
                value.toArrayList().map { v -> getMapWithDefault((v as ReadableMap).toHashMap(), data.models[it.value.nestedClassType.java]!!, data) }
            } else {
                map[it.value.keyPath]
            }
        }
    }
}