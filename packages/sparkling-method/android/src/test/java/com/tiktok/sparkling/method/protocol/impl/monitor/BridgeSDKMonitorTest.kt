// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.protocol.impl.monitor

import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BridgeSDKMonitorTest {
    @Test
    fun appInfoDefaultsAreNullAndCopyableViaDataClass() {
        val info = BridgeSDKMonitor.APPInfo4Monitor()
        assertNull(info.appVersion)
        assertNull(info.update_version_code)
        assertNull(info.channel)
        assertNull(info.sdkVersion)

        val populated =
            info.copy(
                appVersion = "1.2.3",
                update_version_code = "10203",
                channel = "playstore",
                sdkVersion = "42",
            )
        assertEquals("1.2.3", populated.appVersion)
        assertEquals("10203", populated.update_version_code)
        assertEquals("playstore", populated.channel)
        assertEquals("42", populated.sdkVersion)
        assertNotEquals(info, populated)
        assertNotEquals(info.hashCode(), populated.hashCode())
        assertTrue(populated.toString().contains("1.2.3"))
    }

    @Test
    fun containerTypeEnumExposesType() {
        assertEquals("lynx", BridgeSDKMonitor.ContainerType.LYNX.type)
        assertEquals("h5", BridgeSDKMonitor.ContainerType.H5.type)
        assertEquals(2, BridgeSDKMonitor.ContainerType.values().size)
        assertEquals(BridgeSDKMonitor.ContainerType.LYNX, BridgeSDKMonitor.ContainerType.valueOf("LYNX"))
    }

    @Test
    fun constructorRecordsChannelOnCompanion() {
        BridgeSDKMonitor.channel_ = "" // reset
        BridgeSDKMonitor(
            ApplicationProvider.getApplicationContext(),
            BridgeSDKMonitor.APPInfo4Monitor(channel = "release"),
        )
        assertEquals("release", BridgeSDKMonitor.channel_)
    }

    @Test
    fun constructorAcceptsNullChannel() {
        BridgeSDKMonitor.channel_ = "release"
        BridgeSDKMonitor(
            ApplicationProvider.getApplicationContext(),
            BridgeSDKMonitor.APPInfo4Monitor(),
        )
        assertEquals("", BridgeSDKMonitor.channel_)
    }

    @Test
    fun monitorEventDoesNotThrowOnFullModel() {
        val monitor =
            BridgeSDKMonitor(
                ApplicationProvider.getApplicationContext(),
                BridgeSDKMonitor.APPInfo4Monitor(),
            )
        val model =
            BridgeSDKMonitor.MonitorModel
                .Builder()
                .setMethod("foo.bar")
                .setURL("https://example.com")
                .setCode(0)
                .setChannel("release")
                .setContainerType("lynx")
                .setRequestDataLength(123)
                .setRequestSendTimestamp(1L)
                .setRequestReceiveTimestamp(5L)
                .setRequestDecodeDuration(2L)
                .setRequestDuration()
                .setResponseDataLength(321)
                .setResponseSendTimestamp(7L)
                .setResponseReceiveTimestamp(11L)
                .setResponseEncodeDuration(3L)
                .setResponseDuration()
                .setDuration()
                .build()
        // Should not throw – exercises the full JSON serialization path.
        monitor.monitorEvent(model)
    }

    @Test
    fun builderComputesDurationsWhenTimestampsArePresent() {
        val model =
            BridgeSDKMonitor.MonitorModel
                .Builder()
                .setRequestSendTimestamp(100L)
                .setRequestReceiveTimestamp(150L)
                .setResponseSendTimestamp(200L)
                .setResponseReceiveTimestamp(260L)
                .setRequestDuration()
                .setResponseDuration()
                .setDuration()
                .build()
        assertEquals(50L, model.request_duration)
        assertEquals(60L, model.response_duration)
        assertEquals(160L, model.duration)
    }

    @Test
    fun builderLeavesDurationsNullWhenTimestampsMissing() {
        val model =
            BridgeSDKMonitor.MonitorModel
                .Builder()
                .setRequestDuration()
                .setResponseDuration()
                .setDuration()
                .build()
        assertNull(model.request_duration)
        assertNull(model.response_duration)
        assertNull(model.duration)
    }

    @Test
    fun monitorModelCopyAndEquality() {
        val a =
            BridgeSDKMonitor.MonitorModel(
                method = "foo",
                code = 0,
                channel = "ch",
                containerType = "lynx",
                duration = 10L,
                url = "u",
                request_data_length = 1,
                request_send_timestamp = 2L,
                request_receive_timestamp = 3L,
                request_decode_duration = 4L,
                request_duration = 5L,
                response_data_length = 6,
                response_encode_duration = 7L,
                response_send_timestamp = 8L,
                response_receive_timestamp = 9L,
                response_duration = 10L,
            )
        val b = a.copy(method = "bar")
        assertNotEquals(a, b)
        assertEquals("bar", b.method)
        assertEquals(a.code, b.code)
        assertNotNull(a.toString())
    }

    @Test
    fun bridgeMonitorDefaultOnBridgeEventDoesNotThrow() {
        val sentinel =
            object : IBridgeMonitor {
                var rejected = false
                var resolved = false

                override fun onBridgeRejected(entity: MonitorEntity) {
                    rejected = true
                }

                override fun onBridgeResolved(entity: MonitorEntity) {
                    resolved = true
                }
            }
        // Default impl just logs – should not throw with non-null and null payloads.
        sentinel.onBridgeEvent("event", JSONObject().put("k", "v"))
        sentinel.onBridgeEvent("event", null)

        sentinel.onBridgeRejected(MonitorEntity())
        sentinel.onBridgeResolved(MonitorEntity())
        assertTrue(sentinel.rejected)
        assertTrue(sentinel.resolved)
    }
}
