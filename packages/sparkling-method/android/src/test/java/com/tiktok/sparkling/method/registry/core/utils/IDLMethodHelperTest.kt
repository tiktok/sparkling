// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.registry.core.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Covers [IDLMethodHelper.getActivity]/[IDLMethodHelper.getActContext] for all branches:
 *  - immediate Activity hit
 *  - bubble up through nested ContextWrapper layers
 *  - null base context
 *  - non-ContextWrapper context returns null with a warning log
 */
@RunWith(RobolectricTestRunner::class)
class IDLMethodHelperTest {
    @Test
    fun getActivityReturnsImmediatelyWhenContextIsActivity() {
        val activity = mockk<Activity>()
        assertSame(activity, IDLMethodHelper.getActivity(activity))
    }

    @Test
    fun getActivityBubblesUpThroughContextWrappers() {
        val activity = mockk<Activity>()
        val innerWrapper = mockk<ContextWrapper>()
        val outerWrapper = mockk<ContextWrapper>()
        every { outerWrapper.baseContext } returns innerWrapper
        every { innerWrapper.baseContext } returns activity

        assertSame(activity, IDLMethodHelper.getActivity(outerWrapper))
    }

    @Test
    fun getActivityReturnsNullWhenWrapperChainEndsAtNull() {
        val wrapper = mockk<ContextWrapper>()
        every { wrapper.baseContext } returns null

        assertNull(IDLMethodHelper.getActivity(wrapper))
    }

    @Test
    fun getActivityReturnsNullForPlainContext() {
        val plain = mockk<Context>()
        assertNull(IDLMethodHelper.getActivity(plain))
    }

    @Test
    fun getActivityHandlesNullInput() {
        assertNull(IDLMethodHelper.getActivity(null))
    }

    @Test
    fun getActContextDelegatesToGetActivity() {
        val activity = mockk<Activity>()
        assertSame(activity, IDLMethodHelper.getActContext(activity))
        assertNull(IDLMethodHelper.getActContext(mockk<Context>()))
    }
}
