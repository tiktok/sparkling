// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tests for [AsyncUtils]. These exercise both `submit` paths and verify the
 * executor selection behavior without leaking threads beyond test duration.
 */
class AsyncUtilsTest {
    @Test
    fun submitRunsTaskOnSequenceExecutor() {
        val latch = CountDownLatch(1)
        val invoked = AtomicInteger(0)

        AsyncUtils.submit(TaskType.Sequence) {
            invoked.incrementAndGet()
            latch.countDown()
        }

        assertTrue("sequence task should run", latch.await(2, TimeUnit.SECONDS))
        assertEquals(1, invoked.get())
    }

    @Test
    fun submitRunsTaskOnBlockedExecutor() {
        val latch = CountDownLatch(1)
        val invoked = AtomicInteger(0)

        AsyncUtils.submit(TaskType.Blocked) {
            invoked.incrementAndGet()
            latch.countDown()
        }

        assertTrue("blocked task should run", latch.await(2, TimeUnit.SECONDS))
        assertEquals(1, invoked.get())
    }

    @Test
    fun submitSwallowsExceptionsByDefault() {
        // The lambda runs on a background thread; if `submit` itself threw, this
        // call site would throw. We just assert no exception escapes.
        AsyncUtils.submit(TaskType.Sequence) {
            throw RuntimeException("ignored")
        }
    }

    @Test
    fun getExecutorReturnsDistinctExecutorsPerType() {
        val sequence = AsyncUtils.getExecutor(TaskType.Sequence)
        val blocked = AsyncUtils.getExecutor(TaskType.Blocked)
        assertNotNull(sequence)
        assertNotNull(blocked)
        assertNotSame(sequence, blocked)
        // Calling twice should return the same singleton instance.
        assertEquals(sequence, AsyncUtils.getExecutor(TaskType.Sequence))
    }
}
