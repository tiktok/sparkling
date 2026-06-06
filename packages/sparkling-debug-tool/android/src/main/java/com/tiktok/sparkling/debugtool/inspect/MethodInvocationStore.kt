// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool.inspect

import android.os.Handler
import android.os.Looper
import java.util.ArrayDeque

/**
 * Thread-safe ring buffer of recent [MethodInvocation] records that the debug
 * panel renders. Listeners are dispatched on the main thread.
 */
object MethodInvocationStore {
    interface Listener {
        fun onMethodInvocationsChanged()
    }

    private val main = Handler(Looper.getMainLooper())
    private val lock = Any()
    private val buffer = ArrayDeque<MethodInvocation>()
    private val byId = HashMap<String, Int>()
    private val listeners = mutableListOf<Listener>()
    private var capacity: Int = 300

    fun setCapacity(capacity: Int) =
        synchronized(lock) {
            this.capacity = capacity.coerceAtLeast(1)
            trimToCapacity()
            notifyChanged()
        }

    fun add(record: MethodInvocation) {
        synchronized(lock) {
            buffer.addLast(record)
            byId[record.id] = buffer.size - 1
            trimToCapacity()
        }
        notifyChanged()
    }

    /** Update an existing record by id (e.g. when the response arrives). */
    fun update(record: MethodInvocation) {
        synchronized(lock) {
            // Linear search is acceptable for small capacities; the hash index
            // gets stale after deque shifts so we re-resolve on demand.
            val iter = buffer.iterator()
            var index = 0
            while (iter.hasNext()) {
                val current = iter.next()
                if (current.id == record.id) {
                    val list = buffer.toMutableList()
                    list[index] = record
                    buffer.clear()
                    buffer.addAll(list)
                    rebuildIndex()
                    notifyChanged()
                    return
                }
                index++
            }
        }
    }

    fun snapshot(): List<MethodInvocation> = synchronized(lock) { buffer.toList() }

    fun clear() {
        synchronized(lock) {
            buffer.clear()
            byId.clear()
        }
        notifyChanged()
    }

    fun addListener(listener: Listener) {
        synchronized(listeners) {
            if (!listeners.contains(listener)) listeners.add(listener)
        }
    }

    fun removeListener(listener: Listener) {
        synchronized(listeners) { listeners.remove(listener) }
    }

    private fun trimToCapacity() {
        while (buffer.size > capacity) {
            val removed = buffer.removeFirst()
            byId.remove(removed.id)
        }
    }

    private fun rebuildIndex() {
        byId.clear()
        var index = 0
        buffer.forEach { byId[it.id] = index++ }
    }

    private fun notifyChanged() {
        val snapshot = synchronized(listeners) { listeners.toList() }
        if (snapshot.isEmpty()) return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            snapshot.forEach { it.onMethodInvocationsChanged() }
        } else {
            main.post { snapshot.forEach { it.onMethodInvocationsChanged() } }
        }
    }
}
