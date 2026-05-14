// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.config

import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import com.tiktok.sparkling.hybridkit.HybridContext
import com.tiktok.sparkling.hybridkit.service.IKitBridgeService
import com.tiktok.sparkling.hybridkit.utils.HybridLogger

open class SparklingHybridConfig private constructor(
    val baseInfoConfig: BaseInfoConfig,
    val lynxConfig: ILynxConfig?,
    val webConfig: IWebConfig?,
    val bridgeConfig: IBridgeConfig?,
    val logConfig: LogConfig?,
    val debugConfig: DebugConfig?,
) {
    companion object {
        inline fun build(
            baseInfoConfig: BaseInfoConfig,
            block: Builder.() -> Unit,
        ) = Builder(baseInfoConfig).apply(block).build()
    }

    class Builder(
        private val baseInfoConfig: BaseInfoConfig,
    ) {
        private var lynxConfig: ILynxConfig? = null
        private var webConfig: IWebConfig? = null
        private var bridgeConfig: IBridgeConfig? = null
        private var logConfig: LogConfig? = null
        private var debugConfig: DebugConfig? = null

        fun setDebugConfig(debugConfig: DebugConfig) {
            this.debugConfig = debugConfig
        }

        fun setLynxConfig(lynxConfig: ILynxConfig?) {
            this.lynxConfig = lynxConfig
        }

        fun setWebConfig(webConfig: IWebConfig?) {
            this.webConfig = webConfig
        }

        fun setBridgeConfig(bridgeConfig: IBridgeConfig?) {
            this.bridgeConfig = bridgeConfig
        }

        fun setLogConfig(logConfig: LogConfig?) {
            this.logConfig = logConfig
        }

        fun build() = SparklingHybridConfig(baseInfoConfig, lynxConfig, webConfig, bridgeConfig, logConfig, debugConfig)
    }
}

/**
 * Global BaseInfo for HybridKit
 */
open class BaseInfoConfig(
    val isDebug: Boolean,
) : RuntimeInfo() {
    /**
     * Set global settings, the business side can set the settings required in SparkContext/HybridContext.
     */
    open fun applyGlobalSettings(
        settings: WebSettings,
        webView: WebView,
    ) {}

    /**
     * Set global jump logic, the business side can add its own logic in SparkContext/HybridContext, pay attention to the processing of super.
     */
    open fun applyCommonShouldOverrideUrl(
        view: WebView?,
        url: String?,
    ): Boolean = false

    /**
     * Set global pre-operation on url when lynx and web page loadUrl.
     * The business side can add operations to the url in SparkContext/HybridContext and return the expected url.
     */
    open fun applyGlobalLoadUrl(url: String): String = url

    /**
     * Set global pre-operation on url when web page loadUrl and shouldOverrideUrlLoading.
     * The business side can append app common params to web url in SparkContext/HybridContext and return the expected url.
     */
    open fun applyAppendCommonParamsUrl(url: String): String = url

    open fun getStableProps(context: Context): HashMap<String, Any>? = null

    open fun getUnstableProps(
        context: Context,
        hybridContext: HybridContext,
    ): HashMap<String, Any>? = null
}

interface ILynxConfig

interface IBridgeConfig {
    fun createBridgeService(): IKitBridgeService
}

interface IWebConfig

class LogConfig(
    val logger: HybridLogger,
)

class DebugConfig(
    var showErrorWhenJSBFailed: Boolean = false,
)
