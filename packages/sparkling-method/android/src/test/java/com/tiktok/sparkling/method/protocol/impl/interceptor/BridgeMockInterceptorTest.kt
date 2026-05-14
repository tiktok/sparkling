// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.protocol.impl.interceptor

import com.tiktok.sparkling.method.protocol.BridgeContext
import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.protocol.entity.BridgeResult
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class BridgeMockInterceptorTest {
    private lateinit var interceptor: BridgeMockInterceptor
    private val mockBridgeCall = Mockito.mock(BridgeCall::class.java)
    private val mockBridgeResult = Mockito.mock(BridgeResult::class.java)
    private val mockBridgeContext = Mockito.mock(BridgeContext::class.java)

    @Before
    fun setUp() {
        interceptor = BridgeMockInterceptor()
    }

    @Test
    fun testInterceptBridgeCall() {
        val result = interceptor.interceptBridgeCall(mockBridgeCall)
        assertEquals(mockBridgeCall, result)
    }

    @Test
    fun testInterceptBridgeResult() {
        val result = interceptor.interceptBridgeResult(mockBridgeCall, mockBridgeResult)
        assertEquals(mockBridgeResult, result)
    }

    @Test
    fun testInvokeBridgeResult() {
        val result = interceptor.invokeBridgeResult(mockBridgeCall)
        assertNull(result)
    }

    @Test
    fun testInterceptJSBEvent() {
        val data = JSONObject()
        interceptor.interceptJSBEvent(mockBridgeContext, "testEvent", data)
        // No assertion, just ensuring no exception is thrown
    }
}
