// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.protocol

import com.tiktok.sparkling.method.protocol.entity.Platform
import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.protocol.entity.BridgeResult
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeLifeClient
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class DefaultBridgeLifeClientImpTest {
    private lateinit var lifeClient: DefaultBridgeLifeClientImp
    private lateinit var mockBridgeContext: BridgeContext
    private lateinit var mockBridgeCall: BridgeCall

    @Before
    fun setUp() {
        mockBridgeContext = Mockito.mock(BridgeContext::class.java)
        mockBridgeCall = Mockito.mock(BridgeCall::class.java)

        // Set up the BridgeCall mock
        Mockito.`when`(mockBridgeCall.context).thenReturn(mockBridgeContext)

        lifeClient = DefaultBridgeLifeClientImp(mockBridgeContext)
    }

    @Test
    fun testRegisterIBridgeLifeClient() {
        val mockLifeClient = Mockito.mock(IBridgeLifeClient::class.java)
        lifeClient.registerIBridgeLifeClient(mockLifeClient)
        // Cannot directly access bridgeLifeClients, so we can't assert here.
        // We can only ensure no exception is thrown.
    }

    @Test
    fun testShouldHandleBridgeCall() {
        val result = lifeClient.shouldHandleBridgeCall(mockBridgeCall, mockBridgeContext)
        assertTrue(result.shouldHandleBridgeCall)
        assertNull(result.reason)
    }

    @Test
    fun testOnBridgeCalledStart() {
        val child = Mockito.mock(IBridgeLifeClient::class.java)
        lifeClient.registerIBridgeLifeClient(child)
        Mockito.`when`(mockBridgeCall.platform).thenReturn(Platform.Other)

        lifeClient.onBridgeCalledStart(mockBridgeCall, mockBridgeContext)

        Mockito.verify(child).onBridgeCalledStart(mockBridgeCall, mockBridgeContext)
    }

    @Test
    fun testOnBridgeImplHandleStart() {
        lifeClient.onBridgeImplHandleStart(mockBridgeCall, mockBridgeContext)
        // No assertion, just ensuring no exception is thrown
    }

    @Test
    fun testOnBridgeImplHandleEnd() {
        lifeClient.onBridgeImplHandleEnd(mockBridgeCall, mockBridgeContext)
        // No assertion, just ensuring no exception is thrown
    }

    @Test
    fun testOnBridgeCallbackCallStart() {
        val mockResult = Mockito.mock(BridgeResult::class.java)
        Mockito.`when`(mockBridgeCall.platform).thenReturn(Platform.Other)
        lifeClient.onBridgeCallbackCallStart(mockResult, mockBridgeCall, mockBridgeContext)
        // No assertion, just ensuring no exception is thrown
    }

    @Test
    fun testOnBridgeCallbackInvokeStart() {
        val mockResult = Mockito.mock(BridgeResult::class.java)
        Mockito.`when`(mockBridgeCall.platform).thenReturn(Platform.Other)
        lifeClient.onBridgeCallbackInvokeStart(mockResult, mockBridgeCall)
        // No assertion, just ensuring no exception is thrown
    }

    @Test
    fun testDelegatesEventCallbacksToRegisteredLifeClient() {
        val child = Mockito.mock(IBridgeLifeClient::class.java)
        lifeClient.registerIBridgeLifeClient(child)

        lifeClient.onBridgeEventStart("evt", mapOf("k" to "v"))
        lifeClient.onBridgeEventEnd("evt", mapOf("k" to "v"))

        Mockito.verify(child).onBridgeEventStart("evt", mapOf("k" to "v"))
        Mockito.verify(child).onBridgeEventEnd("evt", mapOf("k" to "v"))
    }

    @Test
    fun testOnBridgeCalledEndDelegates() {
        val child = Mockito.mock(IBridgeLifeClient::class.java)
        lifeClient.registerIBridgeLifeClient(child)
        Mockito.`when`(mockBridgeCall.platform).thenReturn(Platform.Other)

        lifeClient.onBridgeCalledEnd(mockBridgeCall, mockBridgeContext)

        Mockito.verify(child).onBridgeCalledEnd(mockBridgeCall, mockBridgeContext)
    }
}
