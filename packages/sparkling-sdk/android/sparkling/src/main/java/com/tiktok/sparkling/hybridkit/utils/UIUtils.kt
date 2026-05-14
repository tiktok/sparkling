// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.hybridkit.utils

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.tiktok.sparkling.hybridkit.HybridEnvironment

/**
 * @date 2021/2/20  13: 48
 */
object UIUtils {
    fun dip2Px(
        context: Context,
        dipValue: Float,
    ): Float {
        val scale = context.resources.displayMetrics.density
        return dipValue * scale + 0.5f
    }

    fun px2dip(
        context: Context,
        pxValue: Float,
    ): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    fun dip2Px(dipValue: Float): Float = dip2Px(HybridEnvironment.instance.context, dipValue)

    fun px2dip(pxValue: Float): Int = px2dip(HybridEnvironment.instance.context, pxValue)

    fun getScreenHeight(context: Context?): Int {
        if (context == null) {
            return 0
        }
        val dm = context.resources.displayMetrics
        return dm?.heightPixels ?: 0
    }

    fun removeParentView(view: View?) {
        if (view?.parent != null && view.parent is ViewGroup) {
            (view.parent as ViewGroup).removeView(view)
        }
    }
}
