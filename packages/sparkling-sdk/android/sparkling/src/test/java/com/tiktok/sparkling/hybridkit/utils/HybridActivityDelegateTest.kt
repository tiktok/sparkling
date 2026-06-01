// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.utils

import android.app.Activity
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class HybridActivityDelegateTest {
    @Test
    fun setTopActivityRetainsWeakReferenceToActivity() {
        val activity = mockk<Activity>(relaxed = true)
        HybridActivityDelegate.setTopActivity(activity)
        assertEquals(activity, HybridActivityDelegate.getTopActivity())
    }

    @Test
    fun replacingTopActivityOverridesPreviousValue() {
        val first = mockk<Activity>(relaxed = true)
        val second = mockk<Activity>(relaxed = true)
        HybridActivityDelegate.setTopActivity(first)
        HybridActivityDelegate.setTopActivity(second)
        assertEquals(second, HybridActivityDelegate.getTopActivity())
    }

    @Test
    fun getTopActivityReturnsNonNullAfterSet() {
        val activity = mockk<Activity>(relaxed = true)
        HybridActivityDelegate.setTopActivity(activity)
        // Just verify retrieval works repeatedly without errors.
        val first = HybridActivityDelegate.getTopActivity()
        val second = HybridActivityDelegate.getTopActivity()
        assertEquals(first, second)
        assertEquals(activity, first)
    }

    @Test
    fun unsafeWeakReferenceIsResettableToNullViaReflection() {
        // Force the internal weak reference to null to exercise the null branch
        // of getTopActivity even after a previous test populated it.
        val field = HybridActivityDelegate::class.java.getDeclaredField("topActivityRef")
        field.isAccessible = true
        field.set(HybridActivityDelegate, null)
        assertNull(HybridActivityDelegate.getTopActivity())
    }
}
