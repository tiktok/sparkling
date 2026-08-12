// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.hybridkit.theme

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.tiktok.sparkling.hybridkit.KitViewManager
import com.tiktok.sparkling.hybridkit.utils.GlobalPropsUtils

enum class ThemePreference(
    val value: String,
) {
    FOLLOW_SYSTEM("follow-system"),
    LIGHT("light"),
    DARK("dark"),
    ;

    companion object {
        const val FOLLOW_SYSTEM_VALUE = "follow-system"
        const val LIGHT_VALUE = "light"
        const val DARK_VALUE = "dark"

        fun fromValue(value: String?): ThemePreference? =
            values().firstOrNull {
                it.value == value?.trim()?.lowercase()
            }
    }
}

object ThemePreferenceManager {
    const val GLOBAL_PROPS_KEY = "preferredTheme"

    internal const val PREFERENCES_NAME = "com.tiktok.sparkling.theme"
    internal const val PREFERENCE_KEY = "theme_preference"

    @JvmStatic
    fun getPreference(context: Context): ThemePreference {
        val value =
            context.applicationContext
                .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getString(PREFERENCE_KEY, null)
        return ThemePreference.fromValue(value) ?: ThemePreference.FOLLOW_SYSTEM
    }

    @JvmStatic
    fun setPreference(
        context: Context,
        preference: ThemePreference,
    ) {
        context.applicationContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREFERENCE_KEY, preference.value)
            .apply()

        val update =
            Runnable {
                val globalProps = mapOf(GLOBAL_PROPS_KEY to preference.value)
                KitViewManager.getKitViews().forEach { (containerId, kitView) ->
                    GlobalPropsUtils.instance.setUnstableProps(containerId, globalProps)
                    runCatching {
                        kitView.updateGlobalPropsByIncrement(globalProps)
                    }
                }
            }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            update.run()
        } else {
            Handler(Looper.getMainLooper()).post(update)
        }
    }
}
