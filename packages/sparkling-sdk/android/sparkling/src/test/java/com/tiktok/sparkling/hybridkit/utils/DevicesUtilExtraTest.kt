// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.utils

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.Surface
import android.view.View
import android.view.Window
import android.view.WindowManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], packageName = "com.tiktok.sparkling")
class DevicesUtilExtraTest {
    @Test
    fun cachedScreenHeightAndWidthReturnPositiveValues() {
        val ctx = RuntimeEnvironment.getApplication()
        val height = DevicesUtil.getScreenHeight(ctx, true)
        val widthCached = DevicesUtil.getScreenWidth(ctx, true)
        assertTrue(height > 0)
        assertTrue(widthCached > 0)
        // Second call hits cache branch
        val again = DevicesUtil.getScreenHeight(ctx, true)
        assertEquals(height, again)
    }

    @Test
    fun getScreenRotationDelegatesToWindowManager() {
        val ctx = RuntimeEnvironment.getApplication()
        // Should return one of the four standard rotations.
        val rotation = DevicesUtil.getScreenRotation(ctx)
        assertTrue(
            rotation == Surface.ROTATION_0 ||
                rotation == Surface.ROTATION_90 ||
                rotation == Surface.ROTATION_180 ||
                rotation == Surface.ROTATION_270,
        )
    }

    @Test
    fun isScreenPortraitReturnsTrueForRotation0() {
        val ctx = RuntimeEnvironment.getApplication()
        // Default is rotation 0 in Robolectric.
        assertTrue(DevicesUtil.isScreenPortrait(ctx))
    }

    @Test
    fun isHuaweiAndSystemMetadataAreReadable() {
        // Ensure these branches are touched. Values vary per JVM.
        DevicesUtil.isHuawei
        assertTrue(DevicesUtil.system.isNotEmpty())
    }

    @Test
    fun dpAndDpFloatExtensionsReturnNonNegativeValues() {
        with(DevicesUtil) {
            val dpF = 16.dpFloat
            val dpI = 16.dp
            assertTrue(dpF >= 0f)
            assertTrue(dpI >= 0)
        }
    }

    @Test
    fun safeAreaHeightFallsBackOnException() {
        val activity = mockk<Activity>()
        val window = mockk<Window>()
        val decor = mockk<View>()
        val resources = mockk<android.content.res.Resources>()
        val metrics =
            DisplayMetrics().apply {
                density = 2.0f
                heightPixels = 1500
            }
        every { activity.window } returns window
        every { window.decorView } returns decor
        every { decor.getWindowVisibleDisplayFrame(any()) } throws RuntimeException("fail")
        every { activity.resources } returns resources
        every { resources.displayMetrics } returns metrics

        @Suppress("DEPRECATION")
        val result = DevicesUtil.safeAreaHeight(48, activity)
        assertEquals(1500, result)
    }

    @Test
    fun safeAreaHeightUsesContentRectHeightWhenTopExceedsStatusBar() {
        val activity = mockk<Activity>()
        val window = mockk<Window>()
        val decor = mockk<View>()
        val resources = mockk<android.content.res.Resources>()
        val metrics = DisplayMetrics().apply { density = 2.0f }
        every { activity.window } returns window
        every { window.decorView } returns decor
        every { activity.resources } returns resources
        every { resources.displayMetrics } returns metrics
        every { decor.getWindowVisibleDisplayFrame(any()) } answers {
            firstArg<Rect>().set(0, 200, 1080, 2000) // top=200px → 100dp ≥ 25
        }

        @Suppress("DEPRECATION")
        val result = DevicesUtil.safeAreaHeight(25, activity)
        // height = 1800px, /density=2 → 900
        assertEquals(900, result)
    }

    @Test
    fun safeAreaHeightSubtractsStatusBarWhenTopIsSmall() {
        val activity = mockk<Activity>()
        val window = mockk<Window>()
        val decor = mockk<View>()
        val resources = mockk<android.content.res.Resources>()
        val metrics = DisplayMetrics().apply { density = 2.0f }
        every { activity.window } returns window
        every { window.decorView } returns decor
        every { activity.resources } returns resources
        every { resources.displayMetrics } returns metrics
        every { decor.getWindowVisibleDisplayFrame(any()) } answers {
            firstArg<Rect>().set(0, 0, 1080, 1600) // top=0 → 0dp < 25
        }

        @Suppress("DEPRECATION")
        val result = DevicesUtil.safeAreaHeight(25, activity)
        // height = 1600px /2 = 800 dp, minus 25
        assertEquals(775, result)
    }

    @Test
    fun getScreenSizeReturnsArrayForActivityContext() {
        val activity = RuntimeEnvironment.getApplication()
        val size = DevicesUtil.getScreenSize(activity)
        assertEquals(2, size.size)
    }

    @Test
    fun getScreenSizeReturnsSentinelOnException() {
        val ctx = mockk<Context>()
        every { ctx.getSystemService(Context.WINDOW_SERVICE) } throws RuntimeException("boom")
        val size = DevicesUtil.getScreenSize(ctx)
        assertEquals(-1, size[0])
        assertEquals(-1, size[1])
    }

    @Test
    fun isLowPowerModeReturnsFalseWhenServiceMissing() {
        val ctx = mockk<Context>()
        every { ctx.getSystemService(Context.POWER_SERVICE) } returns null
        assertFalse(DevicesUtil.isLowPowerMode(ctx))
    }
}
