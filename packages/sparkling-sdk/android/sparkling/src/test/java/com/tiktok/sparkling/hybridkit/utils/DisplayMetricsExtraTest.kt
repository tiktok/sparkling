// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.utils

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.View
import android.view.Window
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DisplayMetricsExtraTest {
    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun statusBarHeightExtensionDelegatesToDevicesUtil() {
        mockkObject(DevicesUtil)
        val ctx = mockk<Context>()
        every { DevicesUtil.getStatusBarHeight(ctx) } returns 96
        assertEquals(96, ctx.statusBarHeight)
    }

    @Test
    fun statusBarHeightDpExtensionConvertsByDensity() {
        mockkObject(DevicesUtil)
        val ctx = mockk<Context>()
        val resources = mockk<android.content.res.Resources>()
        val metrics = DisplayMetrics().apply { density = 4.0f }
        every { ctx.resources } returns resources
        every { resources.displayMetrics } returns metrics
        every { DevicesUtil.getStatusBarHeight(ctx) } returns 100
        assertEquals(25.0, ctx.statusBarHeightDp, 0.001)
    }

    @Test
    fun safeAreaHeightFallsBackToHeightPixelsWhenWindowIsNull() {
        mockkObject(DevicesUtil)
        val activity = mockk<Activity>()
        val resources = mockk<android.content.res.Resources>()
        val metrics =
            DisplayMetrics().apply {
                density = 2.0f
                heightPixels = 1600
            }
        every { activity.resources } returns resources
        every { resources.displayMetrics } returns metrics
        every { activity.window } returns null
        every { DevicesUtil.getStatusBarHeight(activity) } returns 48

        // 1600 / 2 - 48/2 = 800 - 24 = 776
        assertEquals(776.0, activity.safeAreaHeight(), 0.001)
    }

    @Test
    fun safeAreaHeightUsesWindowVisibleDisplayFrameWhenAvailable() {
        mockkObject(DevicesUtil)
        val activity = mockk<Activity>()
        val window = mockk<Window>()
        val decor = mockk<View>()
        val resources = mockk<android.content.res.Resources>()
        val metrics =
            DisplayMetrics().apply {
                density = 2.0f
                heightPixels = 2000
            }
        every { activity.resources } returns resources
        every { resources.displayMetrics } returns metrics
        every { activity.window } returns window
        every { window.decorView } returns decor
        every { decor.getWindowVisibleDisplayFrame(any()) } answers {
            firstArg<Rect>().set(0, 0, 1080, 1800)
        }
        every { DevicesUtil.getStatusBarHeight(activity) } returns 48

        // 1800 / 2 - 24 = 876
        assertEquals(876.0, activity.safeAreaHeight(), 0.001)
    }

    @Test
    fun safeAreaHeightFallsBackWhenDecorViewThrowsNpe() {
        mockkObject(DevicesUtil)
        val activity = mockk<Activity>()
        val window = mockk<Window>()
        val decor = mockk<View>()
        val resources = mockk<android.content.res.Resources>()
        val metrics =
            DisplayMetrics().apply {
                density = 2.0f
                heightPixels = 2400
            }
        every { activity.resources } returns resources
        every { resources.displayMetrics } returns metrics
        every { activity.window } returns window
        every { window.decorView } returns decor
        every { decor.getWindowVisibleDisplayFrame(any()) } throws NullPointerException("boom")
        every { DevicesUtil.getStatusBarHeight(activity) } returns 100

        // fallback: 2400 / 2 - 100/2 = 1200 - 50 = 1150
        assertEquals(1150.0, activity.safeAreaHeight(), 0.001)
    }
}
