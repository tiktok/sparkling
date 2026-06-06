// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.utils

import com.tiktok.sparkling.hybridkit.base.HybridKitInitCallback
import com.tiktok.sparkling.hybridkit.base.HybridKitInitStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class HybridKitInitManagerTest {
    private class CountingCallback : HybridKitInitCallback {
        var calls = 0

        override fun isFinished() {
            calls += 1
        }
    }

    @Before
    fun reset() {
        clearCallbacks()
        HybridKitInitManager.status = HybridKitInitStatus.INIT
    }

    @After
    fun teardown() {
        clearCallbacks()
        HybridKitInitManager.status = HybridKitInitStatus.INIT
    }

    @Test
    fun initHybridKitTransitionsStatusToFinishedAndNotifiesCallback() {
        val cb = CountingCallback()
        HybridKitInitManager.addCallback(cb)

        HybridKitInitManager.initHybridKit(null)

        assertEquals(HybridKitInitStatus.FINISHED, HybridKitInitManager.status)
        assertEquals(1, cb.calls)
    }

    @Test
    fun removedCallbackIsNoLongerInvoked() {
        val cb = CountingCallback()
        HybridKitInitManager.addCallback(cb)
        HybridKitInitManager.removeCallback(cb)

        HybridKitInitManager.initHybridKit(null)

        assertEquals(0, cb.calls)
    }

    @Test
    fun multipleCallbacksReceiveFinishNotification() {
        val cb1 = CountingCallback()
        val cb2 = CountingCallback()
        HybridKitInitManager.addCallback(cb1)
        HybridKitInitManager.addCallback(cb2)

        HybridKitInitManager.initHybridKit(null)

        assertEquals(1, cb1.calls)
        assertEquals(1, cb2.calls)
    }

    @Test
    fun callbacksListAcceptsAddAndRemoveOperations() {
        val cb = CountingCallback()
        assertTrue(addAndCheckSize(cb))
        // Removing a callback that was added should reduce the list back to 0.
        assertFalse(removeAndCheckExists(cb))
    }

    private fun addAndCheckSize(cb: HybridKitInitCallback): Boolean {
        HybridKitInitManager.addCallback(cb)
        return callbackList().contains(cb)
    }

    private fun removeAndCheckExists(cb: HybridKitInitCallback): Boolean {
        HybridKitInitManager.removeCallback(cb)
        return callbackList().contains(cb)
    }

    private fun callbackList(): MutableList<*> {
        val field = HybridKitInitManager::class.java.getDeclaredField("callbacks")
        field.isAccessible = true
        return field.get(HybridKitInitManager) as MutableList<*>
    }

    private fun clearCallbacks() {
        val field = HybridKitInitManager::class.java.getDeclaredField("callbacks")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (field.get(HybridKitInitManager) as MutableList<HybridKitInitCallback>).clear()
    }
}
