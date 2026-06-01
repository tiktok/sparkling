// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.base

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [HybridLoadSession] and its inner [HybridLoadSession.SnapShot] class.
 * The SnapShot computes a number of derived "cost" durations used in performance
 * monitoring; we exercise both the default (all-null) and a fully populated path
 * so every line of the SnapShot init block, toString, and toJSONObject is hit.
 */
@RunWith(RobolectricTestRunner::class)
class HybridLoadSessionTest {
    @Test
    fun toStringWithDefaultsSerializesZeroDurations() {
        val session = HybridLoadSession()
        val text = session.toString()

        assertNotNull(text)
        assertTrue(text.contains("total = 0"))
        assertTrue(text.contains("init2StartRender = 0"))
        assertTrue(text.contains("renderCost = 0"))
        assertTrue(text.contains("lynxCost = 0"))
        assertTrue(text.contains("pluginInfos = "))
    }

    @Test
    fun toJSONObjectWithDefaultsContainsAllKeys() {
        val session = HybridLoadSession()
        val json = session.toJSONObject()

        val expectedKeys =
            listOf(
                "total",
                "init2StartRender",
                "renderCost",
                "prepareTemplateCost",
                "prepareJSBCost",
                "preparePluginExecuteCost",
                "prepareFontCost",
                "sparkContainerCost",
                "lynxCost",
                "lynxViewInitCost",
                "prepareInitDataCost",
                "prepareComponentCost",
                "prepareGlobalPropsCost",
                "prepareComponentEnd2PrepareTemplateStart",
                "prepareExtraInfoCost",
                "customInitCost",
                "pluginInfos",
            )
        for (key in expectedKeys) {
            assertTrue("missing key=$key", json.has(key))
        }
        assertEquals(0L, json.getLong("total"))
        assertEquals(0L, json.getLong("renderCost"))
    }

    @Test
    fun snapShotComputesDerivedDurationsFromPopulatedSession() {
        val session =
            HybridLoadSession().apply {
                sessionId = "session-1"
                clickTime = 1
                openTime = 100
                lynxViewInitStart = 200
                lynxViewInitEnd = 250
                customInitStart = 110
                customInitEnd = 150
                prepareInitDataStart = 260
                prepareInitDataEnd = 280
                prepareFontStart = 300
                prepareFontEnd = 320
                prepareGlobalPropsStart = 330
                prepareGlobalPropsEnd = 340
                pluginExecuteStart = 350
                pluginExecuteEnd = 360
                prepareComponentStart = 370
                prepareComponentEnd = 400
                prepareTemplateStart = 410
                prepareTemplateEnd = 430
                prepareExtraInfoStart = 440
                prepareExtraInfoEnd = 470
                prepareJSBStart = 480
                prepareJSBEnd = 490
                loadEngineAndTemplateDataStart = 500
                startLoadTime = 505
                loadEngineStart = 520
                loadEngineEnd = 600
                extraRecord["x"] = 1L
                pluginInfos["a"] = 5L
                pluginInfos["b"] = 10L
            }

        val json = session.toJSONObject()

        // total = loadEngineEnd - openTime = 600 - 100 = 500
        assertEquals(500L, json.getLong("total"))
        // renderCost = loadEngineEnd - loadEngineStart = 600 - 520 = 80
        assertEquals(80L, json.getLong("renderCost"))
        // lynxViewInitCost = 250 - 200 = 50
        assertEquals(50L, json.getLong("lynxViewInitCost"))
        // lynxCost = renderCost + lynxViewInitCost = 80 + 50 = 130
        assertEquals(130L, json.getLong("lynxCost"))
        // prepareTemplateCost = 430 - 410 = 20
        assertEquals(20L, json.getLong("prepareTemplateCost"))
        // prepareJSBCost = 490 - 480 = 10
        assertEquals(10L, json.getLong("prepareJSBCost"))
        // preparePluginExecuteCost = 360 - 350 = 10
        assertEquals(10L, json.getLong("preparePluginExecuteCost"))
        // prepareFontCost = 320 - 300 = 20
        assertEquals(20L, json.getLong("prepareFontCost"))
        // prepareInitDataCost = 280 - 260 = 20
        assertEquals(20L, json.getLong("prepareInitDataCost"))
        // prepareComponentCost = 400 - 370 = 30
        assertEquals(30L, json.getLong("prepareComponentCost"))
        // prepareGlobalPropsCost = 340 - 330 = 10
        assertEquals(10L, json.getLong("prepareGlobalPropsCost"))
        // prepareComponentEnd2PrepareTemplateStart = 410 - 400 = 10
        assertEquals(10L, json.getLong("prepareComponentEnd2PrepareTemplateStart"))
        // prepareExtraInfoCost = 470 - 440 = 30
        assertEquals(30L, json.getLong("prepareExtraInfoCost"))
        // customInitCost = 150 - 110 = 40
        assertEquals(40L, json.getLong("customInitCost"))
        // sparkContainerCost = 520 - 100 - 50 - 10 = 360
        assertEquals(360L, json.getLong("sparkContainerCost"))

        val pluginInfos = json.getString("pluginInfos")
        assertTrue("expected pluginInfos JSON $pluginInfos", pluginInfos.contains("\"a\""))
        assertTrue("expected pluginInfos JSON $pluginInfos", pluginInfos.contains("\"b\""))
    }

    @Test
    fun toStringIncludesDerivedDurations() {
        val session =
            HybridLoadSession().apply {
                openTime = 0
                loadEngineStart = 1
                loadEngineEnd = 7
                lynxViewInitStart = 0
                lynxViewInitEnd = 0
                prepareTemplateStart = 0
                prepareTemplateEnd = 0
                prepareComponentEnd = 0
            }
        val text = session.toString()
        assertTrue(text.contains("total = 7"))
        assertTrue(text.contains("renderCost = 6"))
    }
}
