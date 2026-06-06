// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class IDependencyIteratorTest {
    private class Node(
        val tag: String,
    ) : AbsDependencyIterator<Node>()

    @Test
    fun nextSetterAndGetterUseSharedState() {
        val a = Node("a")
        val b = Node("b")
        assertNull(a.next())
        a.next(b)
        assertSame(b, a.next())
        assertSame(b, a.next)
    }

    @Test
    fun extensionIteratorVisitsLinkedList() {
        val a = Node("a")
        val b = Node("b")
        val c = Node("c")
        a.next(b)
        b.next(c)

        val visited = mutableListOf<String>()
        (a as IDependencyIterator<Node>).iterator { visited += it.tag }
        assertEquals(listOf("a", "b", "c"), visited)
    }

    @Test
    fun extensionIteratorOnNullIsNoop() {
        val tagged = mutableListOf<String>()
        val nullIterator: IDependencyIterator<Node>? = null
        nullIterator.iterator { tagged += it.tag }
        assertEquals(emptyList<String>(), tagged)
    }

    @Test
    fun iteratorTerminatesWhenNextReturnsNull() {
        val a = Node("a")
        val visited = mutableListOf<String>()
        (a as IDependencyIterator<Node>).iterator { visited += it.tag }
        assertEquals(listOf("a"), visited)
    }
}
