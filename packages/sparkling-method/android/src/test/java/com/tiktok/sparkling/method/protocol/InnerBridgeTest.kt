// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.protocol

import android.content.Context
import com.tiktok.sparkling.method.registry.api.SparklingBridge
import com.lynx.tasm.LynxView
import com.lynx.tasm.LynxViewBuilder
import com.lynx.tasm.LynxBackgroundRuntime
import com.lynx.tasm.LynxBackgroundRuntimeOptions
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeHandler
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeLifeClient
import com.tiktok.sparkling.method.protocol.impl.monitor.IBridgeMonitor
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class InnerBridgeTest {
    private lateinit var innerBridge: InnerBridge
    private val mockContext = Mockito.mock(Context::class.java)
    private val mockSparklingBridge = Mockito.mock(SparklingBridge::class.java)

    @Before
    fun setUp() {
        innerBridge = InnerBridge()
    }

    @Test
    fun testInitWithView() {
        val lynxView = Mockito.mock(LynxView::class.java)
        innerBridge.init(lynxView, "testContainer", null, mockSparklingBridge)
        assertNotNull(innerBridge.getBridgeContext().sparklingBridge)
        assertEquals("testContainer", innerBridge.getBridgeContext().containerId)
    }

    @Test
    fun testInitLynxJSRuntime() {
        val options = LynxBackgroundRuntimeOptions()
        innerBridge.initLynxJSRuntime("testContainer", options, mockContext, mockSparklingBridge)
        assertNotNull(innerBridge.getBridgeContext().sparklingBridge)
        assertEquals("testContainer", innerBridge.getBridgeContext().containerId)
    }

    @Test
    fun testRegisterIBridgeLifeClient() {
        val lifeClient = Mockito.mock(IBridgeLifeClient::class.java)
        innerBridge.registerIBridgeLifeClient(lifeClient)
        // Cannot directly assert, but we can ensure no exception is thrown
    }

    @Test
    fun testRegisterLynxModule() {
        val builder = Mockito.mock(LynxViewBuilder::class.java)
        innerBridge.registerLynxModule(builder, "testContainer")
        assertEquals("testContainer", innerBridge.getBridgeContext().containerId)
    }

    @Test
    fun testRegisterHandler() {
        val handler = Mockito.mock(IBridgeHandler::class.java)
        innerBridge.registerHandler(handler)
        // Cannot directly assert, but we can ensure no exception is thrown
    }

    @Test
    fun testRegisterMonitor() {
        val monitor = Mockito.mock(IBridgeMonitor::class.java)
        innerBridge.registerMonitor(monitor)
        assertTrue(innerBridge.getBridgeContext().monitor.contains(monitor))
    }

    @Test
    fun testSendEvent() {
        val monitor = Mockito.mock(IBridgeMonitor::class.java)
        innerBridge.registerMonitor(monitor)
        val data = JSONObject()
        innerBridge.sendEvent("testEvent", data)
        Mockito.verify(monitor).onBridgeEvent("testEvent", data)
    }

    @Test
    fun testSendJSRuntimeEvent() {
        val monitor = Mockito.mock(IBridgeMonitor::class.java)
        innerBridge.registerMonitor(monitor)
        val data = JSONObject()
        innerBridge.sendJSRuntimeEvent("testEvent", data)
        Mockito.verify(monitor).onBridgeEvent("testEvent", data)
    }

    @Test
    fun testBindLynxJSRuntime() {
        val runtime = Mockito.mock(LynxBackgroundRuntime::class.java)
        innerBridge.bindLynxJSRuntime(runtime)
        assertEquals(runtime, innerBridge.getBridgeContext().lynxBackgroundRuntime)
    }
}
