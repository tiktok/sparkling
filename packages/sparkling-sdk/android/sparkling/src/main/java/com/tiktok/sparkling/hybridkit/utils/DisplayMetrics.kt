// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.utils

import android.app.Activity
import android.content.Context
import android.graphics.Rect

// Extension functions for display metrics calculations with high precision.
// These provide more accurate measurements compared to integer-based calculations.

/**
 * Converts a pixel value to dp using the context's display density.
 *
 * @param context The context to get display metrics from
 * @return The value in dp as a Double
 */
operator fun Number.div(context: Context): Double = toDouble() / context.resources.displayMetrics.density

/**
 * Gets the status bar height in pixels for this context.
 */
val Context.statusBarHeight: Int
    get() = DevicesUtil.getStatusBarHeight(this)

/**
 * Gets the status bar height in dp for this context.
 */
val Context.statusBarHeightDp: Double
    get() = statusBarHeight / this

/**
 * Calculates the safe area height in dp for this Activity.
 * The safe area is the visible content area excluding the status bar.
 *
 * @param statusBarHeightDp The status bar height in dp (defaults to this activity's status bar height)
 * @return The safe area height in dp as a Double
 */
fun Activity.safeAreaHeight(statusBarHeightDp: Number = this.statusBarHeightDp): Double {
    val bottom =
        try {
            window?.run { Rect().also(decorView::getWindowVisibleDisplayFrame).bottom }
        } catch (e: NullPointerException) {
            null
        } ?: resources.displayMetrics.heightPixels
    return bottom / this - statusBarHeightDp.toDouble()
}
