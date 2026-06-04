// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool.console

import android.os.Process
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Android fallback for Lynx versions where `LynxView.baseInspectorOwner` is
 * null. Lynx still writes JS console output to this app process's logcat as
 * `lynx_console.cc`; debuggable apps can read their own process logs.
 */
internal object LynxConsoleLogcatBridge {
    private val started = AtomicBoolean(false)
    private val seenLines = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private const val TAG = "LynxConsoleLogcat"

    fun startOnce() {
        if (!started.compareAndSet(false, true)) return
        Thread({ runLoop() }, "sparkling-lynx-logcat").apply {
            isDaemon = true
            start()
        }
    }

    fun flushRecent() {
        runCatching {
            val process =
                ProcessBuilder(
                    "logcat",
                    "-d",
                    "--pid=${Process.myPid()}",
                    "-v",
                    "brief",
                ).redirectErrorStream(true)
                    .start()
            BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
                lines.forEach { line -> parseLine(line)?.let(ConsoleLogStore::add) }
            }
        }.onFailure {
            Log.w(TAG, "Failed to flush app logcat for Lynx console fallback", it)
        }
    }

    private fun runLoop() {
        runCatching {
            val process =
                ProcessBuilder(
                    "logcat",
                    "--pid=${Process.myPid()}",
                    "-v",
                    "brief",
                ).redirectErrorStream(true)
                    .start()
            BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
                lines.forEach { line -> parseLine(line)?.let(ConsoleLogStore::add) }
            }
        }.onFailure {
            Log.w(TAG, "Failed to read app logcat for Lynx console fallback", it)
        }
    }

    private fun parseLine(line: String): ConsoleLog? {
        if (!line.contains("lynx_console.cc") && !line.contains("LynxConsole") && !line.contains("ReactLynx")) return null
        if (!seenLines.add(line)) {
            return null
        }
        val level =
            when {
                line.contains(":ERROR:") || line.startsWith("E/") -> Log.ERROR
                line.contains(":WARNING:") || line.contains(":WARN:") || line.startsWith("W/") -> Log.WARN
                line.contains(":DEBUG:") || line.startsWith("D/") -> Log.DEBUG
                line.contains(":INFO:") || line.startsWith("I/") -> Log.INFO
                else -> Log.VERBOSE
            }
        return ConsoleLog(
            type = ConsoleLog.typeFromLevel(level),
            level = level,
            tag = "lynx",
            message = normalize(line),
        )
    }

    private fun normalize(line: String): String {
        val start = line.indexOf(")]")
        if (start != -1) {
            return line
                .substring(start + 2)
                .replace("   ||   ", " ")
                .replace('\n', ' ')
                .replace('\r', ' ')
                .trim()
        }
        return line
            .substringAfter("lynx_console.cc", line)
            .replace("   ||   ", " ")
            .replace('\n', ' ')
            .replace('\r', ' ')
            .trim()
    }
}
