// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.api.processor

import com.tiktok.sparkling.method.registry.api.Utils
import com.tiktok.sparkling.method.registry.api.util.map
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
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

/**
 * Binds a Web [JSONObject] to a method's param model, and renders a result map back as one.
 *
 * Every annotation read goes through [reader], so a host whose method classes carry their own
 * annotation set supplies a reader rather than a copy of this class. The protected members are the
 * remaining host seams: which sentinel a cached field may carry and how JSON containers are converted.
 */
open class WebPlatformDataProcessor(
    private val reader: IDLParamModelReader = DefaultIDLParamModelReader,
) : IPlatformDataProcessor<JSONObject> {
    override fun matchPlatformType(platformType: IBridgePlatformType) = platformType == BridgePlatformType.WEB

    var context: IBridgeContext? = null

    private val cachedBinding: WebProcessorForMap by lazy {
        WebProcessorForMap(::isCachedDefaultNestedType, ::jsonToList, ::jsonToMap)
    }

    override fun transformPlatformDataToMap(
        params: JSONObject,
        clazz: Class<out IDLBridgeMethod>,
    ): Map<String, Any?>? {
        val pool = reader.findAnnotationData(context?.containerID, clazz)
        if (pool != null) {
            return cachedBinding.getJavaOnlyMapParams(params, pool)
        } else {
            val modelClazz = reader.retrieveParamModel(clazz) ?: return null
            return getJsonObjectParams(params, modelClazz)
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

    /** Converts a [JSONArray] value that is not a list of nested models. */
    protected open fun jsonToList(value: JSONArray): List<Any?> = Utils.jsonToList(value)

    /** Converts a [JSONObject] value that is not a nested model. */
    protected open fun jsonToMap(value: JSONObject): Map<String, Any?> = Utils.jsonToMap(value)

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
    ): HashMap<String, Pair<Method, IDLParamField>>? {
        val classMap =
            clazz?.declaredMethods?.fold(hashMapOf<String, Pair<Method, IDLParamField>>()) { map, method ->
                val annotation = reader.readParamField(method)
                if (annotation != null) {
                    map[annotation.keyPath] = Pair<Method, IDLParamField>(method, annotation)
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
        classMap: HashMap<String, Pair<Method, IDLParamField>>,
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
                            (value as JSONObject).mapValues { item ->
                                if (!option.contains(item.second)) {
                                    throw IllegalInputParamException("${it.key} has wrong value.should be one of ${option.asList()} but got $value")
                                }
                            }
                        } else {
                            val intOption = reader.readIntEnumOptions(method)
                            if (intOption != null) {
                                val option = intOption
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
            val annotation = requireParamField(method)
            val value = map.opt(annotation.keyPath)
            return@newProxyInstance convertValueWithAnnotation(value, annotation)
        }
    }

    private fun convertValueWithAnnotation(
        value: Any?,
        annotation: IDLParamField?,
    ) = if (isNestClass(value, annotation)) {
        proxyValue(annotation?.nestedClassType?.java, value as JSONObject)
    } else if (isNestListClass(value, annotation)) {
        (value as JSONArray).map {
            proxyValue(annotation?.nestedClassType?.java, it as JSONObject)
        }
    } else {
        when (value) {
            is JSONArray -> {
                jsonToList(value)
            }

            is JSONObject -> {
                jsonToMap(value)
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
        annotation: IDLParamField?,
    ) = value is JSONObject && !reader.isDefaultNestedType(annotation?.nestedClassType)

    private fun isNestListClass(
        value: Any?,
        annotation: IDLParamField?,
    ) = value is JSONArray && !reader.isDefaultNestedType(annotation?.nestedClassType)

    private fun getMapWithDefault(
        clazz: Class<out IDLMethodBaseModel>?,
        json: JSONObject,
    ): JSONObject {
        val methods = clazz?.declaredMethods?.filter { reader.readParamField(it)?.isGetter == true }
        return methods?.fold(JSONObject()) { acc, method ->
            val annotation = requireParamField(method)

            val value = json.opt(annotation.keyPath)

            // Init default value.
            if ((value == null || value == JSONObject.NULL) && annotation.defaultValue.type != DefaultType.NONE) {
                val defaultValue = parseStringByReturnType(method, annotation)
                json.put(annotation.keyPath, defaultValue)
            }
            val nestedClassType = annotation.nestedClassType
            acc.put(
                annotation.keyPath,
                if (!reader.isDefaultNestedType(annotation.nestedClassType) && value is JSONObject) {
                    getMapWithDefault(nestedClassType.java, value)
                } else if (!reader.isDefaultNestedType(annotation.nestedClassType) && value is JSONArray) {
                    value.map { getMapWithDefault(nestedClassType.java, it as JSONObject) }
                } else {
                    json.opt(annotation.keyPath)
                },
            )
            acc
        } ?: JSONObject()
    }

    private fun MutableMap<String, Any>.initDefaultValue(getters: List<Method>) {
        val defaultMethods = getters.filter { requireParamField(it).defaultValue.type != DefaultType.NONE }
        defaultMethods.forEach {
            val annotation = requireParamField(it)
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
        val methods = clazz?.declaredMethods?.filter { reader.readParamField(it)?.isGetter == true }
        return methods?.fold(JSONObject()) { acc, method ->
            val annotation = requireParamField(method)

            val value = map.opt(annotation.keyPath)

            // Init default value.
            if ((value == null || value == JSONObject.NULL) && annotation.defaultValue.type != DefaultType.NONE) {
                val defaultValue = parseStringByReturnType(method, annotation)
                map.put(annotation.keyPath, defaultValue)
            }

            acc.put(
                annotation.keyPath,
                if (!reader.isDefaultNestedType(annotation.nestedClassType) && value != null && value != JSONObject.NULL) {
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
    ): JSONObject = Utils.mapToJSON(params)
}
