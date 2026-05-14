// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.protocol.entity

import com.tiktok.sparkling.method.registry.api.SparklingBridge
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.lynx.react.bridge.JavaOnlyMap
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BridgeResultTest {
    @Before
    fun setUp() {
        // Mock Android dependencies for testing
        System.setProperty("robolectric.offline", "true")
        SparklingBridge.isDebugEnv = true
    }

    @Test
    fun testToJsonResult() {
        val data = JSONObject().put("key", "value")
        val result = BridgeResult.toJsonResult(IDLBridgeMethod.SUCCESS, "success", data)
        val json = result.toJSONObject()
        assertEquals(IDLBridgeMethod.SUCCESS, json.getInt("code"))
        assertEquals("success", json.getString("msg"))
        assertEquals("value", json.getJSONObject("data").getString("key"))
    }

    @Test
    fun testToJSONObject() {
        val json = JSONObject().put("key", "value")
        val result = BridgeResult(json)
        assertEquals(json, result.toJSONObject())

        val map = mapOf("key" to "value")
        val resultFromMap = BridgeResult(map)
        assertEquals("value", resultFromMap.toJSONObject().getString("key"))

        val javaOnlyMap = JavaOnlyMap()
        javaOnlyMap.put("key", "value")
        val resultFromJavaOnlyMap = BridgeResult(javaOnlyMap)
        assertEquals("value", resultFromJavaOnlyMap.toJSONObject().getString("key"))
    }

    @Test
    fun testIsSuccessResult() {
        val successJson = JSONObject().put("code", IDLBridgeMethod.SUCCESS)
        val successResult = BridgeResult(successJson)
        assertTrue(successResult.isSuccessResult())

        val failJson = JSONObject().put("code", IDLBridgeMethod.FAIL)
        val failResult = BridgeResult(failJson)
        assertFalse(failResult.isSuccessResult())
    }

    @Test
    fun testToString() {
        val json = JSONObject().put("key", "value")
        val result = BridgeResult(json)
        assertEquals(json.toString(), result.toString())

        SparklingBridge.isDebugEnv = false
        assertEquals("", result.toString())
    }
}
