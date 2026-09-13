// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import java.util.concurrent.ConcurrentHashMap

/**
 * Who handles the hardware back button.
 *
 * By default the container does: a page that is not the task root finishes, and
 * one that is asks for a second press before leaving the app. Neither is
 * something a page can take over, so a page with unsaved work, an open sheet, or
 * routing of its own has no way to be consulted before it is closed.
 *
 * A page that wants to handle back calls `router.setBackPressIntercept` and then
 * listens for the `onBackPress` event. While it is intercepting, the container
 * dispatches the event and does nothing else - going back is then the page's
 * decision, made by calling `router.close` when it is ready.
 */
object SparklingBackPress {
    /** The event a page listens for while it is intercepting. */
    const val EVENT = "onBackPress"

    /** What the container does when back is pressed on the task root. */
    enum class RootBehavior {
        /** Show "click again to exit" and leave on the second press within 2s. */
        CONFIRM_THEN_EXIT,

        /** Leave immediately, the platform default. */
        EXIT,
    }

    @Volatile
    var rootBehavior: RootBehavior = RootBehavior.CONFIRM_THEN_EXIT

    /** How long a first press stays remembered, in milliseconds. */
    @Volatile
    var confirmWindowMillis: Long = 2000

    private val intercepting = ConcurrentHashMap.newKeySet<String>()

    @JvmStatic
    fun setIntercepting(
        containerId: String?,
        value: Boolean,
    ) {
        val id = containerId?.takeIf { it.isNotBlank() } ?: return
        if (value) {
            intercepting.add(id)
        } else {
            intercepting.remove(id)
        }
    }

    @JvmStatic
    fun isIntercepting(containerId: String?): Boolean =
        containerId != null && intercepting.contains(containerId)

    /**
     * Forget a container.
     *
     * Called when its activity is destroyed: a page that was intercepting and is
     * gone must not leave its id behind, or a later container that happened to
     * be given the same id would inherit an interception nobody asked for.
     */
    @JvmStatic
    fun forget(containerId: String?) {
        containerId?.let { intercepting.remove(it) }
    }

    /** Test seam; not part of the public surface. */
    internal fun reset() {
        intercepting.clear()
        rootBehavior = RootBehavior.CONFIRM_THEN_EXIT
        confirmWindowMillis = 2000
    }
}
