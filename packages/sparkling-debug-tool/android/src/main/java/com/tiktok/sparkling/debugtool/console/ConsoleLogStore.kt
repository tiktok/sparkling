// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool.console

import android.os.Handler
import android.os.Looper
import java.util.ArrayDeque
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe ring buffer of [ConsoleLog]s shared by every [com.tiktok.sparkling.debugtool.console.SparklingConsoleFragment]
 * instance and any code feeding the console (the Lynx [LynxConsoleSink] or the
 * deprecated `SparklingDebugTool.recordConsoleLine` bridge).
 */
object ConsoleLogStore {
    @Volatile
    private var capacity: Int = 300
    private val buffer = ArrayDeque<ConsoleLog>(capacity)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<Listener>()

    interface Listener {
        fun onChanged()
    }

    fun setCapacity(newCapacity: Int) {
        synchronized(buffer) {
            capacity = newCapacity.coerceAtLeast(1)
            while (buffer.size > capacity) {
                buffer.removeFirst()
            }
        }
        dispatchChanged()
    }

    fun add(log: ConsoleLog) {
        synchronized(buffer) {
            val last = buffer.lastOrNull()
            if (last != null &&
                last.type == log.type &&
                last.tag == log.tag &&
                last.message == log.message &&
                log.timestamp - last.timestamp < 500
            ) {
                return
            }
            if (buffer.size >= capacity) {
                buffer.removeFirst()
            }
            buffer.addLast(log)
        }
        dispatchChanged()
    }

    fun snapshot(): List<ConsoleLog> = synchronized(buffer) { buffer.toList() }

    fun clear() {
        synchronized(buffer) { buffer.clear() }
        dispatchChanged()
    }

    fun addListener(listener: Listener) {
        listeners.addIfAbsent(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    private fun dispatchChanged() {
        if (listeners.isEmpty()) return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            listeners.forEach { runCatching { it.onChanged() } }
        } else {
            mainHandler.post {
                listeners.forEach { runCatching { it.onChanged() } }
            }
        }
    }
}
