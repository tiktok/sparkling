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
import com.tiktok.sparkling.method.protocol.impl.interceptor.BridgeMockInterceptor
import com.tiktok.sparkling.method.protocol.impl.monitor.IBridgeMonitor
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeLifeClient
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeProtocol

open class BridgeContext {
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

    /** Sees each call before it runs and may refuse it; hears when its result returns. */
    var bridgeClient: DefaultBridgeClientImp = DefaultBridgeClientImp(this)
    val bridgeLifeClientImp = DefaultBridgeLifeClientImp(this)
    var lynxView: LynxView? = null
    var lynxBackgroundRuntime: LynxBackgroundRuntime? = null
    var containerId: String? = ""
    var protocols: MutableList<IBridgeProtocol> = mutableListOf()
    var monitor: MutableSet<IBridgeMonitor> = mutableSetOf()
    val defaultCallHandler: DefaultCallHandler = DefaultCallHandler()

    /**
     * Runs the methods of the business namespace this container is bound to. A call it can serve
     * goes to it instead of [defaultCallHandler]; see [shouldHandleWithBusinessHandler].
     */
    var businessCallHandler: BusinessCallHandler? = null

    /** Debug hook that can rewrite or answer this container's calls; unset in production. */
    var jsbMockInterceptor: BridgeMockInterceptor? = null

    var platform: BridgePlatformType = BridgePlatformType.ALL

    fun registerProtocol(protocol: IBridgeProtocol) {
        protocols.add(protocol)
    }

    fun getNamespace() = businessCallHandler?.nameSpace

    /**
     * Whether [call] goes to [businessCallHandler]: the call names the bound namespace or none, and
     * that namespace registers the method.
     *
     * Open so that a host whose handler looks methods up in its own way can answer with that lookup.
     */
    open fun shouldHandleWithBusinessHandler(call: BridgeCall): Boolean =
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
