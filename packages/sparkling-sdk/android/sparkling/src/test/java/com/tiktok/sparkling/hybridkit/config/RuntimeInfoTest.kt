// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.config

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    packageName = "com.tiktok.sparkling",
)
class RuntimeInfoTest {
    private lateinit var runtimeInfo: RuntimeInfo

    @Before
    fun setUp() {
        runtimeInfo = RuntimeInfo()
    }

    // Individual constant tests
    @Test
    fun testContainerIdConstant() {
        assertEquals("containerID", RuntimeInfo.CONTAINER_ID)
    }

    @Test
    fun testQueryItemsConstant() {
        assertEquals("queryItems", RuntimeInfo.QUERY_ITEMS)
    }

    @Test
    fun testScreenWidthConstant() {
        assertEquals("screenWidth", RuntimeInfo.SCREEN_WIDTH)
    }

    @Test
    fun testScreenHeightConstant() {
        assertEquals("screenHeight", RuntimeInfo.SCREEN_HEIGHT)
    }

    @Test
    fun testOsConstant() {
        assertEquals("os", RuntimeInfo.OS)
    }

    @Test
    fun testOsVersionConstant() {
        assertEquals("osVersion", RuntimeInfo.OS_VERSION)
    }

    @Test
    fun testLanguageConstant() {
        assertEquals("language", RuntimeInfo.LANGUAGE)
    }

    @Test
    fun testStatusBarHeightConstant() {
        assertEquals("statusBarHeight", RuntimeInfo.STATUS_BAR_HEIGHT)
    }

    @Test
    fun testSafeAreaHeightConstant() {
        assertEquals("safeAreaHeight", RuntimeInfo.SAFEAREA_HEIGHT)
    }

    @Test
    fun testIsPadConstant() {
        assertEquals("isPad", RuntimeInfo.IS_PAD)
    }

    @Test
    fun testNavigationBarHeightConstant() {
        assertEquals("navigationBarHeight", RuntimeInfo.NAVIGATION_BAR_HEIGHT)
    }

    @Test
    fun testPixelRatioConstant() {
        assertEquals("pixelRatio", RuntimeInfo.PIXEL_RATIO)
    }

    @Test
    fun testAppLanguageConstant() {
        assertEquals("appLanguage", RuntimeInfo.APP_LANGUAGE)
    }

    @Test
    fun testAppLocaleConstant() {
        assertEquals("appLocale", RuntimeInfo.APP_LOCALE)
    }

    @Test
    fun testLynxSdkVersionConstant() {
        assertEquals("lynxSdkVersion", RuntimeInfo.LYNX_SDK_VERSION)
    }

    @Test
    fun testTemplateResDataConstant() {
        assertEquals("templateResData", RuntimeInfo.TEMPLATE_RES_DATA)
    }

    @Test
    fun testIsLowPowerModeConstant() {
        assertEquals("isLowPowerMode", RuntimeInfo.IS_LOW_POWER_MODE)
    }

    @Test
    fun testIsAppBackgroundConstant() {
        assertEquals("isAppBackground", RuntimeInfo.IS_APP_BACKGROUND)
    }

    @Test
    fun testA11yModeConstant() {
        assertEquals("accessibleMode", RuntimeInfo.A11Y_MODE)
    }

    @Test
    fun testDeviceModelConstant() {
        assertEquals("deviceModel", RuntimeInfo.DEVICE_MODEL)
    }

    @Test
    fun testEnvironmentConstant() {
        assertEquals("env", RuntimeInfo.ENVIRONMENT)
    }

    @Test
    fun testScreenOrientationConstant() {
        assertEquals("screenOrientation", RuntimeInfo.SCREEN_ORIENTATION)
    }

    @Test
    fun testOrientationConstant() {
        assertEquals("orientation", RuntimeInfo.ORIENTATION)
    }

    @Test
    fun testHasInitDataResConstant() {
        assertEquals("hasInitDataRes", RuntimeInfo.HAS_INIT_DATA_RES)
    }

    // Functional tests
    @Test
    fun testRuntimeInfoInheritsFromConcurrentHashMap() {
        assertTrue(runtimeInfo is java.util.concurrent.ConcurrentHashMap<String, Any>)
    }

    @Test
    fun testRuntimeInfoIsConcurrentHashMap() {
        runtimeInfo["key1"] = "value1"
        runtimeInfo["key2"] = 123

        assertEquals("value1", runtimeInfo["key1"])
        assertEquals(123, runtimeInfo["key2"])
        assertEquals(2, runtimeInfo.size)
    }

    @Test
    fun testPutAndGetValues() {
        runtimeInfo[RuntimeInfo.CONTAINER_ID] = "test-container-id"
        runtimeInfo[RuntimeInfo.SCREEN_WIDTH] = 1080
        runtimeInfo[RuntimeInfo.SCREEN_HEIGHT] = 1920

        assertEquals("test-container-id", runtimeInfo[RuntimeInfo.CONTAINER_ID])
        assertEquals(1080, runtimeInfo[RuntimeInfo.SCREEN_WIDTH])
        assertEquals(1920, runtimeInfo[RuntimeInfo.SCREEN_HEIGHT])
    }

    @Test
    fun testConcurrentAccess() {
        runtimeInfo[RuntimeInfo.OS] = "Android"
        runtimeInfo[RuntimeInfo.OS_VERSION] = "13"
        runtimeInfo[RuntimeInfo.LANGUAGE] = "en"

        assertEquals(3, runtimeInfo.size)
        assertTrue(runtimeInfo.containsKey(RuntimeInfo.OS))
        assertTrue(runtimeInfo.containsKey(RuntimeInfo.OS_VERSION))
        assertTrue(runtimeInfo.containsKey(RuntimeInfo.LANGUAGE))
    }

    @Test
    fun testClearAndEmpty() {
        runtimeInfo[RuntimeInfo.CONTAINER_ID] = "test"
        runtimeInfo[RuntimeInfo.SCREEN_WIDTH] = 1080

        assertFalse(runtimeInfo.isEmpty())
        assertEquals(2, runtimeInfo.size)

        runtimeInfo.clear()

        assertTrue(runtimeInfo.isEmpty())
        assertEquals(0, runtimeInfo.size)
    }

    @Test
    fun testReplaceValues() {
        runtimeInfo[RuntimeInfo.DEVICE_MODEL] = "Pixel 6"
        assertEquals("Pixel 6", runtimeInfo[RuntimeInfo.DEVICE_MODEL])

        runtimeInfo[RuntimeInfo.DEVICE_MODEL] = "Galaxy S22"
        assertEquals("Galaxy S22", runtimeInfo[RuntimeInfo.DEVICE_MODEL])
    }

    @Test
    fun testNullValues() {
        runtimeInfo.remove(RuntimeInfo.ENVIRONMENT)

        assertFalse(runtimeInfo.containsKey(RuntimeInfo.ENVIRONMENT))
        assertNull(runtimeInfo[RuntimeInfo.ENVIRONMENT])
    }
}
