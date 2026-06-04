// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool.inspect

import java.util.concurrent.CopyOnWriteArraySet

/**
 * One Sparkling Lynx instance's runtime props as displayed by the debug
 * panel. The panel splits the underlying map into top-level globalProps and
 * the nested `queryItems` map (matching the host app's runtime layout).
 */
data class GlobalPropsSnapshot(
    val containerId: String,
    val templateUrl: String?,
    val globalProps: Map<String, Any?>,
    val queryItems: Map<String, Any?>,
)

/**
 * Bridge between the host application's view registry and the debug panel.
 *
 * The host registers a [Provider] that walks every live LynxView (or
 * equivalent) and returns a [GlobalPropsSnapshot]. The panel calls
 * [collectAll] every time it is opened or refreshed.
 */
object GlobalPropsRegistry {
    fun interface Provider {
        fun collect(): List<GlobalPropsSnapshot>
    }

    @Volatile
    private var provider: Provider? = null
    private val listeners = CopyOnWriteArraySet<() -> Unit>()

    @JvmStatic
    fun setProvider(provider: Provider?) {
        this.provider = provider
        notifyChanged()
    }

    @JvmStatic
    fun collectAll(): List<GlobalPropsSnapshot> = provider?.collect().orEmpty()

    @JvmStatic
    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    @JvmStatic
    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    @JvmStatic
    fun notifyChanged() {
        listeners.forEach { runCatching { it.invoke() } }
    }
}
