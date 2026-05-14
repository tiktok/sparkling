// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.protocol

import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.api.BusinessCallHandler
import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeLifeClient
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeProtocol
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.After

class BridgeContextTest {
    private lateinit var bridgeContext: BridgeContext

    @Before
    fun setUp() {
        bridgeContext = BridgeContext()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testGetPlatformByBridgeContext() {
        bridgeContext.platform = BridgePlatformType.LYNX
        assertEquals(BridgePlatformType.LYNX, BridgeContext.getPlatformByBridgeContext(bridgeContext))

        bridgeContext.platform = BridgePlatformType.WEB
        assertEquals(BridgePlatformType.WEB, BridgeContext.getPlatformByBridgeContext(bridgeContext))

        bridgeContext.platform = BridgePlatformType.ALL
        assertEquals(BridgePlatformType.ALL, BridgeContext.getPlatformByBridgeContext(bridgeContext))
    }

    @Test
    fun testRegisterProtocol() {
        val protocol = mockk<IBridgeProtocol>()
        bridgeContext.registerProtocol(protocol)
        assertTrue(bridgeContext.protocols.contains(protocol))
    }

    @Test
    fun testShouldHandleWithBusinessHandler() {
        val businessCallHandler = mockk<BusinessCallHandler>()
        bridgeContext.businessCallHandler = businessCallHandler
        val bridgeCall = BridgeCall(bridgeContext)
        bridgeCall.bridgeName = "testBridge"
        bridgeCall.platform = BridgeCall.PlatForm.Web

        // Mock the getNameSpace method which is called by getNamespace()
        every { businessCallHandler.nameSpace } returns "testNamespace"
        every { businessCallHandler.getBridge(bridgeContext, "testBridge") } returns null

        assertFalse(bridgeContext.shouldHandleWithBusinessHandler(bridgeCall))
    }

    @Test
    fun testRegisterIBridgeLifeClient() {
        val lifeClient = mockk<IBridgeLifeClient>()
        bridgeContext.registerIBridgeLifeClient(lifeClient)

        val bridgeCall = BridgeCall(bridgeContext)
        bridgeCall.platform = BridgeCall.PlatForm.Web // Initialize platform

        every { lifeClient.onBridgeCalledStart(any(), any()) } just Runs

        bridgeContext.bridgeLifeClientImp.onBridgeCalledStart(bridgeCall, bridgeContext)

        verify { lifeClient.onBridgeCalledStart(bridgeCall, bridgeContext) }
    }

    @Test
    fun testGetCurrentUrl() {
        // Test without lynxView (should return null since getCurrentUrl returns String?)
        assertNull(bridgeContext.getCurrentUrl())

        // Test with lynxView = null explicitly
        bridgeContext.lynxView = null
        assertNull(bridgeContext.getCurrentUrl())
    }
}
