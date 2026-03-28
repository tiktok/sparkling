// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.example.sparkling.go

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.tiktok.sparkling.Sparkling
import com.tiktok.sparkling.SparklingContext
import com.tiktok.sparkling.SparklingUIProvider
import com.tiktok.sparkling.debugtool.SparklingDebugTool

private const val DEFAULT_MAIN_DEV_BUNDLE_URL = "http://10.0.2.2:5969/main.lynx.bundle"

object DebugDevUrlSupport {
    fun currentMainBundleUrl(context: Context): String {
        return SparklingDebugTool.getDevUrl(context, DEFAULT_MAIN_DEV_BUNDLE_URL)
    }

    fun buildMainPageScheme(context: Context): String {
        val url = currentMainBundleUrl(context)
        return buildMainPageSchemeWithUrl(url)
    }

    fun buildMainPageSchemeWithUrl(url: String): String {
        val encoded = Uri.encode(url.trim())
        return "hybrid://lynxview_page?url=$encoded&hide_nav_bar=1&screen_orientation=portrait"
    }

    fun networkBundleUrlFromScheme(scheme: String?): String? {
        if (scheme.isNullOrBlank()) {
            return null
        }
        val parsed = runCatching { Uri.parse(scheme) }.getOrNull() ?: return null
        val url = parsed.getQueryParameter("url")?.trim()
        if (url.isNullOrEmpty()) {
            return null
        }
        return if (url.startsWith("http://") || url.startsWith("https://")) url else null
    }
}

class DebugSparklingUiProvider(
    private val initialDataJson: String,
    private val currentScheme: String,
) : SparklingUIProvider {
    override fun getLoadingView(context: Context): View {
        return ProgressBar(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER,
            )
        }
    }

    override fun getErrorView(context: Context): View {
        return DebugDevUrlErrorView(context, initialDataJson, currentScheme)
    }

    override fun getToolBar(context: Context): Toolbar? = null
}

private class DebugDevUrlErrorView(
    context: Context,
    private val initialDataJson: String,
    private val currentScheme: String,
) : FrameLayout(context) {
    private var prompted = false

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        val textView = TextView(context).apply {
            text = "Failed to load remote bundle. Please update Dev URL."
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }
        addView(
            textView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            maybePromptDevUrl()
        }
    }

    private fun maybePromptDevUrl() {
        if (prompted) {
            return
        }
        val activity = context as? Activity ?: return
        val currentUrl = DebugDevUrlSupport.networkBundleUrlFromScheme(currentScheme) ?: return
        prompted = true

        SparklingDebugTool.showDevUrlDialog(activity, currentUrl) { updatedUrl ->
            val nextContext = SparklingContext().apply {
                scheme = DebugDevUrlSupport.buildMainPageSchemeWithUrl(updatedUrl)
                withInitData(initialDataJson)
                sparklingUIProvider = DebugSparklingUiProvider(initialDataJson, scheme ?: "")
            }
            Sparkling.build(activity, nextContext).navigate()
            activity.finish()
        }
    }
}
