// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.hybridkit.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.os.LocaleList
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import java.util.Locale
import kotlin.math.roundToInt

object DevicesUtil {
    const val TAG = "DevicesUtil"
    val brand: String
        get() = Build.BRAND

    val model: String
        get() = Build.MODEL

    private fun getDisplay(context: Context): Display? {
        val wm: WindowManager?
        wm =
            if (context is Activity) {
                context.windowManager
            } else {
                context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            }
        return runCatching { wm?.defaultDisplay }.getOrNull()
    }

    private var screenHeight = 0

    fun getScreenHeight(
        context: Context,
        cache: Boolean,
    ): Int {
        if (!cache || screenHeight == 0) {
            screenHeight = getScreenHeight(context)
        }
        return screenHeight
    }

    // The width and height of wide and vertical screens are different, do not cache this value.
    fun getScreenHeight(context: Context): Int =
        try {
            if ((getScreenSize(context)?.get(1) ?: 0) > 0) {
                getScreenSize(context)?.get(1)
                    ?: 0
            } else {
                0
            }
        } catch (t: Throwable) {
            0
        }

    private var screenWidth = 0

    fun getScreenWidth(
        context: Context,
        cache: Boolean,
    ): Int {
        if (!cache || screenWidth == 0) {
            screenWidth = getScreenWidth(context)
        }
        return screenWidth
    }

    // The width and height of wide and vertical screens are different, do not cache this value.
    fun getScreenWidth(context: Context): Int =
        try {
            if ((getScreenSize(context)?.get(0) ?: 0) > 0) {
                getScreenSize(context)?.get(0)
                    ?: 0
            } else {
                0
            }
        } catch (t: Throwable) {
            0
        }

    fun getScreenRotation(context: Context): Int {
        val wm =
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return wm.defaultDisplay.rotation
    }

    fun isScreenPortrait(context: Context): Boolean {
        val rotation = getScreenRotation(context)
        return rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180
    }

    fun getPixelRadio(context: Context): Float = context.resources.displayMetrics.density

    val isHuawei: Boolean
        get() = Build.MANUFACTURER != null && Build.MANUFACTURER.contains("HUAWEI")

    private var sStatusBarHeight = 0

    fun getStatusBarHeight(context: Context): Int {
        if (sStatusBarHeight > 0) {
            return sStatusBarHeight
        }
        var result = 0
        val resourceId =
            context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        if (result == 0) {
            result = UIUtils.dip2Px(context, 25f).toInt()
        }
        sStatusBarHeight = result
        return result
    }

    @Deprecated(
        "This method returns int value, low precision.",
        ReplaceWith("Activity.safeAreaHeight()"),
    )
    fun safeAreaHeight(
        statusBarHeight_: Int,
        context: Activity,
    ): Int =
        try {
            val contentRect = Rect()
            context.window.decorView.getWindowVisibleDisplayFrame(contentRect)
            if (px2dp(contentRect.top.toDouble(), context) >= statusBarHeight_) {
                px2dp(contentRect.height().toDouble(), context)
            } else {
                px2dp(contentRect.height().toDouble(), context) - statusBarHeight_
            }
        } catch (t: Throwable) {
            Log.e(TAG, t.message ?: t.toString())
            context.resources.displayMetrics.heightPixels
        }

    fun px2dp(
        px: Double,
        context: Context,
    ): Int {
        val scale =
            context.resources.displayMetrics.density
                .toDouble()
        return (px / scale + 0.5f).toInt()
    }

    val Number.dpFloat
        get() =
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                toFloat(),
                Resources.getSystem().displayMetrics,
            )

    inline val Number.dp
        get() = dpFloat.roundToInt()

    val system: String
        get() = Build.VERSION.RELEASE

    val platform: String
        get() = "android"

    val language: String
        get() {
            val locale: Locale
            locale =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    LocaleList.getDefault()[0]
                } else {
                    Locale.getDefault()
                }
            return locale.language + "-" + locale.country
        }

    fun isLowPowerMode(context: Context): Boolean {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            return false
        }
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isPowerSaveMode ?: false
        } catch (t: Throwable) {
            false
        }
    }

    fun getScreenSize(context: Context?): IntArray {
        return if (context == null) {
            intArrayOf(-1, -1)
        } else {
            try {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                if (wm != null) {
                    val display = wm.defaultDisplay
                    val size = Point()
                    if (display == null) {
                        return intArrayOf(-1, -1)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        display.getRealSize(size)
                    } else {
                        display.getSize(size)
                    }
                    intArrayOf(size.x, size.y)
                } else {
                    val dm = context.resources.displayMetrics
                    intArrayOf(dm.widthPixels, dm.heightPixels)
                }
            } catch (e: Exception) {
                Log.e(TAG, e.message ?: e.toString())
                intArrayOf(-1, -1)
            }
        }
    }

    fun isTalkBackEnabled(context: Context): Boolean =
        try {
            Settings.Secure.getInt(context.contentResolver, "touch_exploration_enabled", 0) != 0
        } catch (throwable: Throwable) {
            Log.e(TAG, throwable.message ?: "get talk back status failed")
            false
        }

    fun isPad(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.DONUT) {
            context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE
        } else {
            false
        }

    /**
     * Get the navigation bar height in pixels.
     *
     * @param context Context to access resources
     * @return Navigation bar height in pixels, or 0 if not found
     */
    fun getNavigationBarHeight(context: Context): Int {
        val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            context.resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }
}
