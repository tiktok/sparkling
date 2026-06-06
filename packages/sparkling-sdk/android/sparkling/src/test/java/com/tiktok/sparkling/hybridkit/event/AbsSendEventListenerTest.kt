// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.event

import com.tiktok.sparkling.hybridkit.api.iterator
import com.tiktok.sparkling.hybridkit.base.IKitView
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AbsSendEventListenerTest {
    private class RecordingListener(
        val tag: String,
    ) : AbsSendEventListener() {
        val invocations = mutableListOf<Triple<IKitView, String, Any?>>()

        override fun invoke(
            kitView: IKitView,
            eventName: String,
            params: Any?,
        ) {
            invocations += Triple(kitView, eventName, params)
        }
    }

    @Test
    fun nextChainExposesLinkedListBehavior() {
        val first = RecordingListener("first")
        val second = RecordingListener("second")
        first.next(second)

        assertSame(second, first.next())
        assertSame(second, first.next)

        // Direct property assignment should also work.
        val third = RecordingListener("third")
        first.next = third
        assertSame(third, first.next())
    }

    @Test
    fun iteratorVisitsEntireChainInOrder() {
        val first = RecordingListener("first")
        val second = RecordingListener("second")
        val third = RecordingListener("third")
        first.next(second)
        second.next(third)

        val visited = mutableListOf<String>()
        first.iterator { listener ->
            visited += (listener as RecordingListener).tag
        }
        assertEquals(listOf("first", "second", "third"), visited)
    }

    @Test
    fun invokingListenerForwardsArguments() {
        val listener = RecordingListener("only")
        val view = mockk<IKitView>(relaxed = true)
        listener.invoke(view, "ev", mapOf("k" to "v"))
        assertEquals(1, listener.invocations.size)
        assertEquals("ev", listener.invocations[0].second)
        assertEquals(mapOf("k" to "v"), listener.invocations[0].third)
    }
}
