// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol

import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.api.BusinessCallHandler
import com.tiktok.sparkling.method.registry.api.DefaultCallHandler
import com.tiktok.sparkling.method.registry.api.SparklingBridge
import com.lynx.tasm.LynxBackgroundRuntime
import com.lynx.tasm.LynxView
import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.protocol.handler.BridgeDispatcher
import com.tiktok.sparkling.method.protocol.impl.errors.JSBErrorReportModel
import com.tiktok.sparkling.method.protocol.impl.interceptor.BridgeMockInterceptor
import com.tiktok.sparkling.method.protocol.impl.monitor.IBridgeMonitor
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeLifeClient
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeProtocol

class BridgeContext {
    companion object {
        fun getPlatformByBridgeContext(bridgeContext: BridgeContext): BridgePlatformType =
            if (bridgeContext.platform == BridgePlatformType.LYNX) {
                BridgePlatformType.LYNX
            } else if (bridgeContext.platform == BridgePlatformType.WEB) {
                BridgePlatformType.WEB
            } else {
                BridgePlatformType.ALL
            }
    }

    var sparklingBridge: SparklingBridge? = null
    var dispatcher: BridgeDispatcher? = null
    var bridgeClient = DefaultBridgeClientImp(this)
    val bridgeLifeClientImp = DefaultBridgeLifeClientImp(this)
    var lynxView: LynxView? = null
    var lynxBackgroundRuntime: LynxBackgroundRuntime? = null
    var containerId: String? = ""
    var protocols: MutableList<IBridgeProtocol> = mutableListOf()
    var monitor: MutableSet<IBridgeMonitor> = mutableSetOf()
    val defaultCallHandler: DefaultCallHandler = DefaultCallHandler()
    var businessCallHandler: BusinessCallHandler? = null
    var jsbMockInterceptor: BridgeMockInterceptor? = null // for JSB Mock
    var platform: BridgePlatformType = BridgePlatformType.ALL

    /**
     * force on context error, only record view level message in jsbExtension
     */
    val errorReportModel: JSBErrorReportModel = JSBErrorReportModel()

    fun registerProtocol(protocol: IBridgeProtocol) {
        protocols.add(protocol)
    }

    fun getNamespace() = businessCallHandler?.nameSpace

    fun shouldHandleWithBusinessHandler(call: BridgeCall): Boolean =
        if (businessCallHandler != null && (call.nameSpace == getNamespace() || call.nameSpace.isEmpty())) {
            businessCallHandler?.getBridge(this, call.bridgeName) != null
        } else {
            false
        }

    fun registerIBridgeLifeClient(bridgeLifeClient: IBridgeLifeClient) {
        bridgeLifeClientImp.registerIBridgeLifeClient(bridgeLifeClient)
    }

    fun getCurrentUrl(): String? = this.lynxView?.templateUrl ?: this.lynxBackgroundRuntime?.lastScriptUrl
}
