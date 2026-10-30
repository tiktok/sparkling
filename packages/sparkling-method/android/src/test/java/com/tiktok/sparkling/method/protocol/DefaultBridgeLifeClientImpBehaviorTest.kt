// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.protocol

import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.protocol.entity.BridgeResult
import com.tiktok.sparkling.method.protocol.entity.Platform
import com.tiktok.sparkling.method.protocol.impl.monitor.IBridgeMonitor
import com.tiktok.sparkling.method.protocol.impl.monitor.MonitorEntity
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeLifeClient
import com.tiktok.sparkling.method.protocol.interfaces.ShouldHandleBridgeCallResultModel
import com.tiktok.sparkling.method.protocol.utils.BridgeConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DefaultBridgeLifeClientImpBehaviorTest {
    private val context = BridgeContext()
    private val lifeClient = DefaultBridgeLifeClientImp(context)

    private fun call(platform: Platform = Platform.Web): BridgeCall =
        BridgeCall(context).apply {
            bridgeName = "app.test"
            nameSpace = "ns"
            this.platform = platform
        }

    private class RefusingClient(private val reason: String) : IBridgeLifeClient() {
        var asked = 0

        override fun shouldHandleBridgeCall(
            call: BridgeCall,
            bridgeContext: BridgeContext,
        ): ShouldHandleBridgeCallResultModel {
            asked++
            return ShouldHandleBridgeCallResultModel(false, reason)
        }
    }

    private class RecordingMonitor : IBridgeMonitor {
        val resolved = mutableListOf<MonitorEntity>()
        val rejected = mutableListOf<MonitorEntity>()

        override fun onBridgeResolved(entity: MonitorEntity) {
            resolved += entity
        }

        override fun onBridgeRejected(entity: MonitorEntity) {
            rejected += entity
        }
    }

    @Test
    fun firstRefusingClientDecidesTheResult() {
        val first = RefusingClient("first")
        val second = RefusingClient("second")
        lifeClient.registerIBridgeLifeClient(first)
        lifeClient.registerIBridgeLifeClient(second)

        val result = lifeClient.shouldHandleBridgeCall(call(), context)

        assertFalse(result.shouldHandleBridgeCall)
        assertEquals("first", result.reason)
        assertEquals(0, second.asked)
    }

    @Test
    fun clientRegisteredTwiceIsVisitedOnce() {
        val client = RefusingClient("once")
        lifeClient.registerIBridgeLifeClient(client)
        lifeClient.registerIBridgeLifeClient(client)

        assertEquals(listOf(client), lifeClient.registeredClients)
    }

    @Test
    fun reportsWebCallResultsToMonitors() {
        val monitor = RecordingMonitor()
        context.bridgeMonitors.add(monitor)

        lifeClient.onBridgeCallbackInvokeStart(
            BridgeResult.toJsonResult(BridgeConstants.BRIDGE_CALL_SUCCESS, "ok"),
            call(Platform.Web),
        )
        lifeClient.onBridgeCallbackInvokeStart(
            BridgeResult.toJsonResult(BridgeConstants.BRIDGE_CALL_FAIL, "no"),
            call(Platform.Web),
        )

        assertEquals("app.test", monitor.resolved.single().name)
        assertEquals("ns", monitor.resolved.single().nameSpace)
        assertEquals(BridgeConstants.BRIDGE_CALL_FAIL, monitor.rejected.single().code)
        assertEquals("no", monitor.rejected.single().message)
    }

    @Test
    fun throwingMonitorDoesNotStopTheOthers() {
        val throwing =
            object : IBridgeMonitor {
                override fun onBridgeResolved(entity: MonitorEntity): Unit = throw IllegalStateException("boom")

                override fun onBridgeRejected(entity: MonitorEntity): Unit = throw IllegalStateException("boom")
            }
        val monitor = RecordingMonitor()
        context.bridgeMonitors.add(throwing)
        context.bridgeMonitors.add(monitor)

        lifeClient.onBridgeCallbackInvokeStart(
            BridgeResult.toJsonResult(BridgeConstants.BRIDGE_CALL_SUCCESS, "ok"),
            call(Platform.Lynx),
        )

        assertTrue(monitor.resolved.isNotEmpty())
    }
}
