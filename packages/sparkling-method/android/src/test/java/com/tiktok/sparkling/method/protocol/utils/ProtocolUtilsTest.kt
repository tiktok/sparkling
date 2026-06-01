// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.protocol.utils

import com.tiktok.sparkling.method.registry.api.SparklingBridge
import org.json.JSONObject
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [LogUtils] and [MonitorUtils]. The actual logging implementation
 * is provided by android.util.Log (Robolectric) and the monitoring entry
 * points are no-op TODO implementations, so these tests verify that the
 * methods are reachable both when isDebugEnv is true and false.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ProtocolUtilsTest {
    @After
    fun tearDown() {
        SparklingBridge.isDebugEnv = false
    }

    @Test
    fun logUtilsExecutesAllLevelsWhenDebugEnabled() {
        SparklingBridge.isDebugEnv = true
        LogUtils.e("tag", "err")
        LogUtils.d("tag", "dbg")
        LogUtils.w("tag", "warn")
        LogUtils.i("tag", "info")
        LogUtils.v("tag", "verbose")
    }

    @Test
    fun logUtilsIsNoopWhenDebugDisabled() {
        SparklingBridge.isDebugEnv = false
        LogUtils.e("tag", "err")
        LogUtils.d("tag", "dbg")
        LogUtils.w("tag", "warn")
        LogUtils.i("tag", "info")
        LogUtils.v("tag", "verbose")
    }

    @Test
    fun monitorUtilsCustomReportOverloads() {
        // Each overload eventually delegates to the 4-arg variant which is a
        // no-op TODO. We just want to make sure each entry point is reachable.
        MonitorUtils.customReport("evt1", JSONObject().put("k", "v"))
        MonitorUtils.customReport(
            "evt2",
            JSONObject().put("c", 1),
            JSONObject().put("m", 2),
        )
        MonitorUtils.customReport(
            "evt3",
            JSONObject().put("c", 3),
            JSONObject().put("m", 4),
            "extra",
        )
        MonitorUtils.customReport("evt4", null as JSONObject?)
        MonitorUtils.customReport("evt5", null as JSONObject?, null as JSONObject?)
        MonitorUtils.customReport("evt6", null, null, null)
    }

    @Test
    fun monitorUtilsLifecycleReports() {
        MonitorUtils.lifecycleReportPV(hashMapOf("page" to "home"))
        MonitorUtils.lifecycleReportPerf(hashMapOf("metric" to 1.0))
    }
}
