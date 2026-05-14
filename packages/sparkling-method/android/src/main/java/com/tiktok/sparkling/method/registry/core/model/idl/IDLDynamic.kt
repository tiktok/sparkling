// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core.model.idl

import androidx.annotation.Keep
import com.tiktok.sparkling.method.registry.core.utils.JsonUtils
import org.json.JSONObject

/**
 * Desc:
 */
@Keep
interface IDLDynamic {
    fun isNull(): Boolean

    fun asBoolean(): Boolean

    fun asDouble(): Double

    fun asInt(): Int

    fun asLong(): Long

    fun asString(): String

    fun asArray(): List<Any>

    fun asMap(): Map<String, Any>

    fun asByteArray(): ByteArray

    fun getType(): DynamicType

    fun recycle()
}

fun IDLDynamic.getValue(): Any? {
    when (getType()) {
        DynamicType.String -> {
            return asString()
        }

        DynamicType.Number -> {
            return asDouble()
        }

        DynamicType.Boolean -> {
            return asBoolean()
        }

        DynamicType.Long -> {
            return asLong()
        }

        DynamicType.Int -> {
            return asInt()
        }

        DynamicType.Map -> {
            return asMap()
        }

        DynamicType.Array -> {
            return asArray()
        }

        DynamicType.ByteArray -> {
            return asByteArray()
        }

        DynamicType.Null -> {
            return null
        }
    }
}

fun IDLDynamic.toPrimitiveOrJSON(): Any? {
    when (getType()) {
        DynamicType.String -> {
            return asString()
        }

        DynamicType.Number -> {
            return asDouble()
        }

        DynamicType.Boolean -> {
            return asBoolean()
        }

        DynamicType.Long -> {
            return asLong()
        }

        DynamicType.Int -> {
            return asInt()
        }

        DynamicType.Map -> {
            return JsonUtils.toJSONObject(asMap())
        }

        DynamicType.Array -> {
            return JsonUtils.toJSONArray(asArray())
        }

        DynamicType.ByteArray -> {
            return asByteArray()
        }

        DynamicType.Null -> {
            return JSONObject.NULL
        }
    }
}

@Keep
enum class DynamicType {
    Null,
    Boolean,
    Int,
    Number,
    String,
    Map,
    Array,
    Long,
    ByteArray,
}
