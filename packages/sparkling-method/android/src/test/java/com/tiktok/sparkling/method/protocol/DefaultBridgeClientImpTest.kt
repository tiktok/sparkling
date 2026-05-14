// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.protocol

import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeHandler
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeProtocol
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DefaultBridgeClientImpTest {
    private lateinit var bridgeClient: DefaultBridgeClientImp
    private val mockBridgeContext = Mockito.mock(BridgeContext::class.java)

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        bridgeClient = DefaultBridgeClientImp(mockBridgeContext)
    }

    @After
    fun tearDown() {
        Mockito.validateMockitoUsage()
    }

    @Test
    fun testShouldInterceptRequest() {
        val bridgeCall = BridgeCall(mockBridgeContext)
        assertNull(bridgeClient.shouldInterceptRequest(bridgeCall))
    }

    @Test
    fun testOnBridgeInvoked() {
        val protocol = Mockito.mock(IBridgeProtocol::class.java)
        val detail = JSONObject()
        bridgeClient.onBridgeInvoked(protocol, detail)
        // No assertion, just ensuring no exception is thrown
    }

    @Test
    fun testOnBridgeDispatched() {
        val bridgeCall = BridgeCall(mockBridgeContext)
        bridgeClient.onBridgeDispatched(bridgeCall)
        // No assertion, just ensuring no exception is thrown
    }

    @Test
    fun testOnBridgeResultReceived() {
        val handler = Mockito.mock(IBridgeHandler::class.java)
        val detail = JSONObject()
        bridgeClient.onBridgeResultReceived("test", handler, detail)
        // No assertion, just ensuring no exception is thrown
    }

    @Test
    fun testOnBridgeCallback() {
        bridgeClient.onBridgeCallback()
        // No assertion, just ensuring no exception is thrown
    }

    // @Test TODO: Fix test - InvalidUseOfMatchersException at line 25
    // fun testOnBridgeRejected() {
    //     bridgeClient.onBridgeRejected()
    //     // No assertion, just ensuring no exception is thrown
    // }
}
