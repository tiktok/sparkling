// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import com.tiktok.sparkling.hybridkit.base.IKitView
import com.tiktok.sparkling.hybridkit.base.Theme
import com.tiktok.sparkling.hybridkit.event.AbsSendEventListener
import com.tiktok.sparkling.hybridkit.event.SendEventListener
import com.tiktok.sparkling.hybridkit.scheme.HybridSchemeParam
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [HybridContext] focused on its pure logic — fluent setters,
 * template res-data initialization, theme resolution, and the sendEvent
 * dispatch table.
 */
@RunWith(RobolectricTestRunner::class)
class HybridContextLogicTest {
    @Before
    fun setUp() {
        clearAllMocks()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun containerIdIsGeneratedAndUnique() {
        val a = HybridContext()
        val b = HybridContext()
        assertNotNull(a.containerId)
        assertNotNull(b.containerId)
        // unique by the time-based + UUID generator
        org.junit.Assert.assertTrue(
            "container ids should differ",
            a.containerId != b.containerId,
        )
    }

    @Test
    fun fluentSettersReturnSameInstance() {
        val ctx = HybridContext()
        val bundle = Bundle()
        val extra = mapOf("k" to "v")
        val cfg = mutableMapOf("a" to "b")
        val listener = mockk<SendEventListener>(relaxed = true)
        val multi = mockk<AbsSendEventListener>(relaxed = true)

        assertSame(ctx, ctx.withBundle(bundle))
        assertSame(ctx, ctx.withExtra(extra))
        assertSame(ctx, ctx.withLynxViewConfig(cfg))
        assertSame(ctx, ctx.withSendEventListener(listener))
        assertSame(ctx, ctx.withMultiSendEventListener(multi))

        assertSame(bundle, ctx.bundle)
        assertEquals(extra, ctx.extra)
        assertEquals(cfg, ctx.getLynxViewConfig())
        assertSame(listener, ctx.sendEventListener)
        assertSame(multi, ctx.absSendEventListener)
    }

    @Test
    fun initDataAccessorsRoundTrip() {
        val ctx = HybridContext()
        assertNull(ctx.initData())
        ctx.withInitData("payload")
        assertEquals("payload", ctx.initData())
    }

    @Test
    fun resolveFullSchemeFallsBackToScheme() {
        val ctx = HybridContext()
        ctx.scheme = "scheme://a"
        assertEquals("scheme://a", ctx.resolveFullScheme())

        ctx.fullScheme = "scheme://b/full"
        assertEquals("scheme://b/full", ctx.resolveFullScheme())
    }

    @Test
    fun tryResetTemplateResDataAppendsWhenEmpty() {
        val ctx = HybridContext()
        ctx.tryResetTemplateResData(123L)
        assertEquals(123L, ctx.templateResData.getLong("container_init_cost"))
    }

    @Test
    fun tryResetTemplateResDataReplacesWhenZeroSentinel() {
        val ctx = HybridContext()
        ctx.templateResData = JSONObject().put("container_init_cost", 0)
        ctx.tryResetTemplateResData(99L)
        assertEquals(99L, ctx.templateResData.getLong("container_init_cost"))
    }

    @Test
    fun tryResetTemplateResDataReplacesWhenLargerThanOne() {
        val ctx = HybridContext()
        ctx.templateResData = JSONObject().put("foo", 1).put("bar", 2)
        ctx.tryResetTemplateResData(50L)
        assertEquals(50L, ctx.templateResData.getLong("container_init_cost"))
        // foo / bar should have been wiped because the new object is constructed fresh
        assertEquals(false, ctx.templateResData.has("foo"))
    }

    @Test
    fun tryResetTemplateResDataLeavesPopulatedNonZeroAlone() {
        val ctx = HybridContext()
        ctx.templateResData = JSONObject().put("container_init_cost", 42)
        ctx.tryResetTemplateResData(99L)
        // single-key with non-zero existing cost: do nothing
        assertEquals(42L, ctx.templateResData.getLong("container_init_cost"))
    }

    @Test
    fun getThemeForcedStyles() {
        val ctx = HybridContext()
        ctx.hybridSchemeParam = HybridSchemeParam().apply { forceThemeStyle = "Light" }
        assertEquals(Theme.LIGHT, ctx.getTheme(null))

        ctx.hybridSchemeParam = HybridSchemeParam().apply { forceThemeStyle = "DARK" }
        assertEquals(Theme.DARK, ctx.getTheme(null))
    }

    @Test
    fun getThemeFollowsConfigurationWhenNoForcedStyle() {
        val ctx = HybridContext()
        val context = mockk<Context>()
        val resources = mockk<Resources>()
        val cfg =
            Configuration().apply {
                uiMode = Configuration.UI_MODE_NIGHT_YES
            }
        every { context.resources } returns resources
        every { resources.configuration } returns cfg
        assertEquals(Theme.DARK, ctx.getTheme(context))

        val cfgLight =
            Configuration().apply {
                uiMode = Configuration.UI_MODE_NIGHT_NO
            }
        every { resources.configuration } returns cfgLight
        assertEquals(Theme.LIGHT, ctx.getTheme(context))

        // null context falls back to LIGHT
        assertEquals(Theme.LIGHT, ctx.getTheme(null))
    }

    @Test
    fun sendEventDispatchesByParamType() {
        val ctx = HybridContext()
        val view = mockk<IKitView>(relaxed = true)
        mockkObject(KitViewManager)
        every { KitViewManager.getKitView(any()) } returns view

        ctx.sendEvent("e-null", null)
        ctx.sendEvent("e-list", listOf<Any>("a", "b"))
        ctx.sendEvent("e-map", mapOf("k" to "v"))
        ctx.sendEvent("e-json", JSONObject().put("k", "v"))

        verify { view.sendEvent("e-null", null) }
        verify { view.sendEvent("e-list", any<List<Any>>()) }
        verify { view.sendEventByMap("e-map", any()) }
        verify { view.sendEventByJSON(eq("e-json"), any()) }
    }

    @Test
    fun sendEventNoOpWhenNoKitView() {
        val ctx = HybridContext()
        mockkObject(KitViewManager)
        every { KitViewManager.getKitView(any()) } returns null
        // should not throw
        ctx.sendEvent("event", null)
    }
}
