// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import android.content.Context
import android.content.SharedPreferences
import com.tiktok.sparkling.hybridkit.KitViewManager
import com.tiktok.sparkling.hybridkit.utils.ColorUtil
import org.json.JSONObject

/**
 * The app's own light/dark preference, shared by every container.
 *
 * Each page is its own container with its own runtime, and a container's theme
 * used to be decided once, at creation, from `force_theme_style` or the system
 * setting. So an in-app preference did not reach a page opened afterwards, and
 * changing it did not reach a page that was already open: the setting applied to
 * the screen you changed it on and nothing else.
 *
 * This is the one place that answers "which theme". A container that was not
 * given an explicit `force_theme_style` follows it, a container that was keeps
 * what it was told, and changing it tells every live container so they can
 * repaint without being recreated.
 */
object SparklingTheme {
    /** The event every live container receives when the preference changes. */
    const val EVENT = "themeChanged"

    private const val PREFERENCES = "sparkling-theme"
    private const val KEY = "preference"

    enum class Preference {
        /** Follow the device. */
        SYSTEM,
        LIGHT,
        DARK,
        ;

        /** The `force_theme_style` spelling, or null for "do not force". */
        fun style(): String? =
            when (this) {
                SYSTEM -> null
                LIGHT -> "light"
                DARK -> "dark"
            }

        companion object {
            fun parse(value: String?): Preference =
                when (value?.lowercase()) {
                    "light" -> LIGHT
                    "dark" -> DARK
                    else -> SYSTEM
                }
        }
    }

    @Volatile
    private var cached: Preference? = null

    /**
     * Whether the preference outlives the process.
     *
     * On by default, because a theme the user picked and that resets on the next
     * launch is a bug they will report. A host that stores it somewhere of its
     * own turns this off and sets [preference] during startup.
     */
    @Volatile
    var persist: Boolean = true

    private fun preferences(): SharedPreferences? =
        runCatching {
            ColorUtil.appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        }.getOrNull()

    var preference: Preference
        get() {
            cached?.let { return it }
            val stored =
                if (persist) {
                    Preference.parse(preferences()?.getString(KEY, null))
                } else {
                    Preference.SYSTEM
                }
            cached = stored
            return stored
        }
        set(value) {
            if (cached == value) {
                return
            }
            cached = value
            if (persist) {
                preferences()?.edit()?.putString(KEY, value.name.lowercase())?.apply()
            }
            broadcast(value)
        }

    /**
     * What a container should be forced to, or null to let it decide.
     *
     * Read when a scheme is parsed, so a page opened while the preference is
     * Dark is dark from its first frame rather than repainting after it loads.
     */
    @JvmStatic
    fun forcedStyle(): String? = preference.style()

    /**
     * Tell every live container. A page that does not listen is unaffected.
     *
     * Sent as JSON because that is the only form a Lynx page receives:
     * `sendEventByMap` says so on itself.
     */
    private fun broadcast(value: Preference) {
        val payload = JSONObject().put("theme", value.style() ?: "system")
        KitViewManager.getKitViews().forEach { (_, view) ->
            runCatching { view.sendEventByJSON(EVENT, payload) }
        }
    }

    /** Test seam; not part of the public surface. */
    internal fun reset() {
        cached = null
        persist = true
    }
}
