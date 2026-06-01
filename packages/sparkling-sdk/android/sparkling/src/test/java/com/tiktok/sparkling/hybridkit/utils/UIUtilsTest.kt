// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.utils

import android.content.Context
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], packageName = "com.tiktok.sparkling")
class UIUtilsTest {
    private fun mockContextWithDensity(
        density: Float,
        heightPixels: Int = 0,
    ): Context {
        val ctx = mockk<Context>()
        val resources = mockk<android.content.res.Resources>()
        val dm =
            DisplayMetrics().apply {
                this.density = density
                this.heightPixels = heightPixels
            }
        every { ctx.resources } returns resources
        every { resources.displayMetrics } returns dm
        return ctx
    }

    @Test
    fun dip2PxScalesByDensityWithRoundingOffset() {
        val ctx = mockContextWithDensity(2.0f)
        // 10 * 2.0 + 0.5 = 20.5
        assertEquals(20.5f, UIUtils.dip2Px(ctx, 10f), 1e-6f)
    }

    @Test
    fun px2dipDividesByDensityWithRoundingOffsetAndIntCast() {
        val ctx = mockContextWithDensity(2.0f)
        // 41 / 2.0 + 0.5 = 21.0 -> 21
        assertEquals(21, UIUtils.px2dip(ctx, 41f))
    }

    @Test
    fun getScreenHeightReturnsZeroForNullContext() {
        assertEquals(0, UIUtils.getScreenHeight(null))
    }

    @Test
    fun getScreenHeightReadsDisplayMetricsHeightPixels() {
        val ctx = mockContextWithDensity(1.0f, heightPixels = 1920)
        assertEquals(1920, UIUtils.getScreenHeight(ctx))
    }

    @Test
    fun removeParentViewIsNoOpForNullViewOrViewWithoutParent() {
        UIUtils.removeParentView(null) // null safe
        val view = mockk<View>()
        every { view.parent } returns null
        UIUtils.removeParentView(view)
        // No verifications needed; just ensuring no crash.
    }

    @Test
    fun removeParentViewIsNoOpWhenParentIsNotViewGroup() {
        val view = mockk<View>()
        // android.view.ViewParent that is not a ViewGroup
        val parent = mockk<android.view.ViewParent>()
        every { view.parent } returns parent
        UIUtils.removeParentView(view)
        // Nothing to verify; we only assert no class cast or NPE.
    }

    @Test
    fun removeParentViewDetachesFromViewGroupParent() {
        val view = mockk<View>()
        val parent = mockk<ViewGroup>()
        every { view.parent } returns parent
        every { parent.removeView(view) } just Runs

        UIUtils.removeParentView(view)

        verify(exactly = 1) { parent.removeView(view) }
    }
}
