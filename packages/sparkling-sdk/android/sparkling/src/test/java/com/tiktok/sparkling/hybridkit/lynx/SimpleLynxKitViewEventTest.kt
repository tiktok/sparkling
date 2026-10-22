// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.lynx

import com.lynx.react.bridge.JavaOnlyArray
import com.tiktok.sparkling.hybridkit.HybridContext
import com.tiktok.sparkling.method.protocol.impl.monitor.IBridgeMonitor
import com.tiktok.sparkling.method.protocol.impl.monitor.MonitorEntity
import com.tiktok.sparkling.method.registry.api.SparklingBridge
import io.mockk.every
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class SimpleLynxKitViewEventTest {
    @After
    fun tearDown() {
        unmockkAll()
    }

    @Suppress("DEPRECATION")
    @Test
    fun throwingMonitorDoesNotBlockLaterMonitorsOrListEventDispatch() {
        val calls = mutableListOf<String>()
        val bridge = SparklingBridge()
        bridge.registerMonitor(EventMonitor { _, _ ->
            calls.add("throwing monitor")
            throw IllegalStateException("monitor failure")
        })
        bridge.registerMonitor(EventMonitor { _, _ -> calls.add("following monitor") })
        val view = eventView(bridge)
        var sentName: String? = null
        var sentData: JavaOnlyArray? = null
        every { view.sendGlobalEvent(any(), any()) } answers {
            calls.add("Lynx dispatch")
            sentName = firstArg()
            sentData = secondArg()
            Unit
        }

        // Runs the real public SDK method and its private dispatch implementation.
        val failure = runCatching { view.sendEvent("updated", listOf("payload", 7)) }.exceptionOrNull()

        assertEquals(listOf("throwing monitor", "following monitor", "Lynx dispatch"), calls)
        assertNull("A monitoring failure must not escape to the event caller", failure)
        assertEquals("updated", sentName)
        assertEquals("payload", sentData?.getString(0))
        assertEquals(7, sentData?.getInt(1))
        verify(exactly = 1) { view.sendGlobalEvent("updated", any()) }
    }

    private fun eventView(bridge: SparklingBridge): SimpleLynxKitView {
        // Bypass the Lynx native constructor. The spy keeps sendEvent and its
        // private helper real; only the final native dispatch boundary is stubbed.
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val unsafe = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null)
        val raw = unsafeClass.getMethod("allocateInstance", Class::class.java)
            .invoke(unsafe, SimpleLynxKitView::class.java) as SimpleLynxKitView
        raw.hybridContext = HybridContext().apply { this.bridge = bridge }
        return spyk(raw, recordPrivateCalls = true)
    }

    private class EventMonitor(
        private val onEvent: (String, JSONObject?) -> Unit,
    ) : IBridgeMonitor {
        override fun onBridgeEvent(eventName: String, data: JSONObject?) = onEvent(eventName, data)

        override fun onBridgeRejected(entity: MonitorEntity) = Unit

        override fun onBridgeResolved(entity: MonitorEntity) = Unit
    }
}
