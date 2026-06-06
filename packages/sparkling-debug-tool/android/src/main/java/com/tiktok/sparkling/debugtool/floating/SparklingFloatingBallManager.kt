// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool.floating

import android.app.Activity
import android.app.Application

/**
 * Keeps the historical floating-ball API surface while Sparkling's in-app
 * debug entry point lives on each SparklingView's bottom-left debugTag.
 */
object SparklingFloatingBallManager {
    /** Optional callbacks kept for binary compatibility with older hosts. */
    interface ActionHandler {
        fun onTap(activity: Activity): Boolean = false

        fun onLongPress(activity: Activity): Boolean = false
    }

    private var enabled = false

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    fun install(application: Application) = Unit

    @JvmStatic
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    @JvmStatic
    fun isEnabled(): Boolean = enabled

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    fun setActionHandler(handler: ActionHandler?) = Unit

    /** Kept for binary compatibility; SparklingView owns debugTag visibility. */
    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    fun setVisibilityForCurrentActivity(visible: Boolean) = Unit
}
