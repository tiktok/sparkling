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
     * Schemes that name a web address rather than a container. A URL on one of
     * these is never a container scheme, however its host reads.
     */
    private val WEB_SCHEMES = setOf("http", "https")

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

    /**
     * Parse a container scheme.
     *
     * What this is, is decided by the host - `lynxview_page`, `lynxview_card`,
     * `lynxview`, `webview` - and an unrecognised host is rejected below. The
     * scheme therefore only has to say that this is a container URL and not a
     * web address, so any scheme but http(s) is accepted rather than `hybrid://`
     * alone.
     *
     * That is what makes deep links work without a custom parser. An app's
     * public scheme is its own - `myapp://` - because `hybrid://` is generic and
     * every Sparkling app on the device would claim it; requiring `hybrid://`
     * here meant a URL arriving from outside the app could never reach the
     * router as it stood. iOS has always decided on host and query alone
     * (SPKHybridSchemeParam.canResolve), so this also removes a case where the
     * same URL behaved differently on the two platforms.
     */
    @JvmStatic
    fun parseDefaultScheme(scheme: String): HybridSchemeParam? {
        val uri = scheme.toUri()
        val uriScheme = uri.scheme?.lowercase()
        if (uriScheme == null || uriScheme in WEB_SCHEMES) {
            return null
        }
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
        params.hideNavBar = uri.safeGetQueryParameter(SchemeConstants.Param.HIDE_NAV_BAR) == SchemeConstants.Value.ENABLED
        params.navBarColor = resolveThemedColor(uri, SchemeConstants.Param.NAV_BAR_COLOR, params.forceThemeStyle)
        params.screenOrientation = uri.safeGetQueryParameter(SchemeConstants.Param.SCREEN_ORIENTATION)
        params.hideStatusBar = uri.safeGetQueryParameter(SchemeConstants.Param.HIDE_STATUS_BAR) == SchemeConstants.Value.ENABLED
        params.transStatusBar = uri.safeGetQueryParameter(SchemeConstants.Param.TRANS_STATUS_BAR) == SchemeConstants.Value.ENABLED
        params.hideLoading = uri.safeGetQueryParameter(SchemeConstants.Param.HIDE_LOADING) == SchemeConstants.Value.ENABLED
        params.disableAutoRemoveLoading =
            uri.safeGetQueryParameter(SchemeConstants.Param.DISABLE_AUTO_REMOVE_LOADING) == SchemeConstants.Value.ENABLED
        params.loadingBgColor = resolveThemedColor(uri, SchemeConstants.Param.LOADING_BG_COLOR, params.forceThemeStyle)
        params.containerBgColor = resolveThemedColor(uri, SchemeConstants.Param.CONTAINER_BG_COLOR, params.forceThemeStyle)
        params.showNavBarInTransStatusBar = uri.safeGetQueryParameter(SchemeConstants.Param.SHOW_NAV_BAR_IN_TRANS_STATUS_BAR) == SchemeConstants.Value.ENABLED
        params.hideError = uri.safeGetQueryParameter(SchemeConstants.Param.HIDE_ERROR) == SchemeConstants.Value.ENABLED

        return params
    }
}
