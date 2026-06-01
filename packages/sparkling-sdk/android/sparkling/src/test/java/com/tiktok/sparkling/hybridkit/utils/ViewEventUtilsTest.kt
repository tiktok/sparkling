// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.utils

import com.tiktok.sparkling.hybridkit.HybridContext
import com.tiktok.sparkling.hybridkit.KitViewManager
import com.tiktok.sparkling.hybridkit.base.IKitView
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * Tests for [ViewEventUtils] state machine: it tracks the lifecycle of each
 * container and sends synthesized JS events. We use Robolectric to advance the
 * main-thread handler so the delayed dispatch runs synchronously.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ViewEventUtilsTest {
    private lateinit var view: IKitView

    @Before
    fun setUp() {
        clearAllMocks()
        view = mockk(relaxed = true)
        mockkObject(KitViewManager)
        every { KitViewManager.getKitView(any()) } returns view
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun runMainLooperToIdle() {
        // The dispatch uses a 500ms delay; advance virtual time past it so
        // delayed Runnables are executed.
        shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
    }

    @Test
    fun onPauseDispatchesViewDisappearedAndDelayedHiddenEvent() {
        val ctx = HybridContext()
        ViewEventUtils.onPause(ctx)
        // immediate viewDisappeared
        verify(exactly = 1) { view.sendEvent(ViewEventUtils.VIEW_DISAPPEARED, null) }

        // run delayed posts -> viewDisappearedWithType{type=covered}
        runMainLooperToIdle()
        verify(atLeast = 1) { view.sendEventByJSON(eq("viewDisappearedWithType"), any()) }
    }

    @Test
    fun onPauseWithNullContextIsNoOp() {
        ViewEventUtils.onPause(null)
        verify(exactly = 0) { view.sendEvent(any(), any()) }
    }

    @Test
    fun onPauseRepeatedSecondTimeIsNoopForDelayedDispatch() {
        val ctx = HybridContext()
        ViewEventUtils.onPause(ctx)
        runMainLooperToIdle()

        // After the delayed dispatch removed the entry, calling onPause again should
        // still dispatch viewDisappeared synchronously.
        ViewEventUtils.onPause(ctx)
        // dispatcher will run again
        runMainLooperToIdle()

        verify(atLeast = 2) { view.sendEvent(ViewEventUtils.VIEW_DISAPPEARED, null) }
    }

    @Test
    fun onShowFirstCallEmitsViewAppeared() {
        val ctx = HybridContext()
        ViewEventUtils.onShow(ctx)
        verify(exactly = 1) { view.sendEvent(ViewEventUtils.VIEW_APPEARED, null) }
    }

    @Test
    fun onShowAfterPauseTransitionsToResumedAndDelayedAppears() {
        val ctx = HybridContext()
        ViewEventUtils.onPause(ctx) // -> PAUSED
        ViewEventUtils.onShow(ctx) // -> RESUMED, no immediate viewAppeared yet
        runMainLooperToIdle()

        // After the delayed branch runs in RESUMED state, it sends both
        // viewDisappearedWithType and viewAppeared.
        verify(atLeast = 1) { view.sendEvent(ViewEventUtils.VIEW_APPEARED, null) }
    }

    @Test
    fun onDestroyEmitsViewDisappearedWithType() {
        val ctx = HybridContext()
        ViewEventUtils.onDestroy(ctx)
        verify { view.sendEventByJSON(eq("viewDisappearedWithType"), any()) }
    }

    @Test
    fun onShowAfterDestroyDoesNotEmitViewAppeared() {
        // separate test to keep mock counters clean
        val freshView = mockk<IKitView>(relaxed = true)
        every { KitViewManager.getKitView(any()) } returns freshView
        val ctx = HybridContext()
        ViewEventUtils.onDestroy(ctx)
        ViewEventUtils.onShow(ctx)
        verify(exactly = 0) { freshView.sendEvent(ViewEventUtils.VIEW_APPEARED, null) }
    }
}
