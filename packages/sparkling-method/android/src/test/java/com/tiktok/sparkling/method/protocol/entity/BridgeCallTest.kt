// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.protocol.entity

import com.tiktok.sparkling.method.protocol.BridgeContext
import com.tiktok.sparkling.method.registry.api.BridgeSettings
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class BridgeCallTest {
    private lateinit var bridgeCall: BridgeCall
    private val mockContext = mock(BridgeContext::class.java)

    @Before
    fun setUp() {
        bridgeCall = BridgeCall(mockContext)
    }

    @Test
    fun testToStringWithOptimization() {
        BridgeSettings.bridgeCallToStringOptimization = true
        bridgeCall.callbackId = "123"
        bridgeCall.bridgeName = "testBridge"
        assertEquals("BridgeCall(callbackId='123', bridgeName='testBridge')", bridgeCall.toString())
    }

    @Test
    fun testToStringWithoutOptimization() {
        BridgeSettings.bridgeCallToStringOptimization = false
        bridgeCall.callbackId = "123"
        bridgeCall.bridgeName = "testBridge"
        bridgeCall.hitBusinessHandler = true
        bridgeCall.url = "http://test.com"
        bridgeCall.msgType = "call"
        bridgeCall.params = mapOf<String, Any>("key" to "value")
        bridgeCall.sdkVersion = "1.0"
        bridgeCall.nameSpace = "test"
        bridgeCall.frameUrl = "http://test.com/frame"
        val expected = "BridgeCall(callbackId='123', bridgeName='testBridge', hitBusinessHandler='true', url='http://test.com', msgType='call', params='{key=value}', sdkVersion=1.0, nameSpace='test', frameUrl='http://test.com/frame')"
        assertEquals(expected, bridgeCall.toString())
    }

    @Test
    fun testDefaultValues() {
        assertEquals("", bridgeCall.callbackId)
        assertEquals("", bridgeCall.bridgeName)
        assertEquals(CancelCallbackType.NONE, bridgeCall.cancelCallBack)
        assertEquals(false, bridgeCall.hitBusinessHandler)
    }

    @Test
    fun testPlatFormEnum() {
        assertEquals(BridgeCall.PlatForm.Lynx, BridgeCall.PlatForm.valueOf("Lynx"))
        assertEquals(BridgeCall.PlatForm.Web, BridgeCall.PlatForm.valueOf("Web"))
        assertEquals(BridgeCall.PlatForm.Other, BridgeCall.PlatForm.valueOf("Other"))
    }
}
