// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool.console

import android.util.Log
import com.lynx.devtool.LynxInspectorConsoleDelegate
import com.lynx.tasm.LynxView
import java.util.WeakHashMap

/**
 * Attaches a [LynxInspectorConsoleDelegate] to each [LynxView] and forwards every
 * `console.*` payload (a JSON string) into [ConsoleLogStore].
 *
 * Lynx 3.6 exposes `LynxView.getBaseInspectorOwner().setLynxInspectorConsoleDelegate(Object)`
 * - we call the public API directly.
 */
object LynxConsoleSink {
    private const val TAG = "LynxConsoleSink"

    private val attached = WeakHashMap<LynxView, LynxInspectorConsoleDelegate>()

    @Synchronized
    fun attach(lynxView: LynxView): Boolean {
        if (attached.containsKey(lynxView)) return true
        val delegate = SinkDelegate()
        return if (installDelegate(lynxView, delegate)) {
            attached[lynxView] = delegate
            true
        } else {
            false
        }
    }

    @Synchronized
    fun detach(lynxView: LynxView) {
        if (attached.remove(lynxView) == null) return
        installDelegate(lynxView, null)
    }

    private fun installDelegate(
        lynxView: LynxView,
        delegate: LynxInspectorConsoleDelegate?,
    ): Boolean {
        val owner =
            runCatching { lynxView.baseInspectorOwner }.getOrNull() ?: run {
                Log.w(TAG, "LynxView.baseInspectorOwner is null; devtool not initialized?")
                return false
            }
        return try {
            owner.setLynxInspectorConsoleDelegate(delegate)
            true
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to install LynxInspectorConsoleDelegate", t)
            false
        }
    }

    private class SinkDelegate : LynxInspectorConsoleDelegate {
        override fun onConsoleMessage(msg: String?) {
            if (msg.isNullOrEmpty()) return
            val log = ConsoleLog.fromJson(msg) ?: return
            ConsoleLogStore.add(log)
        }
    }
}
