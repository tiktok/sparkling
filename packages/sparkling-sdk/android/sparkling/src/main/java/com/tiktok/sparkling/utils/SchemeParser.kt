// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.utils

import android.content.res.Configuration
import android.net.Uri
import androidx.core.net.toUri
import com.tiktok.sparkling.hybridkit.base.HybridContainerType
import com.tiktok.sparkling.hybridkit.base.HybridKitType
import com.tiktok.sparkling.hybridkit.scheme.HybridSchemeParam
import com.tiktok.sparkling.hybridkit.scheme.SchemeConstants
import com.tiktok.sparkling.hybridkit.utils.ColorUtil
import com.tiktok.sparkling.hybridkit.utils.safeGetQueryParameter

object SchemeParser {
    /**
     * Spellings a scheme flag is allowed to switch on with.
     *
     * The scheme is a URL written by hand, in a deep link, or by a JS caller
     * that has a boolean and stringifies it. Accepting only "1" meant
     * `hide_nav_bar=true` read as false and the container's own navigation bar
     * stayed above the page - a silent, and quite common, layout bug.
     */
    private val ENABLED_VALUES = setOf("1", "true", "yes", "on")

    private fun Uri.flag(name: String): Boolean =
        safeGetQueryParameter(name)?.trim()?.lowercase() in ENABLED_VALUES

    fun interface CustomSchemeParser {
        fun parseScheme(scheme: String): HybridSchemeParam?
    }

    @Volatile
    private var customSchemeParser: CustomSchemeParser? = null

    @JvmStatic
    fun setCustomSchemeParser(parser: CustomSchemeParser?) {
        customSchemeParser = parser
    }

    fun parseScheme(scheme: String): HybridSchemeParam? {
        customSchemeParser?.parseScheme(scheme)?.let { return it }
        return parseDefaultScheme(scheme)
    }

    /**
     * Resolves a themed color from the URI, following the same logic as
     * Spark's SparkColor and Sparkling iOS's themedColor(withDict:forKey:context:).
     *
     * Priority: force_theme_style > system theme > base key.
     * For a given [key], looks up [key]_light / [key]_dark based on the
     * resolved theme, falling back to the base [key] value.
     */
    private fun resolveThemedColor(
        uri: Uri,
        key: String,
        forceThemeStyle: String?,
    ): String? {
        val baseValue = uri.safeGetQueryParameter(key)
        val lightValue = uri.safeGetQueryParameter("${key}_light")
        val darkValue = uri.safeGetQueryParameter("${key}_dark")

        if (lightValue == null && darkValue == null) {
            return baseValue
        }

        val isDark =
            when (forceThemeStyle?.lowercase()) {
                "dark" -> {
                    true
                }

                "light" -> {
                    false
                }

                else -> {
                    val nightMode =
                        runCatching {
                            ColorUtil.appContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                        }.getOrDefault(Configuration.UI_MODE_NIGHT_NO)
                    nightMode == Configuration.UI_MODE_NIGHT_YES
                }
            }

        return if (isDark) {
            darkValue ?: lightValue ?: baseValue
        } else {
            lightValue ?: darkValue ?: baseValue
        }
    }

    @JvmStatic
    fun parseDefaultScheme(scheme: String): HybridSchemeParam? {
        if (!scheme.startsWith(SchemeConstants.Scheme.PREFIX)) {
            return null
        }

        val uri = scheme.toUri()
        val viewTypeString = uri.host?.lowercase()
        val engineType =
            when {
                viewTypeString == SchemeConstants.Host.WEB_VIEW -> HybridKitType.WEB
                viewTypeString?.startsWith(SchemeConstants.Host.LYNX_VIEW) == true -> HybridKitType.LYNX
                else -> HybridKitType.UNKNOWN
            }

        val containerType =
            when (viewTypeString) {
                SchemeConstants.Host.LYNX_VIEW_CARD -> HybridContainerType.CARD

                SchemeConstants.Host.LYNX_VIEW_PAGE,
                SchemeConstants.Host.LYNX_VIEW,
                -> HybridContainerType.PAGE

                else -> HybridContainerType.UNKNOWN
            }

        if (engineType == HybridKitType.UNKNOWN) {
            return null
        }

        val params = HybridSchemeParam()
        params.engineType = engineType
        params.containerType = containerType
        params.forceThemeStyle = uri.safeGetQueryParameter(SchemeConstants.Param.FORCE_THEME_STYLE)

        params.bundle = uri.safeGetQueryParameter(SchemeConstants.Param.BUNDLE)
            ?: uri.safeGetQueryParameter(SchemeConstants.Param.URL)
        params.title = uri.safeGetQueryParameter(SchemeConstants.Param.TITLE)
//        params.fallbackUrl = uri.safeGetQueryParameter(SchemeConstants.Param.FALLBACK_URL)
        params.titleColor = resolveThemedColor(uri, SchemeConstants.Param.TITLE_COLOR, params.forceThemeStyle)
        params.hideNavBar = uri.flag(SchemeConstants.Param.HIDE_NAV_BAR)
        params.navBarColor = resolveThemedColor(uri, SchemeConstants.Param.NAV_BAR_COLOR, params.forceThemeStyle)
        params.screenOrientation = uri.safeGetQueryParameter(SchemeConstants.Param.SCREEN_ORIENTATION)
        params.hideStatusBar = uri.flag(SchemeConstants.Param.HIDE_STATUS_BAR)
        params.transStatusBar = uri.flag(SchemeConstants.Param.TRANS_STATUS_BAR)
        params.hideLoading = uri.flag(SchemeConstants.Param.HIDE_LOADING)
        params.disableAutoRemoveLoading = uri.flag(SchemeConstants.Param.DISABLE_AUTO_REMOVE_LOADING)
        params.loadingBgColor = resolveThemedColor(uri, SchemeConstants.Param.LOADING_BG_COLOR, params.forceThemeStyle)
        params.containerBgColor = resolveThemedColor(uri, SchemeConstants.Param.CONTAINER_BG_COLOR, params.forceThemeStyle)
        params.showNavBarInTransStatusBar = uri.flag(SchemeConstants.Param.SHOW_NAV_BAR_IN_TRANS_STATUS_BAR)
        params.hideError = uri.flag(SchemeConstants.Param.HIDE_ERROR)

        return params
    }
}
