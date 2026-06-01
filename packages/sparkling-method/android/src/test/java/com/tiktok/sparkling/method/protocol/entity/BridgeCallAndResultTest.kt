// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.protocol.entity

import com.lynx.react.bridge.JavaOnlyMap
import com.tiktok.sparkling.method.protocol.BridgeContext
import com.tiktok.sparkling.method.registry.api.BridgeSettings
import com.tiktok.sparkling.method.registry.api.SparklingBridge
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BridgeCallAndResultTest {
    @After
    fun tearDown() {
        BridgeSettings.bridgeCallToStringOptimization = false
        SparklingBridge.isDebugEnv = false
    }

    @Test
    fun bridgeCallExposesDefaultsAndPlatformEnum() {
        val ctx = BridgeContext()
        val call = BridgeCall(ctx)

        assertEquals("", call.callbackId)
        assertEquals("", call.bridgeName)
        assertEquals("", call.url)
        assertEquals("", call.msgType)
        assertEquals(-1, call.timestamp)
        assertEquals(false, call.hitBusinessHandler)
        assertEquals(CancelCallbackType.NONE, call.cancelCallBack)
        assertEquals("DEFAULT", BridgeCall.DEFAULT_NAMESPACE)

        // Touch every PlatForm enum value.
        assertEquals(3, BridgeCall.PlatForm.values().size)
        assertEquals(BridgeCall.PlatForm.Lynx, BridgeCall.PlatForm.valueOf("Lynx"))
        assertEquals(BridgeCall.PlatForm.Web, BridgeCall.PlatForm.valueOf("Web"))
        assertEquals(BridgeCall.PlatForm.Other, BridgeCall.PlatForm.valueOf("Other"))

        // CancelCallbackType enum.
        assertEquals(3, CancelCallbackType.values().size)
        assertEquals(CancelCallbackType.ALL, CancelCallbackType.valueOf("ALL"))
        assertEquals(CancelCallbackType.ONLY_SUCCESS, CancelCallbackType.valueOf("ONLY_SUCCESS"))
    }

    @Test
    fun bridgeCallToStringIncludesAllFieldsByDefault() {
        BridgeSettings.bridgeCallToStringOptimization = false
        val call =
            BridgeCall(BridgeContext()).apply {
                callbackId = "cb1"
                bridgeName = "test.method"
                url = "https://x"
                msgType = "call"
                sdkVersion = "1.0"
                nameSpace = "ns"
                frameUrl = "frame"
                hitBusinessHandler = true
            }
        val s = call.toString()
        assertTrue(s.contains("cb1"))
        assertTrue(s.contains("test.method"))
        assertTrue(s.contains("frame"))
        assertTrue(s.contains("hitBusinessHandler='true'"))
    }

    @Test
    fun bridgeCallToStringRespectsOptimizationFlag() {
        BridgeSettings.bridgeCallToStringOptimization = true
        val call =
            BridgeCall(BridgeContext()).apply {
                callbackId = "id"
                bridgeName = "fn"
                url = "url"
            }
        val s = call.toString()
        assertTrue(s.contains("id"))
        assertTrue(s.contains("fn"))
        // Optimised toString omits url and other fields.
        assertFalse(s.contains("url='url'"))
    }

    @Test
    fun bridgeResultToJsonResultBuildsExpectedShape() {
        val data = JSONObject().put("k", 1)
        val result = BridgeResult.toJsonResult(IDLBridgeMethod.SUCCESS, "ok", data)
        val parcel = result.toJSONObject()
        assertEquals(IDLBridgeMethod.SUCCESS, parcel.getInt("code"))
        assertEquals("ok", parcel.getString("msg"))
        assertEquals(1, parcel.getJSONObject("data").getInt("k"))
    }

    @Test
    fun bridgeResultIsSuccessResultDispatchesAcrossSupportedTypes() {
        // JSONObject branch
        val jsonOk = BridgeResult(JSONObject().put("code", IDLBridgeMethod.SUCCESS))
        assertTrue(jsonOk.isSuccessResult())
        val jsonFail = BridgeResult(JSONObject().put("code", IDLBridgeMethod.FAIL))
        assertFalse(jsonFail.isSuccessResult())

        // Map branch
        val mapOk = BridgeResult(mapOf<String, Any?>("code" to IDLBridgeMethod.SUCCESS))
        assertTrue(mapOk.isSuccessResult())
        val mapFail = BridgeResult(mapOf<String, Any?>("code" to "wrong"))
        assertFalse(mapFail.isSuccessResult())

        // JavaOnlyMap branch
        val jom = JavaOnlyMap().apply { putInt("code", IDLBridgeMethod.SUCCESS) }
        assertTrue(BridgeResult(jom).isSuccessResult())

        // Unknown parcel type returns false.
        assertFalse(BridgeResult("string").isSuccessResult())
    }

    @Test
    fun bridgeResultToStringObservesDebugFlag() {
        val result = BridgeResult(JSONObject().put("code", IDLBridgeMethod.SUCCESS))
        SparklingBridge.isDebugEnv = false
        assertEquals("", result.toString())

        SparklingBridge.isDebugEnv = true
        val s = result.toString()
        assertNotNull(s)
        assertTrue(s.contains("\"code\""))
    }

    @Test
    fun bridgeResultToJsonObjectHandlesAllParcelShapes() {
        val map = mapOf<String, Any?>("k" to 1)
        val parcel = BridgeResult(map).toJSONObject()
        assertEquals(1, parcel.getInt("k"))

        val jom = JavaOnlyMap().apply { putString("k2", "v") }
        val converted = BridgeResult(jom).toJSONObject()
        assertEquals("v", converted.getString("k2"))

        val empty = BridgeResult("not-a-map").toJSONObject()
        assertEquals(0, empty.length())
    }

    @Test
    fun bridgeResultToJsonObjectWithCallReusesParcelHandling() {
        val parcel = JSONObject().put("code", 1)
        val result = BridgeResult(parcel)
        val ctx = BridgeContext()
        val call = BridgeCall(ctx)
        // BridgeCall arg is unused inside, but exercising the overload.
        assertEquals(parcel, result.toJSONObject(call))

        val mapResult = BridgeResult(mapOf<String, Any?>("k" to 1))
        assertEquals(1, mapResult.toJSONObject(null).getInt("k"))

        val unknown = BridgeResult("x")
        assertEquals(0, unknown.toJSONObject(null).length())
    }
}
