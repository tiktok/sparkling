// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core.utils

import com.tiktok.sparkling.method.registry.core.IDLAnnotationData
import com.lynx.react.bridge.PiperData
import com.tiktok.sparkling.method.registry.core.exception.IllegalInputParamException
import com.tiktok.sparkling.method.registry.core.exception.IllegalOperationException
import com.tiktok.sparkling.method.registry.core.exception.IllegalOutputParamException
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Method

object IDLMethodResultModelHelper {
    fun convertToMapByCache(
        pool: IDLAnnotationData,
        contentMap: MutableMap<String, Any?>,
    ) {
        val stringModel = pool.methodResultModel.stringModel
        stringModel.forEach {
            val annotationModel = it.value
            val keyPath = annotationModel.keyPath
            val required = annotationModel.required
            val returnType = annotationModel.returnType
            val isEnum = annotationModel.isEnum
            val enumModel =
                if (isEnum && returnType == Number::class.java) {
                    annotationModel.intEnum
                } else if (isEnum && returnType == String::class.java) {
                    annotationModel.stringEnum
                } else if (isEnum && returnType == List::class.java) {
                    when (annotationModel.primitiveClassType) {
                        Number::class -> {
                            annotationModel.intEnum
                        }

                        String::class -> {
                            annotationModel.stringEnum
                        }

                        else -> {
                            null
                        }
                    }
                } else if (isEnum && returnType == Map::class.java) {
                    when (annotationModel.primitiveClassType) {
                        Number::class -> {
                            annotationModel.intEnum
                        }

                        String::class -> {
                            annotationModel.stringEnum
                        }

                        else -> {
                            null
                        }
                    }
                } else {
                    null
                }
            val fieldValue = contentMap[keyPath]
            if (fieldValue == null && required) {
                throw IllegalOutputParamException("$keyPath is missing from output")
            }

            when (returnType) {
                Number::class.java -> {
                    fieldValue?.let { actualFieldValue ->
                        checkEnum(isEnum, enumModel, actualFieldValue, keyPath)
                        if (actualFieldValue !is Int && actualFieldValue !is Double && actualFieldValue !is Long && actualFieldValue !is Float) {
                            throw IllegalOutputParamException("$keyPath is of invalid return type")
                        }
                    }
                }

                String::class.java -> {
                    if (fieldValue == null && required) {
                        throw IllegalOutputParamException("$keyPath is missing from output")
                    }
                    fieldValue?.let { actualFieldValue ->
                        checkEnum(isEnum, enumModel, actualFieldValue, keyPath)
                        if (actualFieldValue !is String) {
                            throw IllegalOutputParamException("$keyPath is of invalid return type")
                        }
                    }
                }

                java.lang.Boolean::class.java, Boolean::class.java -> {
                    if (fieldValue == null && required) {
                        throw IllegalOutputParamException("$keyPath is missing from output")
                    }
                    fieldValue?.let { actualFieldValue ->
                        if (actualFieldValue !is Boolean) {
                            throw IllegalOutputParamException("$keyPath is of invalid return type")
                        }
                    }
                }

                List::class.java -> {
                    if (fieldValue == null && required) {
                        throw IllegalOutputParamException("$keyPath is missing from output")
                    }
                    fieldValue?.let { actualFieldValue ->
                        if (actualFieldValue !is List<*>) {
                            throw IllegalOutputParamException("$keyPath is of invalid return type")
                        }
                        checkEnum(isEnum, enumModel, actualFieldValue, keyPath)
                    }
                }

                Map::class.java -> {
                    if (fieldValue == null && required) {
                        throw IllegalOutputParamException("$keyPath is missing from output")
                    }
                    fieldValue?.let { actualFieldValue ->
                        if (actualFieldValue !is Map<*, *>) {
                            throw IllegalOutputParamException("$keyPath is of invalid return type")
                        }
                        checkEnum(isEnum, enumModel, actualFieldValue.values, keyPath)
                    }
                }

                Any::class.java -> {
                    if (fieldValue == null && required) {
                        throw IllegalOutputParamException("$keyPath is missing from output")
                    }
                }

                else -> {
                    if (fieldValue != null && fieldValue !is IDLMethodBaseModel) {
                        throw IllegalInputParamException("Failed to parse type ${returnType.name},$fieldValue must be sub class of XBaseModel")
                    }
                }
            }
            val objectInstance = IDLMethodBaseModel::class.java
            val result = getValue(fieldValue, objectInstance, returnType)
            contentMap[annotationModel.keyPath] = result
        }
    }

    fun getterAndSetter(
        pool: IDLAnnotationData,
        contentMap: MutableMap<String, Any?>,
        method: Method,
        args: Array<out Any>?,
    ): Any? {
        val methodModel = pool.methodResultModel.methodModel
        val idlParamField = methodModel[method] ?: throw IllegalOperationException("Unsupported method invocation in result model")
        return if (idlParamField.isGetter) {
            contentMap[idlParamField.keyPath]
        } else {
            val arg = args?.firstOrNull()
            contentMap[idlParamField.keyPath] = arg
        }
    }

    private fun getValue(
        arg: Any?,
        objectInstance: Class<out IDLMethodBaseModel>,
        returnType: Class<*>,
    ): Any? =
        if (returnType == Any::class.java && arg is IAssignDir<*>) {
            arg.getValue()
        } else if (arg is Int || arg is String || arg is Number || arg is Boolean || arg == null) {
            arg
        } else if (arg is List<*>) {
            arg.map { getValue(it, objectInstance, returnType) }
        } else if (arg is Map<*, *>) {
            arg.mapValues {
                getValue(it.value, objectInstance, returnType)
            }
        } else if (arg is JSONObject || arg is JSONArray) {
            arg
        } else if (arg is PiperData) {
            arg
        } else {
            // because of the JSONObject null, we need to catch the exception.
            try {
                objectInstance.cast(arg)?.convert()
            } catch (e: Exception) {
                if (e is IllegalOutputParamException) {
                    throw e
                } else {
                    arg.toString()
                }
            }
        }

    private fun checkEnum(
        isEnum: Boolean,
        enumModel: List<Any>?,
        actualFieldValue: Any,
        keyPath: String,
    ) {
        if (isEnum) {
            val failCheck =
                if (actualFieldValue is Collection<*>) {
                    actualFieldValue.any { checkEnumBasic(enumModel, it) }
                } else {
                    checkEnumBasic(enumModel, actualFieldValue)
                }
            if (failCheck) {
                throw IllegalOutputParamException("$keyPath is not valid")
            }
        }
    }

    private fun checkEnumBasic(
        enumModel: List<Any>?,
        actualFieldValue: Any?,
    ): Boolean {
        if (enumModel == null) {
            return false
        }
        return !enumModel.contains(actualFieldValue)
    }
}
