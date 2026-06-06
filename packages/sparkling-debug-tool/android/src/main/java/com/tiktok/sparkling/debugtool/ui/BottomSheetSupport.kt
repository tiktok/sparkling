// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool.ui

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import kotlin.math.roundToInt

/**
 * Configures a [DialogFragment]'s window so it renders as a bottom-anchored
 * half-screen sheet. Call from `onStart()`.
 *
 * We deliberately avoid a hard dependency on com.google.android.material so
 * the debug tool stays lightweight; the system [Dialog] is enough.
 */
internal fun DialogFragment.applyHalfSheetWindow(heightFraction: Float = 0.66f) {
    val window = dialog?.window ?: return
    val ctx = requireContext()
    val metrics = ctx.resources.displayMetrics
    val height = (metrics.heightPixels * heightFraction).roundToInt()

    val bg =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.TRANSPARENT)
            cornerRadius = 0f
        }
    window.setBackgroundDrawable(bg)

    val params = window.attributes
    params.width = ViewGroup.LayoutParams.MATCH_PARENT
    params.height = height
    params.gravity = Gravity.BOTTOM
    params.dimAmount = 0.4f
    window.attributes = params
    window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
}
