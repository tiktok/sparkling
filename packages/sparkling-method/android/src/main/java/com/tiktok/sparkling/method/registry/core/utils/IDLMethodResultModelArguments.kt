// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core.utils

import com.lynx.react.bridge.PiperData
import com.tiktok.sparkling.method.registry.core.IDLParamField
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
 * Creates result-model proxies using a configurable annotation reader. Existing overloads
 * use [DefaultIDLResultModelReader] to preserve the Sparkling annotations and cache.
 */
object IDLMethodResultModelArguments {
    /**
     * This method can let create the resultmodels bound to ContainerID
     */
    fun <T> createModel(
        clazz: Class<T>,
        containerID: String?,
    ): T where T : IDLMethodBaseModel = createModel(clazz, containerID, DefaultIDLResultModelReader)

    /**
     * Builds a result model, reading its fields through [reader].
     *
     * A host whose method classes carry another annotation set passes its own reader.
     */
    fun <T> createModel(
        clazz: Class<T>,
        containerID: String?,
        reader: IDLResultModelReader,
    ): T where T : IDLMethodBaseModel = createModelWithContainerID(clazz, containerID, reader)

    /**
     * This method has no containerID info.
     * Please use createModel(clazz: Class<T>, containerID: String) to create.
     */
    @Deprecated(
        "This method has no containerID info. " +
            "Please use createModel(clazz: Class<T>, containerID: String) to create.",
    )
    fun <T> createModel(clazz: Class<T>): T where T : IDLMethodBaseModel = createModel(clazz, null)

    private fun <T> createModelWithContainerID(
        clazz: Class<T>,
        containerID: String?,
        reader: IDLResultModelReader,
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
                    val pool = reader.findResultAnnotationData(containerID, clazz)
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
                        convertToMap(clazz, contentMap, reader)
                        return contentMap
                    }
                    // toString
                    if (method.name == "toString") {
                        return contentMap.toString()
                    }

                    val annotationModel = reader.readField(method)
                    if (annotationModel != null && annotationModel.isGetter) {
                        // getter method
                        return contentMap[annotationModel.keyPath]
                    } else if (annotationModel != null) {
                        // setter method
                        val arg = args?.firstOrNull()
                        // todo: typecast
                        contentMap[annotationModel.keyPath] = arg
                        return Unit
                    }
                    throw IllegalOperationException("Unsupported method invocation in result model")
                }
            },
        ) as T

    private fun <T> convertToMap(
        clazz: Class<T>,
        contentMap: MutableMap<String, Any?>,
        reader: IDLResultModelReader,
    ) where T : IDLMethodBaseModel {
        val getters = clazz.declaredMethods.filter { reader.readField(it)?.isGetter == true }
        getters.forEach { getterMethod ->
            val annotationModel =
                reader.readField(getterMethod)
                    ?: return@forEach
            val keyPath = annotationModel.keyPath
            val required = annotationModel.required
            val returnType = getterMethod.returnType
            val isEnum = annotationModel.isEnum
            val stringEnum = readStringEnum(isEnum, returnType, annotationModel, getterMethod, reader)
            val intEnum = readIntEnum(isEnum, returnType, annotationModel, getterMethod, reader)
            val fieldValue = contentMap[keyPath]
            if (fieldValue == null && required) {
                throw IllegalOutputParamException("$keyPath is missing from output")
            }

            when (returnType) {
                Number::class.java -> {
                    fieldValue?.let { actualFieldValue ->
                        checkEnum(isEnum, stringEnum, intEnum, actualFieldValue, keyPath)
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
                        checkEnum(isEnum, stringEnum, intEnum, actualFieldValue, keyPath)
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
                        checkEnum(isEnum, stringEnum, intEnum, actualFieldValue, keyPath)
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
                        checkEnum(isEnum, stringEnum, intEnum, actualFieldValue.values, keyPath)
                    }
                }

                Any::class.java -> {
                    if (fieldValue == null && required) {
                        throw IllegalOutputParamException("$keyPath is missing from output")
                    }
                }

                else -> {
                    if (fieldValue != null && !reader.isNestedModel(fieldValue)) {
                        throw IllegalInputParamException("Failed to parse type ${returnType.name},$fieldValue must be sub class of XBaseModel")
                    }
                }
            }

            val result = getValue(fieldValue, reader.modelClass, returnType)
            contentMap[annotationModel.keyPath] = result
        }
    }

    private fun readStringEnum(
        isEnum: Boolean,
        returnType: Class<*>,
        annotationModel: IDLParamField,
        getterMethod: Method,
        reader: IDLResultModelReader,
    ): Array<out String>? {
        if (!isEnum || !usesStringEnum(returnType, annotationModel)) {
            return null
        }
        return reader.readStringEnumOptions(getterMethod)
    }

    private fun readIntEnum(
        isEnum: Boolean,
        returnType: Class<*>,
        annotationModel: IDLParamField,
        getterMethod: Method,
        reader: IDLResultModelReader,
    ): IntArray? {
        if (!isEnum || !usesIntEnum(returnType, annotationModel)) {
            return null
        }
        return reader.readIntEnumOptions(getterMethod)
    }

    private fun usesIntEnum(
        returnType: Class<*>,
        annotationModel: IDLParamField,
    ): Boolean {
        if (returnType == Number::class.java) {
            return true
        }
        return (returnType == List::class.java || returnType == Map::class.java) &&
            annotationModel.primitiveClassType == Number::class
    }

    private fun usesStringEnum(
        returnType: Class<*>,
        annotationModel: IDLParamField,
    ): Boolean {
        if (returnType == String::class.java) {
            return true
        }
        return (returnType == List::class.java || returnType == Map::class.java) &&
            annotationModel.primitiveClassType == String::class
    }

    private fun checkEnum(
        isEnum: Boolean,
        stringEnum: Array<out String>?,
        intEnum: IntArray?,
        actualFieldValue: Any,
        keyPath: String,
    ) {
        if (isEnum) {
            val failCheck =
                if (actualFieldValue is Collection<*>) {
                    actualFieldValue.any { checkEnumBasic(stringEnum, intEnum, it) }
                } else {
                    checkEnumBasic(stringEnum, intEnum, actualFieldValue)
                }
            if (failCheck) {
                throw IllegalOutputParamException("$keyPath is not valid")
            }
        }
    }

    private fun checkEnumBasic(
        stringEnum: Array<out String>?,
        intEnum: IntArray?,
        actualFieldValue: Any?,
    ): Boolean =
        when {
            stringEnum != null -> {
                actualFieldValue !in stringEnum
            }

            intEnum != null -> {
                actualFieldValue as Int !in intEnum
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
