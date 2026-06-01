// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.registry.api.util

import com.tiktok.sparkling.method.protocol.interfaces.IBridgeMethodCallback
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BridgeMethodCallbackHelperTest {
    private class CapturingCallback : IBridgeMethodCallback {
        var lastResult: Any? = null

        override fun onBridgeResult(parcel: Any) {
            lastResult = parcel
        }
    }

    @Test
    fun bridgeNotFoundDeliversExpectedJson() {
        val callback = CapturingCallback()
        BridgeMethodCallbackHelper.bridgeNotFound(callback)

        val payload = callback.lastResult as JSONObject
        assertEquals(-2, payload.getInt("code"))
        assertEquals("The JSBridge method is not found, please register", payload.getString("msg"))
        assertEquals(0, payload.getJSONObject("data").length())
    }

    @Test
    fun bridgeNoPermissionDeliversExpectedJson() {
        val callback = CapturingCallback()
        BridgeMethodCallbackHelper.bridgeNoPermission(callback)

        val payload = callback.lastResult as JSONObject
        assertEquals(-1, payload.getInt("code"))
        assertEquals(
            "The URL is not authorized to call this JSBridge method",
            payload.getString("msg"),
        )
        assertEquals(0, payload.getJSONObject("data").length())
    }
}
