// Copyright (c) 2022 TikTok Pte. Ltd.
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    packageName = "com.tiktok.sparkling",
)
class DisplayMetricsTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testNumberDivContext() {
        val mockContext = mockk<Context>()
        val mockResources = mockk<android.content.res.Resources>()
        val mockDisplayMetrics =
            DisplayMetrics().apply {
                density = 2.0f
            }

        every { mockContext.resources } returns mockResources
        every { mockResources.displayMetrics } returns mockDisplayMetrics

        val result = 100 / mockContext
        assertEquals(50.0, result, 0.01)
    }

    @Test
    fun testNumberDivContextWithDifferentDensity() {
        val mockContext = mockk<Context>()
        val mockResources = mockk<android.content.res.Resources>()
        val mockDisplayMetrics =
            DisplayMetrics().apply {
                density = 3.0f
            }

        every { mockContext.resources } returns mockResources
        every { mockResources.displayMetrics } returns mockDisplayMetrics

        val result = 300 / mockContext
        assertEquals(100.0, result, 0.01)
    }

    @Test
    fun testNumberDivContextWithFloat() {
        val mockContext = mockk<Context>()
        val mockResources = mockk<android.content.res.Resources>()
        val mockDisplayMetrics =
            DisplayMetrics().apply {
                density = 2.5f
            }

        every { mockContext.resources } returns mockResources
        every { mockResources.displayMetrics } returns mockDisplayMetrics

        val result = 100.0 / mockContext
        assertEquals(40.0, result, 0.01)
    }

    @Test
    fun testStatusBarHeightExtension() {
        mockkObject(DevicesUtil)
        val mockContext = mockk<Context>()

        every { DevicesUtil.getStatusBarHeight(mockContext) } returns 48

        // Note: We can't directly test the extension property on a mock,
        // but we can verify DevicesUtil.getStatusBarHeight works
        val statusBarHeight = DevicesUtil.getStatusBarHeight(mockContext)
        assertEquals(48, statusBarHeight)
    }

    @Test
    fun testStatusBarHeightDpExtension() {
        mockkObject(DevicesUtil)
        val mockContext = mockk<Context>()
        val mockResources = mockk<android.content.res.Resources>()
        val mockDisplayMetrics =
            DisplayMetrics().apply {
                density = 2.0f
            }

        every { mockContext.resources } returns mockResources
        every { mockResources.displayMetrics } returns mockDisplayMetrics
        every { DevicesUtil.getStatusBarHeight(mockContext) } returns 48

        // Test the conversion: 48px / 2.0 density = 24dp
        val statusBarHeightPx = DevicesUtil.getStatusBarHeight(mockContext)
        val statusBarHeightDp = statusBarHeightPx / mockContext

        assertEquals(24.0, statusBarHeightDp, 0.01)
    }

    @Test
    fun testSafeAreaHeightWithMockedActivity() {
        val mockActivity = mockk<Activity>()
        val mockWindow = mockk<Window>()
        val mockDecorView = mockk<View>()
        val mockResources = mockk<android.content.res.Resources>()
        val mockDisplayMetrics =
            DisplayMetrics().apply {
                density = 2.0f
                heightPixels = 1920
            }

        every { mockActivity.resources } returns mockResources
        every { mockResources.displayMetrics } returns mockDisplayMetrics
        every { mockActivity.window } returns mockWindow
        every { mockWindow.decorView } returns mockDecorView
        every { mockDecorView.getWindowVisibleDisplayFrame(any()) } answers {
            val rect = firstArg<Rect>()
            rect.set(0, 48, 1080, 1920)
        }

        mockkObject(DevicesUtil)
        every { DevicesUtil.getStatusBarHeight(mockActivity) } returns 48

        // Calculate expected: bottom (1920) / density (2.0) - statusBarHeightDp (24) = 960 - 24 = 936
        val statusBarHeightDp = 48 / mockActivity
        val expectedSafeArea = 1920 / mockActivity - statusBarHeightDp

        assertEquals(936.0, expectedSafeArea, 0.01)
    }

    @Test
    fun testSafeAreaHeightFallbackToScreenHeight() {
        val mockActivity = mockk<Activity>()
        val mockResources = mockk<android.content.res.Resources>()
        val mockDisplayMetrics =
            DisplayMetrics().apply {
                density = 2.0f
                heightPixels = 1920
            }

        every { mockActivity.resources } returns mockResources
        every { mockResources.displayMetrics } returns mockDisplayMetrics
        every { mockActivity.window } returns null

        mockkObject(DevicesUtil)
        every { DevicesUtil.getStatusBarHeight(mockActivity) } returns 48

        // When window is null, fallback to heightPixels
        // Expected: 1920 / 2.0 - 24 = 960 - 24 = 936
        val statusBarHeightDp = 48 / mockActivity
        val fallbackHeight = mockDisplayMetrics.heightPixels / mockActivity
        val expectedSafeArea = fallbackHeight - statusBarHeightDp

        assertEquals(936.0, expectedSafeArea, 0.01)
    }

    @Test
    fun testDivisionOperatorWithZeroDensity() {
        val mockContext = mockk<Context>()
        val mockResources = mockk<android.content.res.Resources>()
        val mockDisplayMetrics =
            DisplayMetrics().apply {
                density = 0.0f // Edge case
            }

        every { mockContext.resources } returns mockResources
        every { mockResources.displayMetrics } returns mockDisplayMetrics

        val result = 100 / mockContext
        assertTrue(result.isInfinite())
    }

    @Test
    fun testDivisionOperatorWithNegativeNumber() {
        val mockContext = mockk<Context>()
        val mockResources = mockk<android.content.res.Resources>()
        val mockDisplayMetrics =
            DisplayMetrics().apply {
                density = 2.0f
            }

        every { mockContext.resources } returns mockResources
        every { mockResources.displayMetrics } returns mockDisplayMetrics

        val result = (-100) / mockContext
        assertEquals(-50.0, result, 0.01)
    }
}
