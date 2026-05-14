// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.api.util

import com.tiktok.sparkling.method.protocol.interfaces.IBridgeMethodCallback
import org.json.JSONObject

object BridgeMethodCallbackHelper {
    fun bridgeNotFound(callback: IBridgeMethodCallback) {
        val jsonObject =
            JSONObject().apply {
                put("code", -2)
                put("data", JSONObject())
                put("msg", "The JSBridge method is not found, please register")
            }
        callback.onBridgeResult(jsonObject)
    }

    fun bridgeNoPermission(callback: IBridgeMethodCallback) {
        val jsonObject =
            JSONObject().apply {
                put("code", -1)
                put("data", JSONObject())
                put("msg", "The URL is not authorized to call this JSBridge method")
            }
        callback.onBridgeResult(jsonObject)
    }
}
