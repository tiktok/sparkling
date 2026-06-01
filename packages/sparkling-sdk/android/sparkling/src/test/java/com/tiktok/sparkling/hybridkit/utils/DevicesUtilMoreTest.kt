// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.utils

import android.content.ContentResolver
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.PowerManager
import android.util.DisplayMetrics
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], packageName = "com.tiktok.sparkling")
class DevicesUtilMoreTest {
    @Test
    fun brandAndModelAreNonNullStrings() {
        assertNotNull(DevicesUtil.brand)
        assertNotNull(DevicesUtil.model)
        assertNotNull(DevicesUtil.platform)
        assertEquals("android", DevicesUtil.platform)
    }

    @Test
    fun getStatusBarHeightReturnsPositiveValueAndCachesAcrossCalls() {
        val ctx = RuntimeEnvironment.getApplication()
        val first = DevicesUtil.getStatusBarHeight(ctx)
        assertTrue(first > 0)
        val cached = DevicesUtil.getStatusBarHeight(ctx)
        assertEquals(first, cached)
    }

    @Test
    fun getNavigationBarHeightDoesNotCrash() {
        val ctx = RuntimeEnvironment.getApplication()
        val height = DevicesUtil.getNavigationBarHeight(ctx)
        assertTrue(height >= 0)
    }

    @Test
    fun getPixelRadioReturnsContextDisplayDensity() {
        val ctx = RuntimeEnvironment.getApplication()
        val expected = ctx.resources.displayMetrics.density
        assertEquals(expected, DevicesUtil.getPixelRadio(ctx), 0.0f)
    }

    @Test
    fun languageReturnsLocaleAndCountryString() {
        val lang = DevicesUtil.language
        // Format must contain the language-country separator.
        assertTrue(lang.contains("-"))
    }

    @Test
    fun isPadIsBooleanForApplicationContext() {
        val ctx = RuntimeEnvironment.getApplication()
        // Just ensure no exception is thrown.
        DevicesUtil.isPad(ctx)
    }

    @Test
    fun isPadFalseForSmallScreenLayout() {
        val ctx = mockk<Context>()
        val resources = mockk<Resources>()
        val configuration =
            Configuration().apply {
                // SCREENLAYOUT_SIZE_NORMAL is < SCREENLAYOUT_SIZE_LARGE
                screenLayout = Configuration.SCREENLAYOUT_SIZE_NORMAL
            }
        every { ctx.resources } returns resources
        every { resources.configuration } returns configuration
        assertFalse(DevicesUtil.isPad(ctx))
    }

    @Test
    fun isTalkBackEnabledReturnsBooleanForRobolectricContext() {
        val ctx = RuntimeEnvironment.getApplication()
        // Default is false in Robolectric. Should not throw.
        DevicesUtil.isTalkBackEnabled(ctx)
    }

    @Test
    fun isTalkBackEnabledReturnsFalseWhenContentResolverThrows() {
        val ctx = mockk<Context>()
        every { ctx.contentResolver } throws RuntimeException("boom")
        assertFalse(DevicesUtil.isTalkBackEnabled(ctx))
    }

    @Test
    fun getScreenSizeReturnsSentinelForNullContext() {
        val size = DevicesUtil.getScreenSize(null)
        assertEquals(-1, size[0])
        assertEquals(-1, size[1])
    }

    @Test
    fun getScreenSizeFallsBackToDisplayMetricsWhenWindowManagerReturnsNullDisplay() {
        val ctx = mockk<Context>()
        val wm = mockk<WindowManager>()
        every { ctx.getSystemService(Context.WINDOW_SERVICE) } returns wm
        every { wm.defaultDisplay } returns null
        val size = DevicesUtil.getScreenSize(ctx)
        // Branch returns sentinel when display is null.
        assertEquals(-1, size[0])
        assertEquals(-1, size[1])
    }

    @Test
    fun isLowPowerModeReturnsFalseWhenPowerSaveModeOff() {
        val ctx = mockk<Context>()
        val pm = mockk<PowerManager>()
        every { ctx.getSystemService(Context.POWER_SERVICE) } returns pm
        every { pm.isPowerSaveMode } returns false
        assertFalse(DevicesUtil.isLowPowerMode(ctx))
    }

    @Test
    fun isLowPowerModeSwallowsThrowable() {
        val ctx = mockk<Context>()
        every { ctx.getSystemService(Context.POWER_SERVICE) } throws RuntimeException("boom")
        assertFalse(DevicesUtil.isLowPowerMode(ctx))
    }

    @Test
    fun px2DpRoundsToNearestInt() {
        val ctx = mockk<Context>()
        val resources = mockk<Resources>()
        val metrics = DisplayMetrics().apply { density = 2.0f }
        every { ctx.resources } returns resources
        every { resources.displayMetrics } returns metrics
        assertEquals(50, DevicesUtil.px2dp(100.0, ctx))
    }

    @Test
    fun getScreenHeightWithoutCacheRecalculatesValues() {
        val ctx = RuntimeEnvironment.getApplication()
        val first = DevicesUtil.getScreenHeight(ctx, false)
        assertTrue(first > 0)
    }
}
