// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol

import android.content.Context
import android.view.View
import android.webkit.WebView
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.api.SparklingBridge
import com.lynx.tasm.LynxBackgroundRuntime
import com.lynx.tasm.LynxBackgroundRuntimeOptions
import com.lynx.tasm.LynxView
import com.lynx.tasm.LynxViewBuilder
import com.tiktok.sparkling.method.protocol.handler.BridgeDispatcher
import com.tiktok.sparkling.method.protocol.impl.lynx.LynxBridgeProtocol
import com.tiktok.sparkling.method.protocol.impl.lynx.LynxViewImpl
import com.tiktok.sparkling.method.protocol.impl.monitor.BridgeSDKMonitor
import com.tiktok.sparkling.method.protocol.impl.monitor.IBridgeMonitor
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeHandler
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeLifeClient
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

internal class InnerBridge {
    private var mBridgeContext = BridgeContext()

    companion object {
        private val hasInitMonitor: AtomicBoolean = AtomicBoolean(false)
        var globalMonitor: BridgeSDKMonitor? = null

        fun initSDKMonitor(
            context: Context,
            appInfo: BridgeSDKMonitor.APPInfo4Monitor,
        ) {
            if (hasInitMonitor.compareAndSet(false, true)) {
                globalMonitor = BridgeSDKMonitor(context, appInfo)
            }
        }
    }

    init {
        mBridgeContext.dispatcher = BridgeDispatcher()
    }

    /**
     * use this to pass the bridge to mBridgeContext, and you can use the custom auth ability.
     */
    fun init(
        view: View,
        containerId: String?,
        jsBridgeProtocols: Int? = null,
        bridge: SparklingBridge,
    ) {
        mBridgeContext.sparklingBridge = bridge
        init(view, containerId, jsBridgeProtocols)
    }

    fun init(
        view: View,
        containerId: String?,
        jsBridgeProtocols: Int? = null,
    ) {
        if (view is LynxView) {
            mBridgeContext.lynxView = view
            mBridgeContext.platform = BridgePlatformType.LYNX
            mBridgeContext.registerProtocol(LynxBridgeProtocol(mBridgeContext))
        } else if (view is WebView) {
            // TODO
        }
        mBridgeContext.protocols.forEach {
            it.init()
        }
        mBridgeContext.containerId = containerId
    }

    fun initLynxJSRuntime(
        containerId: String,
        options: LynxBackgroundRuntimeOptions,
        context: Context,
        sparklingBridge: SparklingBridge,
    ) {
        mBridgeContext.sparklingBridge = sparklingBridge
        mBridgeContext.platform = BridgePlatformType.LYNX
        mBridgeContext.registerProtocol(LynxBridgeProtocol(mBridgeContext))
        mBridgeContext.protocols.forEach {
            it.init()
        }
        registerLynxJSRuntimeModule(options, containerId)
    }

    private fun registerLynxJSRuntimeModule(
        options: LynxBackgroundRuntimeOptions,
        containerId: String?,
    ) {
        LynxViewImpl(mBridgeContext).initJSRuntime(options)
        mBridgeContext.containerId = containerId
    }

    fun registerIBridgeLifeClient(bridgeLifeClient: IBridgeLifeClient) {
        mBridgeContext.registerIBridgeLifeClient(bridgeLifeClient)
    }

    fun registerLynxModule(
        builder: LynxViewBuilder,
        containerId: String?,
    ) {
        LynxViewImpl(mBridgeContext).apply {
            init(builder)
        }
        mBridgeContext.containerId = containerId
    }

    fun registerHandler(handler: IBridgeHandler) {
        mBridgeContext.dispatcher?.registerHandler(handler)
    }

    fun getBridgeContext(): BridgeContext = mBridgeContext

    fun registerMonitor(monitor: IBridgeMonitor) {
        mBridgeContext.monitor.add(monitor)
    }

    fun sendEvent(
        event: String,
        data: JSONObject?,
    ) {
        mBridgeContext.monitor.forEach {
            runCatching {
                it.onBridgeEvent(event, data)
            }.onFailure {
                it.printStackTrace()
            }
        }
        mBridgeContext.protocols.forEach {
            it.sendEvent(event, data)
        }
    }

    fun sendJSRuntimeEvent(
        event: String,
        data: JSONObject?,
    ) {
        mBridgeContext.monitor.forEach {
            runCatching {
                it.onBridgeEvent(event, data)
            }.onFailure {
                it.printStackTrace()
            }
        }
        mBridgeContext.protocols.forEach {
            it.sendJSRuntimeEvent(event, data)
        }
    }

    fun bindLynxJSRuntime(lynxBackgroundRuntime: LynxBackgroundRuntime) {
        mBridgeContext.lynxBackgroundRuntime = lynxBackgroundRuntime
    }
}
