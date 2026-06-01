// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.base

import android.view.View
import com.tiktok.sparkling.hybridkit.HybridContext
import io.mockk.mockk
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Drives every default implementation in [IKitView] via a tiny stub that only
 * implements the abstract surface, so JaCoCo records the default bodies and
 * the dispatch into [HybridContext.sendEventListener] / [absSendEventListener].
 */
class IKitViewDefaultImplsTest {
    /**
     * A stub implementation of [IKitView] that satisfies the abstract members
     * with no-ops. Default methods come from the interface itself.
     */
    private class StubKitView(
        override var hybridContext: HybridContext,
    ) : IKitView {
        override fun realView(): View? = null

        override fun load() {}

        override fun load(uri: String) {}

        override fun reload() {}

        override fun onShow() {}

        override fun onHide() {}

        override fun destroy(clearContext: Boolean) {}

        override fun hasDestroyed(): Boolean = false

        override fun getGlobalProps(): MutableMap<String, Any>? = null

        override fun getScheme(): String? = null

        override fun onLoadSuccess() {}
    }

    @Test
    fun defaultEmptyBodyMethodsAreNoOps() {
        val view = StubKitView(HybridContext())
        // every default-body method should be reachable without throwing
        view.refreshSchemeParam(mockk(relaxed = true))
        view.refreshContext(mockk(relaxed = true))
        view.updateData(emptyMap())
        view.updateDataByJson("{}")
        view.updateDataWithExtra("{}", emptyMap())
        view.updateDataWithExtra(listOf("{}"), emptyMap())
        view.resetData(emptyMap())
        view.resetDataByJson("{}")
        view.resetDataWithExtra("{}", emptyMap())
        view.resetDataWithExtra(listOf("{}"), emptyMap())
        view.updateGlobalPropsByIncrement(emptyMap())
        view.getCurrentData(null)
        view.destroyWhenJSRuntimeCallback()
        assertTrue(view.readyToSendEvent())
        assertNull(view.getAndRemoveForestResponse())
    }

    @Test
    fun sendEventVariantsInvokeListenerOnContext() {
        val ctx = HybridContext()
        val captured = mutableListOf<Triple<String, String, Any?>>()
        ctx.sendEventListener = { kit, eventName, params ->
            captured += Triple(kit.javaClass.simpleName, eventName, params)
        }

        val view = StubKitView(ctx)
        val list = listOf("a", "b")
        val json = JSONObject().put("k", "v")
        val map = mapOf<String, Any?>("k" to "v")

        @Suppress("DEPRECATION")
        view.sendEvent("evt-list", list)
        view.sendEventByJSON("evt-json", json)
        view.sendEventByMap("evt-map", map)

        assertEquals(3, captured.size)
        assertEquals("evt-list", captured[0].second)
        assertEquals(list, captured[0].third)
        assertEquals("evt-json", captured[1].second)
        assertEquals(json, captured[1].third)
        assertEquals("evt-map", captured[2].second)
        assertEquals(map, captured[2].third)
    }

    @Test
    fun sendEventVariantsHandleNullParams() {
        val ctx = HybridContext()
        val view = StubKitView(ctx)
        @Suppress("DEPRECATION")
        view.sendEvent("evt", null)
        view.sendEventByJSON("evt", null)
        view.sendEventByMap("evt", null)
        // listener is null so nothing fires; reaching here is the assertion.
    }
}
