// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.playground

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tiktok.sparkling.Sparkling
import com.tiktok.sparkling.SparklingActivity
import com.tiktok.sparkling.SparklingContext
import com.tiktok.sparkling.SparklingContextTransferStation
import com.tiktok.sparkling.SparklingScreenOrientationPolicy
import com.tiktok.sparkling.hybridkit.HybridCommon
import com.tiktok.sparkling.hybridkit.HybridKit
import com.tiktok.sparkling.hybridkit.config.SparklingHybridConfig
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaygroundOrientationDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val targetContext = instrumentation.targetContext
    private val application = targetContext.applicationContext as SparklingApplication
    private var currentActivity: Activity? = null
    private var activeContainerId: String? = null

    @After
    fun tearDown() {
        instrumentation.runOnMainSync {
            currentActivity?.finish()
            currentActivity = null
        }
        activeContainerId?.let(SparklingContextTransferStation::releaseSparklingContext)
        activeContainerId = null
        setGlobalDefault(null)
        instrumentation.waitForIdleSync()
    }

    @Test
    fun typedLandscapeFullPage() {
        val result =
            launchFullPage(
                name = "typed-landscape",
                policy = SparklingScreenOrientationPolicy.LANDSCAPE,
                expected = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                screenshotName = "typed-landscape",
            )
        assertTrue(result.screenshotWidth > result.screenshotHeight)
    }

    @Test
    fun typedPortraitFullPage() {
        val result =
            launchFullPage(
                name = "typed-portrait",
                policy = SparklingScreenOrientationPolicy.PORTRAIT,
                expected = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                screenshotName = "typed-portrait",
            )
        assertTrue(result.screenshotHeight > result.screenshotWidth)
    }

    @Test
    fun explicitSystemOverridesGlobalLandscape() {
        setGlobalDefault(SparklingScreenOrientationPolicy.LANDSCAPE)
        launchFullPage(
            name = "explicit-system-over-global",
            policy = SparklingScreenOrientationPolicy.SYSTEM,
            expected = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
        )
    }

    @Test
    fun canonicalScreenOrientationLandscape() {
        launchFullPage(
            name = "canonical-landscape",
            canonicalOrientation = "landscape",
            expected = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
        )
    }

    @Test
    fun unsetUsesAndroidSystemBehavior() {
        launchFullPage(
            name = "unset",
            expected = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
        )
    }

    @Test
    fun embeddedLandscapePolicyDoesNotChangeHostActivity() {
        val intent =
            Intent(targetContext, OrientationHostActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        val host = instrumentation.startActivitySync(intent) as OrientationHostActivity
        currentActivity = host
        instrumentation.runOnMainSync {
            host.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            val context =
                SparklingContext().apply {
                    scheme = "hybrid://lynxview_page?bundle=$BUNDLE_NAME"
                    screenOrientationPolicy = SparklingScreenOrientationPolicy.LANDSCAPE
                }
            assertTrue(Sparkling.build(host, context).createView(withoutPrepare = true) != null)
        }
        instrumentation.waitForIdleSync()
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
            host.requestedOrientation,
        )
        println(
            "ORIENTATION_GATE event=embedded requested=${host.requestedOrientation} " +
                "expected=${ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT}",
        )
    }

    private fun launchFullPage(
        name: String,
        policy: SparklingScreenOrientationPolicy? = null,
        canonicalOrientation: String? = null,
        expected: Int,
        screenshotName: String? = null,
    ): FullPageResult {
        val context =
            SparklingContext().apply {
                scheme =
                    buildString {
                        append("hybrid://lynxview_page?bundle=")
                        append(BUNDLE_NAME)
                        append("&hide_nav_bar=1")
                        canonicalOrientation?.let {
                            append("&screen_orientation=")
                            append(it)
                        }
                    }
                screenOrientationPolicy = policy
            }
        activeContainerId = context.containerId
        val monitor =
            instrumentation.addMonitor(SparklingActivity::class.java.name, null, false)
        assertTrue(Sparkling.build(targetContext, context).navigate())
        assertSame(
            context,
            SparklingContextTransferStation.getSparklingContext(context.containerId),
        )
        val activity =
            requireNotNull(
                instrumentation.waitForMonitorWithTimeout(monitor, TIMEOUT_MILLIS),
            ) as SparklingActivity
        currentActivity = activity
        assertTrue(
            "Timed out waiting for $name requestedOrientation=$expected",
            waitUntil {
                activity.requestedOrientation == expected
            },
        )
        instrumentation.waitForIdleSync()
        SystemClock.sleep(ORIENTATION_SETTLE_MILLIS)
        val screenshot = screenshotName?.let(::saveScreenshot)
        assertEquals(expected, activity.requestedOrientation)
        instrumentation.runOnMainSync {
            activity.finish()
        }
        instrumentation.waitForIdleSync()
        SparklingContextTransferStation.releaseSparklingContext(context.containerId)
        assertNull(SparklingContextTransferStation.getSparklingContext(context.containerId))
        activeContainerId = null
        currentActivity = null
        println(
            "ORIENTATION_GATE event=pass name=$name requested=$expected " +
                "screen=${screenshot?.width ?: -1}x${screenshot?.height ?: -1}",
        )
        return FullPageResult(
            screenshotWidth = screenshot?.width ?: -1,
            screenshotHeight = screenshot?.height ?: -1,
        )
    }

    private fun setGlobalDefault(policy: SparklingScreenOrientationPolicy?) {
        val current = requireNotNull(HybridCommon.hybridConfig)
        val config =
            SparklingHybridConfig.build(current.baseInfoConfig) {
                setLynxConfig(current.lynxConfig)
                setWebConfig(current.webConfig)
                setBridgeConfig(current.bridgeConfig)
                setLogConfig(current.logConfig)
                current.debugConfig?.let(::setDebugConfig)
                setDefaultScreenOrientationPolicy(policy)
            }
        HybridKit.setHybridConfig(config, application)
    }

    private fun waitUntil(condition: () -> Boolean): Boolean {
        val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MILLIS
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) {
                return true
            }
            SystemClock.sleep(50)
        }
        return condition()
    }

    private fun saveScreenshot(name: String): Bitmap {
        val directory =
            requireNotNull(
                targetContext.getExternalFilesDir("orientation-validation"),
            )
        directory.mkdirs()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val output = directory.resolve("$name.png")
        output.outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        println("ORIENTATION_GATE artifact=${output.absolutePath}")
        return bitmap
    }

    private data class FullPageResult(
        val screenshotWidth: Int,
        val screenshotHeight: Int,
    )

    private companion object {
        const val BUNDLE_NAME = "device-acceptance.lynx.bundle"
        val TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(20)
        const val ORIENTATION_SETTLE_MILLIS = 700L
    }
}
