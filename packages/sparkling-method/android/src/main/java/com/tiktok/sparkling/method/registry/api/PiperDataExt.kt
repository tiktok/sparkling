// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.registry.api

import com.lynx.react.bridge.PiperData
import org.json.JSONObject

/**
 */

internal fun PiperData.toJSONObject(): JSONObject =
    try {
        when (this.dataType) {
            PiperData.DataType.String.ordinal -> {
                val rawData = this.rawData
                if (rawData is String) {
                    JSONObject(rawData)
                } else {
                    JSONObject()
                }
            }

            PiperData.DataType.Map.ordinal -> {
                val rawData = this.rawData
                if (rawData is Map<*, *>) {
                    JSONObject(rawData)
                } else {
                    JSONObject()
                }
            }

            else -> {
                JSONObject()
            }
        }
    } catch (e: Exception) {
        JSONObject()
    }

internal fun PiperData.stringify(): String =
    when (this.dataType) {
        PiperData.DataType.String.ordinal -> {
            this.rawData as? String
        }

        PiperData.DataType.Map.ordinal -> {
            val rawData = this.rawData
            if (rawData is Map<*, *>) {
                JSONObject(rawData).toString()
            } else {
                null
            }
        }

        else -> {
            null
        }
    } ?: ""

internal fun Map<*, *>.containsPiperData(): Boolean {
    this.forEach { (_, v) ->
        if (v is PiperData) {
            return true
        }
        if (v is Map<*, *> && v.containsPiperData()) {
            return true
        }
    }
    return false
}
