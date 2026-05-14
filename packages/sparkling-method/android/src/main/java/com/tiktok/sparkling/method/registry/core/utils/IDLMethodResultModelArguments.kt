// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core.utils

import com.tiktok.sparkling.method.registry.core.IDLMethodRegistryCacheManager
import com.lynx.react.bridge.PiperData
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodIntEnum
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamField
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodStringEnum
import com.tiktok.sparkling.method.registry.core.exception.IllegalInputParamException
import com.tiktok.sparkling.method.registry.core.exception.IllegalOperationException
import com.tiktok.sparkling.method.registry.core.exception.IllegalOutputParamException
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Desc:
 */
object IDLMethodResultModelArguments {
    /**
     * This method can let create the resultmodels bound to ContainerID
     */
    fun <T> createModel(
        clazz: Class<T>,
        containerID: String?,
    ): T where T : IDLMethodBaseModel = createModelWithContainerID(clazz, containerID)

    /**
     * This method has no containerID info.
     * Please use createModel(clazz: Class<T>, containerID: String) to create.
     */
    @Deprecated(
        "This method has no containerID info. " +
            "Please use createModel(clazz: Class<T>, containerID: String) to create.",
    )
    fun <T> createModel(clazz: Class<T>): T where T : IDLMethodBaseModel = createModelWithContainerID(clazz, null)

    private fun <T> createModelWithContainerID(
        clazz: Class<T>,
        containerID: String?,
    ) where T : IDLMethodBaseModel =
        Proxy.newProxyInstance(
            clazz.classLoader,
            arrayOf(clazz),
            object : InvocationHandler {
                val contentMap = mutableMapOf<String, Any?>()

                override fun invoke(
                    proxy: Any?,
                    method: Method,
                    args: Array<out Any>?,
                ): Any? {
                    val pool =
                        (IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(containerID))?.getAnnotationDataByResultClass(
                            clazz,
                        )
                    if (pool != null) {
                        if (method.name == "convert") {
                            IDLMethodResultModelHelper.convertToMapByCache(pool, contentMap)
                            return contentMap
                        }
                        return IDLMethodResultModelHelper.getterAndSetter(pool, contentMap, method, args)
                    }
                    println("idl Model->Map. no cache")
                    if (method.name == "convert") {
                        // do ModelValidation, find all getters
                        convertToMap(clazz, contentMap)
                        return contentMap
                    }
                    // toString
                    if (method.name == "toString") {
                        return contentMap.toString()
                    }

                    val annotationModel = method.getAnnotation(IDLMethodParamField::class.java)
                    if (annotationModel != null && annotationModel.isGetter) {
                        // getter method
                        val annotation = method.getAnnotation(IDLMethodParamField::class.java)
                        return contentMap[annotation.keyPath]
                    } else if (annotationModel != null) {
                        // setter method
                        val annotation = method.getAnnotation(IDLMethodParamField::class.java)

                        val arg = args?.firstOrNull()
                        // todo: typecast
                        contentMap[annotation.keyPath] = arg
                        return Unit
                    }
                    throw IllegalOperationException("Unsupported method invocation in result model")
                }
            },
        ) as T

    private fun <T> convertToMap(
        clazz: Class<T>,
        contentMap: MutableMap<String, Any?>,
    ) where T : IDLMethodBaseModel {
        val getters = clazz.declaredMethods.filter { it.getAnnotation(IDLMethodParamField::class.java)?.isGetter == true }
        getters.forEach { getterMethod ->
            val annotationModel = getterMethod.getAnnotation(IDLMethodParamField::class.java)
            val keyPath = annotationModel.keyPath
            val required = annotationModel.required
            val returnType = getterMethod.returnType
            val isEnum = annotationModel.isEnum
            val enumModel =
                if (isEnum && returnType == Number::class.java) {
                    getterMethod.getAnnotation(IDLMethodIntEnum::class.java)
                } else if (isEnum && returnType == String::class.java) {
                    getterMethod.getAnnotation(IDLMethodStringEnum::class.java)
                } else if (isEnum && returnType == List::class.java) {
                    when (annotationModel.primitiveClassType) {
                        Number::class -> {
                            getterMethod.getAnnotation(IDLMethodIntEnum::class.java)
                        }

                        String::class -> {
                            getterMethod.getAnnotation(IDLMethodStringEnum::class.java)
                        }

                        else -> {
                            null
                        }
                    }
                } else if (isEnum && returnType == Map::class.java) {
                    when (annotationModel.primitiveClassType) {
                        Number::class -> {
                            getterMethod.getAnnotation(IDLMethodIntEnum::class.java)
                        }

                        String::class -> {
                            getterMethod.getAnnotation(IDLMethodStringEnum::class.java)
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

            //                        convert to map
            //                        val arg = contentMap[annotationModel.keyPath]
            val objectInstance = IDLMethodBaseModel::class.java
            val result = getValue(fieldValue, objectInstance, returnType)
            contentMap[annotationModel.keyPath] = result
        }
    }

    private fun checkEnum(
        isEnum: Boolean,
        enumModel: Annotation?,
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
        enumModel: Annotation?,
        actualFieldValue: Any?,
    ): Boolean =
        when (enumModel) {
            is IDLMethodStringEnum -> {
                actualFieldValue !in enumModel.option
            }

            is IDLMethodIntEnum -> {
                actualFieldValue as Int !in enumModel.option
            }

            else -> {
                true
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
}
