// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.view.WindowManager
import android.view.Display
import android.view.accessibility.AccessibilityManager
import com.tiktok.sparkling.hybridkit.HybridContext
import com.tiktok.sparkling.hybridkit.HybridKit
import com.tiktok.sparkling.hybridkit.HybridCommon
import com.tiktok.sparkling.hybridkit.HybridEnvironment
import com.tiktok.sparkling.hybridkit.KitViewManager
import com.tiktok.sparkling.hybridkit.base.HybridKitType
import com.tiktok.sparkling.hybridkit.base.HybridLoadSession
import com.tiktok.sparkling.hybridkit.base.Theme
import com.tiktok.sparkling.hybridkit.config.SparklingHybridConfig
import com.tiktok.sparkling.hybridkit.scheme.HybridSchemeParam
import io.mockk.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HybridKitTest {
    private lateinit var mockApplication: Application
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        clearAllMocks()

        mockApplication = mockk(relaxed = true)
        mockContext = mockk(relaxed = true)

        // Mock WindowManager for DevicesUtil - this is needed because GlobalPropsUtils
        // calls DevicesUtil.isScreenPortrait(HybridEnvironment.instance.context)
        val mockWindowManager = mockk<WindowManager>(relaxed = true)
        val mockDisplay = mockk<Display>(relaxed = true)
        every { mockDisplay.rotation } returns 0
        every { mockWindowManager.defaultDisplay } returns mockDisplay
        every { mockContext.getSystemService(Context.WINDOW_SERVICE) } returns mockWindowManager
        every { mockApplication.getSystemService(Context.WINDOW_SERVICE) } returns mockWindowManager

        // Mock AccessibilityManager for LynxView accessibility system
        val mockAccessibilityManager = mockk<AccessibilityManager>(relaxed = true)
        every { mockContext.getSystemService(Context.ACCESSIBILITY_SERVICE) } returns mockAccessibilityManager
        every { mockApplication.getSystemService(Context.ACCESSIBILITY_SERVICE) } returns mockAccessibilityManager

        // Initialize HybridEnvironment to prevent UninitializedPropertyAccessException
        HybridEnvironment.instance.context = mockApplication
    }

    @Test
    fun testInit() {
        HybridKit.init(mockApplication)
        assertEquals(mockApplication, HybridKit.application)
    }

    @Test
    fun testSetHybridConfig() {
        val hybridConfig = mockk<SparklingHybridConfig>(relaxed = true)
        HybridKit.setHybridConfig(hybridConfig, mockApplication)
        verify { HybridCommon.setHybridConfig(hybridConfig, mockApplication) }
    }

    @Test
    fun testCreateKitViewWithLynxType() {
        val scheme =
            mockk<HybridSchemeParam> {
                every { engineType } returns HybridKitType.LYNX
            }

        // Create a mock HybridLoadSession
        val mockLoadSession =
            mockk<HybridLoadSession> {
                every { openTime } returns System.currentTimeMillis()
            }

        val param =
            mockk<HybridContext>(relaxed = true) {
                every { containerId } returns "test-container-id"
                every { getDependency(HybridLoadSession::class.java) } returns mockLoadSession
            }

        // Ensure HybridEnvironment is properly initialized
        HybridEnvironment.instance.context = mockApplication

        val result = HybridKit.createKitView(scheme, param, mockContext)

        assertNotNull(scheme.engineType)
        assertEquals(HybridKitType.LYNX, scheme.engineType)
        // The result should not be null for LYNX type
        assertNotNull(result)
    }

    @Test
    fun testCreateKitViewWithUnsupportedType() {
        val scheme =
            mockk<HybridSchemeParam> {
                every { engineType } returns mockk()
            }
        val param = mockk<HybridContext>(relaxed = true)

        val result = HybridKit.createKitView(scheme, param, mockContext)

        assertNull(result)
    }
}

@RunWith(RobolectricTestRunner::class)
class HybridContextTest {
    private lateinit var hybridContext: HybridContext
    private lateinit var mockContext: Context
    private lateinit var mockResources: Resources
    private lateinit var mockConfiguration: Configuration

    @Before
    fun setUp() {
        hybridContext = HybridContext()
        mockContext = mockk(relaxed = true)
        mockResources = mockk(relaxed = true)
        mockConfiguration = mockk(relaxed = true)

        every { mockContext.resources } returns mockResources
        every { mockResources.configuration } returns mockConfiguration
    }

    @Test
    fun testContainerIdGeneration() {
        val hybridContext1 = HybridContext()
        val hybridContext2 = HybridContext()

        assertNotEquals(hybridContext1.containerId, hybridContext2.containerId)
        assertTrue(hybridContext1.containerId.contains("-"))
    }

    @Test
    fun testWithInitData() {
        val testData = "test_init_data"
        hybridContext.withInitData(testData)

        assertEquals(testData, hybridContext.initData())
    }

    @Test
    fun testWithLynxViewConfig() {
        val config = mutableMapOf("key1" to "value1", "key2" to "value2")
        val result = hybridContext.withLynxViewConfig(config)

        assertEquals(hybridContext, result)
        assertEquals(config, hybridContext.getLynxViewConfig())
    }

    @Test
    fun testTryResetTemplateResDataWithEmptyData() {
        val loadTime = 1000L
        hybridContext.tryResetTemplateResData(loadTime)

        assertEquals(loadTime, hybridContext.templateResData.getLong("container_init_cost"))
    }

    @Test
    fun testTryResetTemplateResDataWithExistingData() {
        hybridContext.templateResData.put("existing_key", "existing_value")
        val loadTime = 2000L

        hybridContext.tryResetTemplateResData(loadTime)

        assertEquals(loadTime, hybridContext.templateResData.getLong("container_init_cost"))
        assertFalse(hybridContext.templateResData.has("existing_key"))
    }

    @Test
    fun testGetThemeWithForceLightTheme() {
        val schemeParam =
            mockk<HybridSchemeParam> {
                every { forceThemeStyle } returns "light"
            }
        hybridContext.hybridSchemeParam = schemeParam

        val theme = hybridContext.getTheme(mockContext)

        assertEquals(Theme.LIGHT, theme)
    }

    @Test
    fun testGetThemeWithForceDarkTheme() {
        val schemeParam =
            mockk<HybridSchemeParam> {
                every { forceThemeStyle } returns "dark"
            }
        hybridContext.hybridSchemeParam = schemeParam

        val theme = hybridContext.getTheme(mockContext)

        assertEquals(Theme.DARK, theme)
    }

    @Test
    fun testGlobalPropsManipulation() {
        hybridContext.globalProps["test_key"] = "test_value"

        assertEquals("test_value", hybridContext.globalProps["test_key"])
        assertTrue(hybridContext.globalProps.containsKey("test_key"))
    }

    @Test
    fun testSendEventWithNullParams() {
        val mockKitView = mockk<com.tiktok.sparkling.hybridkit.base.IKitView>(relaxed = true)
        mockkObject(KitViewManager)
        every { KitViewManager.getKitView(any()) } returns mockKitView

        hybridContext.sendEvent("test_event", null)

        verify { mockKitView.sendEvent("test_event", null) }
    }

    @Test
    fun testSendEventWithListParams() {
        val mockKitView = mockk<com.tiktok.sparkling.hybridkit.base.IKitView>(relaxed = true)
        mockkObject(KitViewManager)
        every { KitViewManager.getKitView(any()) } returns mockKitView

        val params = listOf("param1", "param2")
        hybridContext.sendEvent("test_event", params)

        verify { mockKitView.sendEvent("test_event", params) }
    }

    @Test
    fun testSendEventWithJSONObjectParams() {
        val mockKitView = mockk<com.tiktok.sparkling.hybridkit.base.IKitView>(relaxed = true)
        mockkObject(KitViewManager)
        every { KitViewManager.getKitView(any()) } returns mockKitView

        val params = JSONObject().apply { put("key", "value") }
        hybridContext.sendEvent("test_event", params)

        verify { mockKitView.sendEventByJSON("test_event", params) }
    }

    @Test
    fun testSendEventWithMapParams() {
        val mockKitView = mockk<com.tiktok.sparkling.hybridkit.base.IKitView>(relaxed = true)
        mockkObject(KitViewManager)
        every { KitViewManager.getKitView(any()) } returns mockKitView

        val params = mapOf("key" to "value")
        hybridContext.sendEvent("test_event", params)

        verify { mockKitView.sendEventByMap("test_event", params) }
    }
}
