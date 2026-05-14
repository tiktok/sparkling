// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.utils

import android.app.Application
import android.content.Context
import com.tiktok.sparkling.hybridkit.HybridEnvironment
import com.tiktok.sparkling.hybridkit.config.BaseInfoConfig
import com.tiktok.sparkling.hybridkit.config.RuntimeInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
class GlobalPropsUtilsTest {
    private lateinit var context: Context
    private var originalContext: Application? = null
    private var originalBaseInfoConfig: BaseInfoConfig? = null

    @Before
    fun setUp() {
        val application = RuntimeEnvironment.getApplication()
        context = application
        val environment = HybridEnvironment.instance
        originalContext = runCatching { environment.context }.getOrNull()
        originalBaseInfoConfig = environment.baseInfoConfig
        environment.context = application
        environment.baseInfoConfig = null
    }

    @After
    fun tearDown() {
        val environment = HybridEnvironment.instance
        originalContext?.let { environment.context = it }
        environment.baseInfoConfig = originalBaseInfoConfig
    }

    @Test
    fun testBuiltInStableFieldsContainsScreenWidth() {
        assertTrue(GlobalPropsUtils.builtInStableFields.containsKey(RuntimeInfo.SCREEN_WIDTH))
    }

    @Test
    fun testBuiltInStableFieldsContainsScreenHeight() {
        assertTrue(GlobalPropsUtils.builtInStableFields.containsKey(RuntimeInfo.SCREEN_HEIGHT))
    }

    @Test
    fun testBuiltInStableFieldsContainsStatusBarHeight() {
        assertTrue(GlobalPropsUtils.builtInStableFields.containsKey(RuntimeInfo.STATUS_BAR_HEIGHT))
    }

    @Test
    fun testBuiltInStableFieldsContainsSafeAreaHeight() {
        assertTrue(GlobalPropsUtils.builtInStableFields.containsKey(RuntimeInfo.SAFEAREA_HEIGHT))
    }

    @Test
    fun testBuiltInStableFieldsContainsIsPad() {
        assertTrue(GlobalPropsUtils.builtInStableFields.containsKey(RuntimeInfo.IS_PAD))
    }

    @Test
    fun testBuiltInStableFieldsContainsNavigationBarHeight() {
        assertTrue(GlobalPropsUtils.builtInStableFields.containsKey(RuntimeInfo.NAVIGATION_BAR_HEIGHT))
    }

    @Test
    fun testBuiltInStableFieldsContainsPixelRatio() {
        assertTrue(GlobalPropsUtils.builtInStableFields.containsKey(RuntimeInfo.PIXEL_RATIO))
    }

    @Test
    fun testBuiltInStableFieldsContainsOsInfo() {
        assertTrue(GlobalPropsUtils.builtInStableFields.containsKey(RuntimeInfo.OS))
        assertTrue(GlobalPropsUtils.builtInStableFields.containsKey(RuntimeInfo.OS_VERSION))
    }

    @Test
    fun testBuiltInStableFieldsContainsDeviceModel() {
        assertTrue(GlobalPropsUtils.builtInStableFields.containsKey(RuntimeInfo.DEVICE_MODEL))
    }

    @Test
    fun testBuiltInStableFieldsContainsLanguage() {
        assertTrue(GlobalPropsUtils.builtInStableFields.containsKey(RuntimeInfo.LANGUAGE))
    }

    @Test
    fun testBuiltInStableFieldsContainsOrientation() {
        assertTrue(GlobalPropsUtils.builtInStableFields.containsKey(RuntimeInfo.SCREEN_ORIENTATION))
        assertTrue(GlobalPropsUtils.builtInStableFields.containsKey(RuntimeInfo.ORIENTATION))
    }

    @Test
    fun testBuiltInStableFieldsContainsAccessibilityMode() {
        assertTrue(GlobalPropsUtils.builtInStableFields.containsKey(RuntimeInfo.A11Y_MODE))
    }

    @Test
    fun testBuiltInStableFieldsContainsLowPowerMode() {
        assertTrue(GlobalPropsUtils.builtInStableFields.containsKey(RuntimeInfo.IS_LOW_POWER_MODE))
    }

    @Test
    fun testIsPadFieldReturnsInteger() {
        val isPadSupplier = GlobalPropsUtils.builtInStableFields[RuntimeInfo.IS_PAD]
        assertNotNull(isPadSupplier)
        val result = isPadSupplier!!.invoke()
        assertTrue(result is Int)
        assertTrue(result == 0 || result == 1)
    }

    @Test
    fun testNavigationBarHeightFieldReturnsInteger() {
        val navBarHeightSupplier = GlobalPropsUtils.builtInStableFields[RuntimeInfo.NAVIGATION_BAR_HEIGHT]
        assertNotNull(navBarHeightSupplier)
        val result = navBarHeightSupplier!!.invoke()
        assertTrue(result is Int)
        assertTrue((result as Int) >= 0)
    }

    @Test
    fun testPixelRatioFieldReturnsFloat() {
        val pixelRatioSupplier = GlobalPropsUtils.builtInStableFields[RuntimeInfo.PIXEL_RATIO]
        assertNotNull(pixelRatioSupplier)
        val result = pixelRatioSupplier!!.invoke()
        assertTrue(result is Float)
        assertTrue((result as Float) > 0)
    }

    @Test
    fun testSetStableProps() {
        val props = mapOf("customKey" to "customValue")
        GlobalPropsUtils.instance.setStableProps(props)

        val stableProps = GlobalPropsUtils.instance.getStableGlobalProps()
        assertEquals("customValue", stableProps["customKey"])
    }

    @Test
    fun testSetUnstableProps() {
        val containerId = "test_container"
        val props = mapOf("unstableKey" to "unstableValue")

        GlobalPropsUtils.instance.setUnstableProps(containerId, props)
        val globalProps = GlobalPropsUtils.instance.getGlobalProps(containerId)

        assertEquals("unstableValue", globalProps["unstableKey"])
    }

    @Test
    fun testFlushGlobalProps() {
        val containerId = "test_container_flush"
        val props = mapOf("key" to "value")

        GlobalPropsUtils.instance.setUnstableProps(containerId, props)
        GlobalPropsUtils.instance.flushGlobalProps(containerId)

        val globalProps = GlobalPropsUtils.instance.getGlobalProps(containerId)
        // After flush, container-specific props should be cleared
        assertTrue(!globalProps.containsKey("key") || globalProps["key"] == null)
    }
}
