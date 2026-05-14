// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

import com.tiktok.sparkling.method.registry.core.utils.JsonUtils
import org.json.JSONArray
import org.json.JSONObject

class ReadableMapImpl(
    private val origin: JSONObject,
) : XReadableMap {
    override fun hasKey(name: String): Boolean = origin.has(name)

    override fun isNull(name: String): Boolean = origin.isNull(name)

    override fun getBoolean(name: String): Boolean = origin.optBoolean(name)

    override fun getDouble(name: String): Double = origin.optDouble(name)

    override fun getInt(name: String): Int = origin.optInt(name)

    override fun getString(name: String): String = origin.optString(name)

    override fun getArray(name: String): XReadableArray? {
        val res = origin.optJSONArray(name)
        return if (res == null) {
            null
        } else {
            ReadableArrayImpl(res)
        }
    }

    override fun getMap(name: String): XReadableMap? {
        val res = origin.optJSONObject(name)
        return if (res == null) {
            null
        } else {
            ReadableMapImpl(res)
        }
    }

    override fun get(name: String): XDynamic = DynamicImpl(origin.opt(name))

    override fun getType(name: String): XReadableType =
        when (origin.opt(name)) {
            is JSONArray -> XReadableType.Array
            is Boolean -> XReadableType.Boolean
            is JSONObject -> XReadableType.Map
            is Int -> XReadableType.Int
            is Number -> XReadableType.Number
            is String -> XReadableType.String
            else -> XReadableType.Null
        }

    override fun keyIterator(): XKeyIterator = KeyIteratorImpl(origin.keys())

    override fun toMap(): Map<String, Any?> = JsonUtils.jsonToMap(origin)
}

internal class ReadableArrayImpl(
    private val origin: JSONArray,
) : XReadableArray {
    override fun size(): Int = origin.length()

    override fun isNull(index: Int): Boolean = origin.isNull(index)

    override fun getBoolean(index: Int): Boolean = origin.optBoolean(index)

    override fun getDouble(index: Int): Double = origin.optDouble(index)

    override fun getInt(index: Int): Int = origin.optInt(index)

    override fun getString(index: Int): String = origin.optString(index)

    override fun getArray(index: Int): XReadableArray? {
        val res = origin.optJSONArray(index)
        return if (res == null) {
            null
        } else {
            ReadableArrayImpl(res)
        }
    }

    override fun getMap(index: Int): XReadableMap? {
        val res = origin.optJSONObject(index)
        return if (res == null) {
            null
        } else {
            ReadableMapImpl(res)
        }
    }

    override fun get(index: Int): XDynamic = DynamicImpl(origin.opt(index))

    override fun getType(index: Int): XReadableType =
        when (origin.opt(index)) {
            is JSONArray -> XReadableType.Array
            is Boolean -> XReadableType.Boolean
            is JSONObject -> XReadableType.Map
            is Number -> XReadableType.Number
            is String -> XReadableType.String
            else -> XReadableType.Null
        }

    override fun toList(): List<Any?> = JsonUtils.jsonToList(origin)
}

internal class DynamicImpl(
    private val origin: Any?,
) : XDynamic {
    override fun isNull(): Boolean = origin == null

    override fun getType(): XReadableType =
        when (origin) {
            is JSONArray -> XReadableType.Array
            is Boolean -> XReadableType.Boolean
            is JSONObject -> XReadableType.Map
            is Number -> XReadableType.Number
            is String -> XReadableType.String
            else -> XReadableType.Null
        }

    override fun asBoolean(): Boolean {
        if (origin is Boolean) {
            return origin
        } else {
            throw Exception("Dynamic is not Boolean")
        }
    }

    override fun asDouble(): Double =
        when (origin) {
            is Double -> origin
            is Int -> origin.toDouble()
            is Float -> origin.toDouble()
            is Long -> origin.toDouble()
            else -> throw Exception("Dynamic is not Double")
        }

    override fun asInt(): Int {
        if (origin is Int) {
            return origin
        } else {
            throw Exception("Dynamic is not Int")
        }
    }

    override fun asString(): String {
        if (origin is String) {
            return origin
        } else {
            throw Exception("Dynamic is not String")
        }
    }

    override fun asArray(): XReadableArray? {
        if (origin is JSONArray) {
            return ReadableArrayImpl(origin)
        } else {
            throw Exception("Dynamic is not JSONArray")
        }
    }

    override fun asMap(): XReadableMap? {
        if (origin is JSONObject) {
            return ReadableMapImpl(origin)
        } else {
            throw Exception("Dynamic is not JSONObject")
        }
    }

    override fun recycle() {
    }
}

internal class KeyIteratorImpl(
    private val origin: Iterator<String>,
) : XKeyIterator {
    override fun hasNextKey(): Boolean = origin.hasNext()

    override fun nextKey(): String = origin.next()
}
