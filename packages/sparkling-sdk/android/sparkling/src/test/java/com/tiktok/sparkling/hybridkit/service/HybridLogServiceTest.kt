// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.service

import com.tiktok.sparkling.hybridkit.utils.LogLevel
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class HybridLogServiceTest {
    private val service = HybridLogService()

    @Test
    fun onLogDoesNotThrowForAllLevels() {
        // Robolectric provides android.util.Log, so each branch executes.
        service.onLog("debug-message", LogLevel.D, "TestTag")
        service.onLog("verbose-message", LogLevel.V, "TestTag")
        service.onLog("info-message", LogLevel.I, "TestTag")
        service.onLog("warn-message", LogLevel.W, "TestTag")
        service.onLog("error-message", LogLevel.E, "TestTag")
    }

    @Test
    fun onRejectLogsWithoutThrowing() {
        service.onReject(IllegalStateException("boom"), "extra-info", "TestTag")
    }
}
