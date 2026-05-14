// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core.model.idl

import androidx.annotation.Keep
import com.tiktok.sparkling.method.registry.core.XReadableMap
import com.tiktok.sparkling.method.registry.core.XReadableType
import com.tiktok.sparkling.method.registry.core.optDouble
import com.tiktok.sparkling.method.registry.core.optInt

/**
 * Desc:
 */
@Keep
interface IDLMethodBaseParamModel : IDLMethodBaseModel

fun IDLMethodBaseParamModel.getIntValue(
    params: XReadableMap,
    name: String,
    defaultValue: Int,
): Int =
    when {
        !params.hasKey(name) -> {
            defaultValue
        }

        params.get(name).getType() == XReadableType.Int -> {
            params.optInt(name, defaultValue)
        }

        params.get(name).getType() == XReadableType.Number -> {
            params.optDouble(name, defaultValue.toDouble()).toInt()
        }

        else -> {
            defaultValue
        }
    }

fun IDLMethodBaseParamModel.getLongValue(
    params: XReadableMap,
    name: String,
    defaultValue: Long = 0,
): Long? =
    when (params.get(name).getType()) {
        XReadableType.Int -> {
            params.getInt(name).toLong()
        }

        XReadableType.Number -> {
            params.getDouble(name).toLong()
        }

        else -> {
            null
        }
    }

fun IDLMethodBaseParamModel.getBooleanValue(
    params: XReadableMap,
    name: String,
): Boolean? =
    if (params.get(name).getType() == XReadableType.Boolean) {
        params.getBoolean(name)
    } else {
        null
    }
