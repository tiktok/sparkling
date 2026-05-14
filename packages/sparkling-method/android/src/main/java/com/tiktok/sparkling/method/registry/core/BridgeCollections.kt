// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

interface XReadableMap {
    fun hasKey(name: String): Boolean

    fun isNull(name: String): Boolean

    fun getBoolean(name: String): Boolean

    fun getDouble(name: String): Double

    fun getInt(name: String): Int

    fun getString(name: String): String

    fun getArray(name: String): XReadableArray?

    fun getMap(name: String): XReadableMap?

    fun get(name: String): XDynamic

    fun getType(name: String): XReadableType

    fun keyIterator(): XKeyIterator

    fun toMap(): Map<String, Any?>
}

fun XReadableMap.optString(
    name: String,
    defaultValue: String = "",
): String {
    if (!hasKey(name)) {
        return defaultValue
    }

    val value = get(name)
    return if (value.getType() == XReadableType.String) {
        value.asString()
    } else {
        defaultValue
    }
}

fun XReadableMap.optBoolean(
    name: String,
    defaultValue: Boolean = false,
): Boolean {
    if (!hasKey(name)) {
        return defaultValue
    }

    val value = get(name)
    return if (value.getType() == XReadableType.Boolean) {
        value.asBoolean()
    } else {
        defaultValue
    }
}

fun XReadableMap.optInt(
    name: String,
    defaultValue: Int = 0,
): Int {
    if (!hasKey(name)) {
        return defaultValue
    }

    val value = get(name)
    return if (value.getType() == XReadableType.Int) {
        value.asInt()
    } else {
        defaultValue
    }
}

fun XReadableMap.optDouble(
    name: String,
    defaultValue: Double = 0.0,
): Double {
    if (!hasKey(name)) {
        return defaultValue
    }

    val value = get(name)
    return if (value.getType() == XReadableType.Number) {
        value.asDouble()
    } else {
        defaultValue
    }
}

fun XReadableMap.optMap(
    name: String,
    defaultValue: XReadableMap? = null,
): XReadableMap? {
    if (!hasKey(name)) {
        return defaultValue
    }

    val value = get(name)
    return if (value.getType() == XReadableType.Map) {
        value.asMap()
    } else {
        defaultValue
    }
}

fun XReadableMap.optArray(
    name: String,
    defaultValue: XReadableArray? = null,
): XReadableArray? {
    if (!hasKey(name)) {
        return defaultValue
    }

    val value = get(name)
    return if (value.getType() == XReadableType.Array) {
        value.asArray()
    } else {
        defaultValue
    }
}

interface XReadableArray {
    fun size(): Int

    fun isNull(index: Int): Boolean

    fun getBoolean(index: Int): Boolean

    fun getDouble(index: Int): Double

    fun getInt(index: Int): Int

    fun getString(index: Int): String

    fun getArray(index: Int): XReadableArray?

    fun getMap(index: Int): XReadableMap?

    fun get(index: Int): XDynamic

    fun getType(index: Int): XReadableType

    fun toList(): List<Any?>
}

fun XReadableArray.toObjectList(): List<Any> {
    val data = mutableListOf<Any>()
    for (idx in 0 until size()) {
        when (getType(idx)) {
            XReadableType.String -> {
                data.add(getString(idx))
            }

            XReadableType.Number -> {
                data.add(getDouble(idx))
            }

            XReadableType.Boolean -> {
                data.add(getBoolean(idx))
            }

            XReadableType.Int -> {
                data.add(getInt(idx))
            }

            XReadableType.Map -> {
                getMap(idx)?.let {
                    data.add(it.toObjectMap())
                }
            }

            XReadableType.Array -> {
                getArray(idx)?.let {
                    data.add(it.toObjectList())
                }
            }

            else -> {
                // ignore
            }
        }
    }
    return data
}

fun XReadableMap.toObjectMap(): Map<String, Any> {
    val data = mutableMapOf<String, Any>()
    val iterator = keyIterator()
    while (iterator.hasNextKey()) {
        val key = iterator.nextKey()
        when (getType(key)) {
            XReadableType.String -> {
                data[key] = getString(key)
            }

            XReadableType.Number -> {
                data[key] = getDouble(key)
            }

            XReadableType.Boolean -> {
                data[key] = getBoolean(key)
            }

            XReadableType.Int -> {
                data[key] = getInt(key)
            }

            XReadableType.Map -> {
                getMap(key)?.let {
                    data[key] = it.toObjectMap()
                }
            }

            XReadableType.Array -> {
                getArray(key)?.let {
                    data[key] = it.toObjectList()
                }
            }

            else -> {
                // ignore
            }
        }
    }
    return data
}

interface XDynamic {
    fun isNull(): Boolean

    fun getType(): XReadableType

    fun asBoolean(): Boolean

    fun asDouble(): Double

    fun asInt(): Int

    fun asString(): String

    fun asArray(): XReadableArray?

    fun asMap(): XReadableMap?

    fun recycle()
}

interface XKeyIterator {
    fun hasNextKey(): Boolean

    fun nextKey(): String
}

enum class XReadableType {
    Null,
    Boolean,
    Number,
    Int,
    String,
    Map,
    Array,
}
