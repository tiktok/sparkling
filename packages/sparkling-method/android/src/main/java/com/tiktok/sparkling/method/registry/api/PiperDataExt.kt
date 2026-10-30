// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.api

import com.lynx.react.bridge.PiperData
import org.json.JSONObject

/**
 * Serializes the raw payload carried by a [PiperData] into its JSON string form,
 * falling back to an empty string for payload types that cannot be represented.
 */
internal fun PiperData.stringify(): String {
    return when (this.dataType) {
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

        else -> null
    } ?: ""
}

/**
 * Reports whether the map holds a [PiperData] value at any nesting depth.
 */
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
