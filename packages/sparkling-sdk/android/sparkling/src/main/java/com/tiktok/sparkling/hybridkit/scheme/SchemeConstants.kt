// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.hybridkit.scheme

object SchemeConstants {
    object Scheme {
        const val PREFIX = "hybrid://"
    }
    
    object Host {
        const val LYNX_VIEW = "lynxview"
        const val LYNX_VIEW_PAGE = "lynxview_page"
        const val LYNX_VIEW_CARD = "lynxview_card"
        const val WEB_VIEW = "webview"
    }

    object ContainerType {
        const val PAGE = "page"
        const val CARD = "card"
    }

    object Param {
        const val BUNDLE = "bundle"
        const val TITLE = "title"
        const val FALLBACK_URL = "fallback_url"
        const val TITLE_COLOR = "title_color"
        const val HIDE_NAV_BAR = "hide_nav_bar"
        const val NAV_BAR_COLOR = "nav_bar_color"
        const val SCREEN_ORIENTATION = "screen_orientation"
        const val HIDE_STATUS_BAR = "hide_status_bar"
        const val TRANS_STATUS_BAR = "trans_status_bar"
        const val HIDE_LOADING = "hide_loading"
        const val LOADING_BG_COLOR = "loading_bg_color"
        const val CONTAINER_BG_COLOR = "container_bg_color"
        const val SHOW_NAV_BAR_IN_TRANS_STATUS_BAR = "show_nav_bar_in_trans_status_bar"
        const val HIDE_ERROR = "hide_error"
        const val FORCE_THEME_STYLE = "force_theme_style"
    }
    
    object Value {
        const val ENABLED = "1"
    }
}
