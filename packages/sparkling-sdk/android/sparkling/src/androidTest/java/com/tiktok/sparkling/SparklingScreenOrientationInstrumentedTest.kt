// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SparklingScreenOrientationInstrumentedTest {
    @Test
    fun fullPageActivityReceivesPerContainerOrientationPolicy() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context =
            SparklingContext().apply {
                containerId = "instrumented-orientation-landscape"
                screenOrientationPolicy = SparklingScreenOrientationPolicy.LANDSCAPE
            }
        SparklingContextTransferStation.saveSparklingContext(context)
        val intent =
            Intent(instrumentation.targetContext, SparklingActivity::class.java).apply {
                putExtra(Sparkling.SPARKLING_CONTEXT_CONTAINER_ID, context.containerId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        val activity = instrumentation.startActivitySync(intent) as SparklingActivity

        try {
            instrumentation.waitForIdleSync()
            assertEquals(
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                activity.requestedOrientation,
            )
        } finally {
            instrumentation.runOnMainSync {
                activity.finish()
            }
            SparklingContextTransferStation.releaseSparklingContext(context.containerId)
            assertEquals(
                null,
                SparklingContextTransferStation.getSparklingContext(context.containerId),
            )
        }
    }
}
