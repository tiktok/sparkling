// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool.console

import android.util.Log
import com.lynx.tasm.base.AbsLogDelegate
import com.lynx.tasm.base.IComplicatedLogDelegate
import com.lynx.tasm.base.LLog
import com.lynx.tasm.base.LogSource
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Fallback capture path for Android Lynx builds where `baseInspectorOwner` is
 * null. Lynx still emits JS console lines through `LLog` with `LogSource.JS`,
 * so this delegate keeps the in-app Log tab useful even without an inspector
 * owner.
 */
internal object LynxConsoleLogDelegate : AbsLogDelegate(), IComplicatedLogDelegate {
    private val installed = AtomicBoolean(false)
    private var pendingSource: LogSource? = null
    private var pendingRuntimeId: Long? = null

    init {
        mMinimumLoggingLevel = Log.VERBOSE
    }

    fun installOnce() {
        installed.compareAndSet(false, true)
        LLog.setMinimumLoggingLevel(Log.VERBOSE)
        runCatching { LLog.setJSLogsFromExternalChannels(true) }
        runCatching { LLog.setDebugLoggingDelegate(this) }
            .onFailure { runCatching { LLog.addLoggingDelegate(this) } }
    }

    override fun getShouldFormatMessage(): Boolean = false

    override fun isLoggable(level: Int): Boolean = true

    override fun isLoggable(
        source: LogSource?,
        level: Int,
    ): Boolean = true

    override fun isComplicatedLogLoggable(
        level: Int,
        source: LogSource?,
        runtimeId: Long?,
    ): Boolean {
        pendingSource = source
        pendingRuntimeId = runtimeId
        return true
    }

    override fun v(
        tag: String?,
        msg: String?,
    ) = record(Log.VERBOSE, tag, msg)

    override fun d(
        tag: String?,
        msg: String?,
    ) = record(Log.DEBUG, tag, msg)

    override fun i(
        tag: String?,
        msg: String?,
    ) = record(Log.INFO, tag, msg)

    override fun w(
        tag: String?,
        msg: String?,
    ) = record(Log.WARN, tag, msg)

    override fun e(
        tag: String?,
        msg: String?,
    ) = record(Log.ERROR, tag, msg)

    override fun log(
        priority: Int,
        tag: String?,
        msg: String?,
    ) = record(priority, tag, msg)

    private fun record(
        priority: Int,
        tag: String?,
        msg: String?,
    ) {
        if (msg.isNullOrBlank()) return
        val source = pendingSource
        val runtimeId = pendingRuntimeId
        pendingSource = null
        pendingRuntimeId = null

        val isJsConsole =
            source == LogSource.JS ||
                source == LogSource.JS_EXT ||
                (source == null && runtimeId == null && msg.contains("console.cc") && !msg.contains("\"lepusRuntimeId:"))
        if (!isJsConsole) return

        ConsoleLogStore.add(
            ConsoleLog(
                type = ConsoleLog.typeFromLevel(priority),
                level = priority,
                tag = tag ?: "console",
                message = normalizeConsoleMessage(msg),
            ),
        )
    }

    private fun normalizeConsoleMessage(message: String): String =
        message
            .substringAfter(")]", message)
            .replace('\n', ' ')
            .replace('\r', ' ')
            .trim()
}
