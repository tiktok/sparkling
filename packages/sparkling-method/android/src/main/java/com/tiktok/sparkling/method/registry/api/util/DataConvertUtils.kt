// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.registry.api.util

import com.tiktok.sparkling.method.registry.api.stringify
import com.lynx.jsbridge.Arguments
import com.lynx.react.bridge.Dynamic
import com.lynx.react.bridge.JavaOnlyArray
import com.lynx.react.bridge.JavaOnlyMap
import com.lynx.react.bridge.PiperData
import com.lynx.react.bridge.ReadableArray
import com.lynx.react.bridge.ReadableMap
import com.lynx.react.bridge.ReadableType
import com.lynx.react.bridge.WritableArray
import com.lynx.react.bridge.WritableMap
import com.tiktok.sparkling.method.registry.core.XReadableArray
import com.tiktok.sparkling.method.registry.core.XReadableMap
import com.tiktok.sparkling.method.registry.core.XReadableType
import org.json.JSONArray
import org.json.JSONObject

object DataConvertUtils : IConvertUtils {
    var disableLongToDouble = false
    var newInputTypeChange = false

    override fun toStringOrJson(data: Any?): String {
        if (data == null) {
            return ""
        }
        return when (data) {
            is Map<*, *> -> JSONObject(data).toString()
            is List<*> -> JSONArray(data).toString()
            else -> data.toString()
        }
    }

    override fun mapSupportPiperdataToJSON(map: Map<String, Any?>): JSONObject {
        val jsonObject = JSONObject()
        map.forEach {
            val key = it.key
            when (val value = it.value) {
                is Long -> {
                    if (disableLongToDouble) {
                        jsonObject.put(key, value)
                    } else {
                        jsonObject.put(key, value.toDouble())
                    }
                }

                is Float -> {
                    jsonObject.put(key, value.toDouble())
                }

                is Int -> {
                    jsonObject.put(key, value)
                }

                is Double -> {
                    jsonObject.put(key, value)
                }

                is String -> {
                    jsonObject.put(key, value)
                }

                is Boolean -> {
                    jsonObject.put(key, value)
                }

                is JSONObject -> {
                    jsonObject.put(key, value)
                }

                is JSONArray -> {
                    jsonObject.put(key, value)
                }

                is PiperData -> {
                    jsonObject.put(key, value)
                }

                is Map<*, *> -> {
                    try {
                        jsonObject.put(key, mapSupportPiperdataToJSON(value as Map<String, Any>))
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                is List<*> -> {
                    try {
                        jsonObject.put(key, listSupportPiperdataToJSON(value as List<Any>))
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        }
        return jsonObject
    }

    override fun listSupportPiperdataToJSON(list: List<Any>): JSONArray {
        val jsonArray = JSONArray()
        list.forEach {
            when (it) {
                is Float -> {
                    jsonArray.put(it.toDouble())
                }

                is Long -> {
                    if (disableLongToDouble) {
                        jsonArray.put(it)
                    } else {
                        jsonArray.put(it.toDouble())
                    }
                }

                is Int -> {
                    jsonArray.put(it)
                }

                is Double -> {
                    jsonArray.put(it)
                }

                is String -> {
                    jsonArray.put(it)
                }

                is Boolean -> {
                    jsonArray.put(it)
                }

                is PiperData -> {
                    jsonArray.put(it)
                }

                is Map<*, *> -> {
                    try {
                        jsonArray.put(mapSupportPiperdataToJSON(it as Map<String, Any>))
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                is List<*> -> {
                    try {
                        jsonArray.put(listSupportPiperdataToJSON(it as List<Any>))
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        }
        return jsonArray
    }

    override fun mapToJSON(map: Map<String, Any?>): JSONObject {
        val jsonObject = JSONObject()
        map.forEach {
            val key = it.key
            when (val value = it.value) {
                is Long -> {
                    if (disableLongToDouble) {
                        jsonObject.put(key, value)
                    } else {
                        jsonObject.put(key, value.toDouble())
                    }
                }

                is Float -> {
                    jsonObject.put(key, value.toDouble())
                }

                is Int -> {
                    jsonObject.put(key, value)
                }

                is Double -> {
                    jsonObject.put(key, value)
                }

                is String -> {
                    jsonObject.put(key, value)
                }

                is Boolean -> {
                    jsonObject.put(key, value)
                }

                is JSONObject -> {
                    jsonObject.put(key, value)
                }

                is JSONArray -> {
                    jsonObject.put(key, value)
                }

                is PiperData -> {
                    jsonObject.put(key, value.stringify())
                }

                is Map<*, *> -> {
                    try {
                        jsonObject.put(key, mapToJSON(value as Map<String, Any>))
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                is List<*> -> {
                    try {
                        jsonObject.put(key, listToJSON(value as List<Any>))
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        }
        return jsonObject
    }

    override fun listToJSON(list: List<Any>): JSONArray {
        val jsonArray = JSONArray()
        list.forEach {
            when (it) {
                is Float -> {
                    jsonArray.put(it.toDouble())
                }

                is Long -> {
                    if (disableLongToDouble) {
                        jsonArray.put(it)
                    } else {
                        jsonArray.put(it.toDouble())
                    }
                }

                is Int -> {
                    jsonArray.put(it)
                }

                is Double -> {
                    jsonArray.put(it)
                }

                is String -> {
                    jsonArray.put(it)
                }

                is Boolean -> {
                    jsonArray.put(it)
                }

                is PiperData -> {
                    jsonArray.put(it.stringify())
                }

                is Map<*, *> -> {
                    try {
                        jsonArray.put(mapToJSON(it as Map<String, Any>))
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                is List<*> -> {
                    try {
                        jsonArray.put(listToJSON(it as List<Any>))
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        }
        return jsonArray
    }

    override fun jsonToMap(json: JSONObject): Map<String, Any?> =
        mutableMapOf<String, Any?>().apply {
            val iterator = json.keys()
            while (iterator.hasNext()) {
                val key = iterator.next()
                when (json.opt(key)) {
                    is Long -> put(key, json.optLong(key))
                    is Double -> put(key, json.optDouble(key))
                    is Int -> put(key, json.optInt(key))
                    is String -> put(key, json.optString(key))
                    is JSONObject -> put(key, jsonToMap(json.optJSONObject(key)))
                    is JSONArray -> put(key, jsonToList(json.optJSONArray(key)))
                    is Boolean -> put(key, json.optBoolean(key))
                    else -> put(key, null)
                }
            }
        }

    override fun jsonToList(json: JSONArray): List<Any?> =
        mutableListOf<Any?>().apply {
            val len = json.length()
            for (index in 0 until len) {
                when (json.opt(index)) {
                    is Long -> add(json.optLong(index))
                    is Double -> add(json.optDouble(index))
                    is Int -> add(json.getInt(index))
                    is String -> add(json.optString(index))
                    is JSONObject -> add(jsonToMap(json.optJSONObject(index)))
                    is JSONArray -> add(jsonToList(json.optJSONArray(index)))
                    is Boolean -> add(json.optBoolean(index))
                    else -> add(null)
                }
            }
        }

    override fun convertJsonToMap(jsonObject: JSONObject): WritableMap {
        val map = JavaOnlyMap()
        val iterator = jsonObject.keys()
        while (iterator.hasNext()) {
            val key: String = iterator.next()
            when (val value = jsonObject.get(key)) {
                is JSONObject -> {
                    map.putMap(key, convertJsonToMap(value))
                }

                is JSONArray -> {
                    map.putArray(key, convertJsonToArray(value))
                }

                is Boolean -> {
                    map.putBoolean(key, value)
                }

                is Int -> {
                    map.putInt(key, value)
                }

                is Long -> {
                    if (disableLongToDouble) {
                        map.putLong(key, value)
                    } else {
                        map.putDouble(key, value.toDouble())
                    }
                }

                is Float -> {
                    map.putDouble(key, value.toDouble())
                }

                is Double -> {
                    map.putDouble(key, value)
                }

                is String -> {
                    map.putString(key, value)
                }

                JSONObject.NULL -> {
                    map.putMap(key, null)
                }

                else -> {
                    map.putString(key, value.toString())
                }
            }
        }

        return map
    }

    override fun convertJsonToArray(jsonArray: JSONArray): WritableArray {
        val array = JavaOnlyArray()

        for (idx in 0 until jsonArray.length()) {
            when (val value = jsonArray.get(idx)) {
                is JSONObject -> {
                    array.pushMap(convertJsonToMap(value))
                }

                is JSONArray -> {
                    array.pushArray(convertJsonToArray(value))
                }

                is Boolean -> {
                    array.pushBoolean(value)
                }

                is Int -> {
                    array.pushInt(value)
                }

                is Long -> {
                    if (disableLongToDouble) {
                        array.pushLong(value)
                    } else {
                        array.pushDouble(value.toDouble())
                    }
                }

                is Float -> {
                    array.pushDouble(value.toDouble())
                }

                is Double -> {
                    array.pushDouble(value)
                }

                is String -> {
                    array.pushString(value)
                }

                else -> {
                    if (JSONObject.NULL == value || null == value) {
                        array.pushNull()
                    } else {
                        array.pushString(value.toString())
                    }
                }
            }
        }

        return array
    }

    override fun convertMapToReadableMap(source: Map<String, Any?>): WritableMap {
        val map = JavaOnlyMap()
        source.forEach {
            val key = it.key
            when (val value = it.value) {
                is Map<*, *> -> {
                    try {
                        map.putMap(key, convertMapToReadableMap(value as Map<String, Any>))
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                is List<*> -> {
                    try {
                        map.putArray(key, convertArrayToWritableArray(value as List<Any>))
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                is Array<*> -> {
                    try {
                        map.putArray(key, convertRealArrayToWritableArray(value as Array<Any>))
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                is Boolean -> {
                    map.putBoolean(key, value)
                }

                is Int -> {
                    map.putInt(key, value)
                }

                is Long -> {
                    if (disableLongToDouble) {
                        map.putLong(key, value)
                    } else {
                        map.putDouble(key, value.toDouble())
                    }
                }

                is Float -> {
                    map.putDouble(key, value.toDouble())
                }

                is Double -> {
                    map.putDouble(key, value)
                }

                is String -> {
                    map.putString(key, value)
                }

                is JSONObject -> {
                    map.putMap(key, convertJsonToMap(value))
                }

                is JSONArray -> {
                    map.putArray(key, convertJsonToArray(value))
                }

                is PiperData -> {
                    map.putPiperData(key, value)
                }

                else -> {
                    if (value == null || value == JSONObject.NULL) {
                        map.putNull(key)
                    } else {
                        map.putString(key, value.toString())
                    }
                }
            }
        }
        return map
    }

    private fun convertRealArrayToWritableArray(sourceArray: Array<Any>): WritableArray {
        val ret = Arguments.createArray()
        sourceArray.forEach { item ->
            when (item) {
                is Float, is Double -> {
                    ret.pushDouble(item as Double)
                }

                is Long -> {
                    ret.pushLong(item as Long)
                }

                is Number -> {
                    ret.pushInt(item as Int)
                }

                is String -> {
                    ret.pushString(item as String)
                }

                is Boolean -> {
                    ret.pushBoolean(item as Boolean)
                }

                is Map<*, *> -> {
                    ret.pushMap(convertMapToReadableMap(item as Map<String, Any?>))
                }

                is List<*> -> {
                    ret.pushArray(convertArrayToWritableArray(item as List<Any>))
                }

                is Array<*> -> {
                    ret.pushArray(convertRealArrayToWritableArray(item as Array<Any>))
                }

                is JSONArray -> {
                    ret.pushArray(convertJsonToArray(item))
                }

                is JSONObject -> {
                    ret.pushMap(convertJsonToMap(item))
                }

                is PiperData -> {
                    ret.pushPiperData(item)
                }

                else -> {}
            }
        }
        return ret
    }

    override fun convertArrayToWritableArray(sourceArray: List<Any>): WritableArray {
        val array = JavaOnlyArray()

        for (idx in sourceArray.indices) {
            when (val value = sourceArray[idx]) {
                is Map<*, *> -> {
                    try {
                        array.pushMap(convertMapToReadableMap(value as Map<String, Any>))
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                is List<*> -> {
                    try {
                        array.pushArray(convertArrayToWritableArray(value as List<Any>))
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                is Boolean -> {
                    array.pushBoolean(value)
                }

                is Int -> {
                    array.pushInt(value)
                }

                is Long -> {
                    if (disableLongToDouble) {
                        array.pushLong(value)
                    } else {
                        array.pushDouble(value.toDouble())
                    }
                }

                is Float -> {
                    array.pushDouble(value.toDouble())
                }

                is Double -> {
                    array.pushDouble(value)
                }

                is String -> {
                    array.pushString(value)
                }

                is PiperData -> {
                    array.pushPiperData(value)
                }

                else -> {
                    array.pushString(value.toString())
                }
            }
        }

        return array
    }

    override fun convertXReadableMapToReadableMap(source: XReadableMap): WritableMap {
        val map = JavaOnlyMap()
        val keyIterator = source.keyIterator()
        while (keyIterator.hasNextKey()) {
            val key = keyIterator.nextKey()
            val value = source.get(key)
            when (value.getType()) {
                XReadableType.String -> {
                    map[key] = value.asString()
                }

                XReadableType.Int -> {
                    map[key] = value.asInt()
                }

                XReadableType.Number -> {
                    map[key] = value.asDouble()
                }

                XReadableType.Boolean -> {
                    map[key] = value.asBoolean()
                }

                XReadableType.Array -> {
                    map[key] =
                        value.asArray()?.let { map[key] = convertXReadableArrayToReadableArray(it) }
                }

                XReadableType.Map -> {
                    map[key] =
                        value.asMap()?.let { map[key] = convertXReadableMapToReadableMap(it) }
                }

                else -> {
                }
            }
        }

        return map
    }

    override fun convertXReadableArrayToReadableArray(source: XReadableArray): WritableArray {
        val array = JavaOnlyArray()

        for (idx in 0 until source.size()) {
            when (source.get(idx).getType()) {
                XReadableType.Boolean -> {
                    array.pushBoolean(source.getBoolean(idx))
                }

                XReadableType.Int -> {
                    array.pushInt(source.getInt(idx))
                }

                XReadableType.Number -> {
                    array.pushDouble(source.getDouble(idx))
                }

                XReadableType.String -> {
                    array.pushString(source.getString(idx))
                }

                XReadableType.Array -> {
                    array.pushArray(
                        source.getArray(idx)?.let {
                            convertXReadableArrayToReadableArray(
                                it,
                            )
                        },
                    )
                }

                XReadableType.Map -> {
                    array.pushMap(
                        source.getMap(idx)?.let {
                            convertXReadableMapToReadableMap(
                                it,
                            )
                        },
                    )
                }

                else -> {
                }
            }
        }

        return array
    }

    override fun getValue(value: Any?): Any? =
        when (value) {
            is ReadableArray -> {
                val size = value.size()
                val list = ArrayList<Any?>()
                for (i in 0 until size) {
                    list.add(getValue(value.getDynamic(i).getValue()))
                }
                list
            }

            is ReadableMap -> {
                val keyIterator = value.keySetIterator()
                val map = hashMapOf<String, Any?>()
                while (keyIterator.hasNextKey()) {
                    val key = keyIterator.nextKey()
                    map[key] = getValue(value.getDynamic(key).getValue())
                }
                map
            }

            is Number -> {
                if (!newInputTypeChange) {
                    val doubleValue = value.toDouble()
                    val intValue = value.toInt()
                    if (doubleValue == intValue.toDouble()) {
                        intValue
                    } else {
                        doubleValue
                    }
                } else {
                    when (value) {
                        is Long -> {
                            value
                        }

                        else -> {
                            val doubleValue = value.toDouble()
                            val intValue = value.toInt()
                            if (doubleValue == intValue.toDouble()) {
                                intValue
                            } else {
                                doubleValue
                            }
                        }
                    }
                }
            }

            else -> {
                value
            }
        }

    /**
     *
     */
    fun Dynamic.getValue(): Any? {
        return when (type) {
            ReadableType.String -> {
                asString()
            }

            ReadableType.Number -> {
                val asDouble = asDouble()
                val asInt = asInt()
                return if (asInt.toDouble() == asDouble) {
                    asInt
                } else {
                    asDouble
                }
            }

            ReadableType.Boolean -> {
                asBoolean()
            }

            ReadableType.Map -> {
                asMap()
            }

            ReadableType.Array -> {
                asArray()
            }

            ReadableType.Null -> {
                null
            }

            else -> {
                if (!newInputTypeChange) {
                    null
                } else {
                    when (type) {
                        ReadableType.Long -> {
                            asLong()
                        }

                        else -> {
                            null
                        }
                    }
                }
            }
        }
    }
}

internal fun <T> JSONArray.map(cb: (item: Any) -> T): List<T> {
    val result = mutableListOf<T>()
    for (i in 0 until this.length()) {
        result.add(cb(opt(i)))
    }
    return result
}
