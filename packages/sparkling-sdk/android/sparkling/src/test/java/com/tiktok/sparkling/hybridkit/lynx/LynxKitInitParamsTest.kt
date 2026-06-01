// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.lynx

import android.net.Uri
import com.lynx.tasm.LynxViewClient
import com.lynx.tasm.behavior.Behavior
import com.tiktok.sparkling.hybridkit.base.HybridKitType
import com.tiktok.sparkling.hybridkit.scheme.HybridSchemeParam
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [LynxKitInitParams] focused on its kotlin property accessors,
 * default-args constructor, and the global-props / behavior helpers.
 */
@RunWith(RobolectricTestRunner::class)
class LynxKitInitParamsTest {
    private fun newParams(uri: Uri? = null): LynxKitInitParams = LynxKitInitParams(null, null, null, "", uri)

    @Test
    fun secondaryConstructorPopulatesPrimaryFields() {
        val uri = mockk<Uri>(relaxed = true)
        val modules = mutableMapOf<String, SparklingLynxModuleWrapper>()
        val behaviors = mutableListOf<Behavior>()
        val initData = LynxInitData()

        val params = LynxKitInitParams(modules, behaviors, initData, "fonts.ttf", uri)

        assertEquals(HybridKitType.LYNX, params.type)
        assertEquals(uri, params.loadUri)
        assertEquals(modules, params.lynxModules)
        assertEquals(behaviors, params.lynxBehaviors)
        assertEquals(initData, params.initData)
        assertEquals("fonts.ttf", params.preloadFonts)
        assertNull(params.templateBundle)
        assertNull(params.templateBundleDeferred)
    }

    @Test
    fun applyAndGetHybridSchemeParam() {
        val params = newParams()
        assertNull(params.getHybridSchemeParam())

        val scheme = mockk<HybridSchemeParam>(relaxed = true)
        params.applyHybridSchemeParam(scheme)

        assertEquals(scheme, params.getHybridSchemeParam())
        assertEquals(scheme, params.hybridSchemaParams)
    }

    @Test
    fun globalPropsSetMergeRemoveAndObtain() {
        val params = newParams()
        // initial empty
        assertTrue(params.obtainGlobalProps()!!.isEmpty())

        params.setGlobalProps(mapOf("a" to 1, "b" to "two"))
        params.setGlobalProps(mapOf("c" to true))

        var props = params.obtainGlobalProps()!!
        assertEquals(1, props["a"])
        assertEquals("two", props["b"])
        assertEquals(true, props["c"])

        params.removeGlobalProps(listOf("a", "missing"))
        props = params.obtainGlobalProps()!!
        assertNull(props["a"])
        assertEquals("two", props["b"])

        // null inputs are no-ops
        params.setGlobalProps(null)
        params.removeGlobalProps(null)
        assertEquals("two", params.obtainGlobalProps()!!["b"])
    }

    @Test
    fun lynxClientDelegateAddAndGet() {
        val params = newParams()
        val delegate1 = mockk<LynxViewClient>(relaxed = true)
        val delegate2 = mockk<LynxViewClient>(relaxed = true)
        params.addLynxClientDelegate(delegate1)
        params.addLynxClientDelegate(delegate2)

        val list = params.lynxClientDelegate()
        assertEquals(2, list.size)
        assertEquals(delegate1, list[0])
        assertEquals(delegate2, list[1])
    }

    @Test
    fun addBehavioursInitializesListWhenNull() {
        val params = newParams()
        assertNull(params.lynxBehaviors)

        val b1 = mockk<Behavior>(relaxed = true)
        val b2 = mockk<Behavior>(relaxed = true)
        params.addBehaviours(mutableListOf(b1, b2))

        assertNotNull(params.lynxBehaviors)
        assertEquals(2, params.lynxBehaviors!!.size)

        // adding more appends to the existing list
        val b3 = mockk<Behavior>(relaxed = true)
        params.addBehaviours(mutableListOf(b3))
        assertEquals(3, params.lynxBehaviors!!.size)
    }

    @Test
    fun getTemplateBundleIncludesDeferredFallsBackWhenDeferredIsNull() {
        val params = newParams()
        assertNull(params.getTemplateBundleIncludesDeferred())

        // when no deferred is set, it returns templateBundle, which is also null
        params.templateBundleDeferred = { null }
        assertNull(params.getTemplateBundleIncludesDeferred())
    }

    @Test
    fun extraInfoCallbackHasDefaultNoopImpls() {
        // make sure the default body is hit (it's empty, but JaCoCo still tracks the
        // companion + default method bytecode).
        val cb = ExtraInfoCallback()
        cb.extraInfoParsedInPreDecode(emptyMap())
        cb.extraInfoParsed(emptyMap())
    }

    @Test
    fun defaultPropertyValuesAreSensible() {
        val params = newParams()
        assertNull(params.lynxModules)
        assertNull(params.lynxBehaviors)
        assertNull(params.initData)
        assertEquals("", params.preloadFonts)
        assertNull(params.templateBundle)
        assertNull(params.templateArray)
        assertNull(params.lynxWidth)
        assertNull(params.lynxHeight)
        assertEquals(HybridSchemeParam.DefaultPresetHeight, params.presetHeightSpec)
        assertEquals(HybridSchemeParam.DefaultPresetWidth, params.presetWidthSpec)
        assertNull(params.fontScale)
        assertNull(params.templateData)
        assertNull(params.extraInfoCallback)
        assertNull(params.kitBridgeService)
        assertNull(params.lynxBackgroundRuntime)
    }
}
