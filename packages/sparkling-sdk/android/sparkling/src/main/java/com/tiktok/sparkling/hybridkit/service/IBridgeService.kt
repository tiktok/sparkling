// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.hybridkit.service

import android.content.Context
import android.content.pm.PackageInstaller.SessionInfo
import android.util.Log
import com.tiktok.sparkling.hybridkit.base.IKitView
import com.tiktok.sparkling.hybridkit.service.api.IService
import org.json.JSONObject
import java.util.concurrent.ExecutorService

interface IBridgeService : IService {
    fun createBridgeService(): IKitBridgeService?
}

interface IBridgeRegistry {
    fun onRegisterBridge(
        kitView: IKitView,
        context: Context,
        sessionInfo: SessionInfo?,
    )

    fun onUnRegisterBridge(kitView: IKitView) {}
}

interface IKitBridgeService :
    IBridgeStatusObserver,
    IWebViewStatusListener,
    IBridgeRefresher {
    fun setBridgeRegister(registry: IBridgeRegistry)

    /**
     * config the custom executor when bridge need run in background thread
     */
    fun setBridgeRunInBackGroundExecutor(customExecutors: ExecutorService)

    fun sendEvent(
        name: String,
        params: JSONObject?,
    )

    fun sendEvent(
        name: String,
        params: Map<String, Any?>?,
    )
}

interface IBridgeStatusObserver {
    /**
     * instance created. (e.g. WebView instance created)
     */
    fun onKitViewCreated(
        context: Context,
        kitView: IKitView,
        sessionInfo: SessionInfo?,
    )

    /**
     * container created. (e.g. WebView instance created + WebX container created)
     */
    fun onKitViewProvided(
        context: Context,
        kitView: IKitView,
        sessionInfo: SessionInfo?,
    ) {}

    fun onLynxViewPreInit(
        context: Context,
        builder: Any?,
        containerId: String?,
    )
}

interface IBridgeRefresher {
    fun onContextRefreshed(context: Context) {
        Log.e("IBridgeRefresher", "start refresh Context, context = $context")
    }
}

interface IWebViewStatusListener {
    fun onPageStart(url: String)

    fun shouldOverrideUrlLoading(url: String?): Boolean

    fun onDestroy()

    fun onLoadResource(url: String)
}
