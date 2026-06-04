// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool.inspect

import android.os.Handler
import android.os.Looper
import com.lynx.tasm.LynxView
import com.tiktok.sparkling.debug.SparklingDebugToolRegistry
import com.tiktok.sparkling.debugtool.console.LynxConsoleLogDelegate
import com.tiktok.sparkling.debugtool.console.LynxConsoleSink
import com.tiktok.sparkling.hybridkit.KitViewManager
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Starts the typed debug-tool SPI and attaches console capture to already-live
 * Sparkling views. Runtime events are delivered through
 * `SparklingDebugToolProviderImpl` via [SparklingDebugToolRegistry].
 */
internal object SparklingDebugAutoWiring {
    private val installed = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun installOnce() {
        if (!installed.compareAndSet(false, true)) return
        LynxConsoleLogDelegate.installOnce()
        SparklingDebugToolRegistry.hasDebugToolProvider()
        ensureConsoleAttachedToLiveKitViews()
    }

    fun ensureConsoleAttachedToLiveKitViews() {
        KitViewManager.getKitViews().values.forEach { attachConsoleWithRetry(it.realView()) }
    }

    fun attachConsoleWithRetry(
        view: Any?,
        attempt: Int = 0,
    ) {
        LynxConsoleLogDelegate.installOnce()
        val attached = (view as? LynxView)?.let { LynxConsoleSink.attach(it) } ?: false
        if (attached || attempt >= 6) return
        mainHandler.postDelayed({ attachConsoleWithRetry(view, attempt + 1) }, 250L * (attempt + 1))
    }
}
