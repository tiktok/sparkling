// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.hybridkit.utils

import android.content.Context
import android.graphics.Color
import androidx.core.content.ContextCompat

object ColorUtil {
    lateinit var appContext: Context

    fun rgbaToArgb(rgbaColor: String): String {
        try {
            if (rgbaColor.isEmpty()) {
                throw IllegalArgumentException("Empty color string")
            }

            var dstColor = rgbaColor
            if (rgbaColor.length != 8 && rgbaColor.length != 9) {
                return if (rgbaColor.length == 6) "#$dstColor" else dstColor
            }

            var hexColor = rgbaColor
            if (rgbaColor.startsWith("#")) {
                hexColor = rgbaColor.drop(1)
            }
            dstColor = hexColor.takeLast(2) + hexColor.dropLast(2)

            return "#$dstColor"
        } catch (t: Throwable) {
            return "#00000000"
        }
    }

    fun parseColorSafely(color: String): Int =
        try {
            Color.parseColor(color)
        } catch (t: Throwable) {
            Color.TRANSPARENT
        }

    @JvmStatic
    @JvmName("getColor")
    internal fun getColor(resId: Int): Int = ContextCompat.getColor(appContext, resId)
}
