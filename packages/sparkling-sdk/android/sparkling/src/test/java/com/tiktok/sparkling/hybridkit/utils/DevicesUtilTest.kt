// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.Window
import android.view.WindowManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    packageName = "com.tiktok.sparkling",
)
class DevicesUtilTest {
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
    fun testGetPixelRadio() {
        val pixelRatio = DevicesUtil.getPixelRadio(context)
        assertTrue(pixelRatio > 0)
    }

    @Test
    fun testGetStatusBarHeight() {
        val statusBarHeight = DevicesUtil.getStatusBarHeight(context)
        assertTrue(statusBarHeight >= 0)
    }

    @Test
    fun testGetNavigationBarHeight() {
        val navigationBarHeight = DevicesUtil.getNavigationBarHeight(context)
        assertTrue(navigationBarHeight >= 0)
    }

    @Test
    fun testGetNavigationBarHeightWithMissingResource() {
        val mockContext = mockk<Context>()
        val mockResources = mockk<Resources>()

        every { mockContext.resources } returns mockResources
        every { mockResources.getIdentifier("navigation_bar_height", "dimen", "android") } returns 0

        val result = DevicesUtil.getNavigationBarHeight(mockContext)
        assertEquals(0, result)
    }

    @Test
    fun testGetNavigationBarHeightWithValidResource() {
        val mockContext = mockk<Context>()
        val mockResources = mockk<Resources>()
        val expectedHeight = 48

        every { mockContext.resources } returns mockResources
        every { mockResources.getIdentifier("navigation_bar_height", "dimen", "android") } returns 123
        every { mockResources.getDimensionPixelSize(123) } returns expectedHeight

        val result = DevicesUtil.getNavigationBarHeight(mockContext)
        assertEquals(expectedHeight, result)
    }

    @Test
    fun testIsPadReturnsFalseForPhone() {
        val mockContext = mockk<Context>()
        val mockResources = mockk<Resources>()
        val configuration =
            Configuration().apply {
                screenLayout = Configuration.SCREENLAYOUT_SIZE_NORMAL
            }

        every { mockContext.resources } returns mockResources
        every { mockResources.configuration } returns configuration

        val result = DevicesUtil.isPad(mockContext)
        assertFalse(result)
    }

    @Test
    fun testIsPadReturnsTrueForTablet() {
        val mockContext = mockk<Context>()
        val mockResources = mockk<Resources>()
        val configuration =
            Configuration().apply {
                screenLayout = Configuration.SCREENLAYOUT_SIZE_LARGE
            }

        every { mockContext.resources } returns mockResources
        every { mockResources.configuration } returns configuration

        val result = DevicesUtil.isPad(mockContext)
        assertTrue(result)
    }

    @Test
    fun testIsPadReturnsTrueForXLarge() {
        val mockContext = mockk<Context>()
        val mockResources = mockk<Resources>()
        val configuration =
            Configuration().apply {
                screenLayout = Configuration.SCREENLAYOUT_SIZE_XLARGE
            }

        every { mockContext.resources } returns mockResources
        every { mockResources.configuration } returns configuration

        val result = DevicesUtil.isPad(mockContext)
        assertTrue(result)
    }

    @Test
    fun testPx2dp() {
        val mockContext = mockk<Context>()
        val mockResources = mockk<Resources>()
        val mockDisplayMetrics =
            DisplayMetrics().apply {
                density = 2.0f
            }

        every { mockContext.resources } returns mockResources
        every { mockResources.displayMetrics } returns mockDisplayMetrics

        val result = DevicesUtil.px2dp(100.0, mockContext)
        assertEquals(50, result)
    }

    @Test
    fun testGetScreenWidth() {
        val screenWidth = DevicesUtil.getScreenWidth(context)
        assertTrue(screenWidth > 0)
    }

    @Test
    fun testGetScreenHeight() {
        val screenHeight = DevicesUtil.getScreenHeight(context)
        assertTrue(screenHeight > 0)
    }

    @Test
    fun testIsLowPowerModeReturnsFalseByDefault() {
        val isLowPower = DevicesUtil.isLowPowerMode(context)
        assertFalse(isLowPower)
    }

    @Test
    fun testIsTalkBackEnabled() {
        val isTalkBack = DevicesUtil.isTalkBackEnabled(context)
        assertFalse(isTalkBack)
    }

    @Test
    fun testPlatformIsAndroid() {
        assertEquals("android", DevicesUtil.platform)
    }

    @Test
    fun testLanguageIsNotEmpty() {
        val language = DevicesUtil.language
        assertTrue(language.isNotEmpty())
        assertTrue(language.contains("-"))
    }

    @Test
    fun testBrandIsNotEmpty() {
        val brand = DevicesUtil.brand
        assertTrue(brand.isNotEmpty())
    }

    @Test
    fun testModelIsNotEmpty() {
        val model = DevicesUtil.model
        assertTrue(model.isNotEmpty())
    }

    @Test
    fun testGetScreenSizeWithNullContextReturnsInvalidSentinel() {
        val result = DevicesUtil.getScreenSize(null)
        assertEquals(-1, result[0])
        assertEquals(-1, result[1])
    }
}
