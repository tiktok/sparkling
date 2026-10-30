// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol

import com.lynx.tasm.LynxBackgroundRuntime
import com.lynx.tasm.LynxView
import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.protocol.handler.BridgeDispatcher
import com.tiktok.sparkling.method.protocol.impl.interceptor.BridgeMockInterceptor
import com.tiktok.sparkling.method.protocol.impl.monitor.IBridgeMonitor
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeLifeClient
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeProtocol
import com.tiktok.sparkling.method.registry.api.BusinessCallHandler
import com.tiktok.sparkling.method.registry.api.DefaultCallHandler
import com.tiktok.sparkling.method.registry.api.SparklingBridge
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IBridgePlatformType

/**
 * The bridge state of one container: its platform, handlers, clients, protocols and observers.
 *
 * Open so that a host can keep state of its own in a subclass.
 */
open class BridgeContext {
    companion object {
        /** The platform a lookup for this container uses: Lynx, Web, or every platform otherwise. */
        fun getPlatformByBridgeContext(bridgeContext: BridgeContext): BridgePlatformType {
            return if (bridgeContext.platformType == BridgePlatformType.LYNX) {
                BridgePlatformType.LYNX
            } else if (bridgeContext.platformType == BridgePlatformType.WEB) {
                BridgePlatformType.WEB
            } else {
                BridgePlatformType.ALL
            }
        }
    }

    var platformType: IBridgePlatformType = BridgePlatformType.ALL

    /**
     * Receives this container's call and event lifecycle callbacks and passes them on.
     *
     * Open so that a host can hold a life client of its own type here; TikTok's has to keep its own
     * class because prebuilt consumers reference it through this getter.
     */
    open val bridgeLifeClientImp: IBridgeLifeClient = DefaultBridgeLifeClientImp(this)

    /** Sees each call before it runs and may refuse it; hears when its result returns. */
    var bridgeClient: DefaultBridgeClientImp = DefaultBridgeClientImp(this)

    /**
     * Runs this container's own methods and records whether the container has been released.
     *
     * Lazy so that a host overriding it with its own handler type does not also allocate this one.
     */
    open val defaultCallHandler: DefaultCallHandler by lazy { DefaultCallHandler() }

    /**
     * Runs the methods of the business namespace this container is bound to. A call it can serve
     * goes to it instead of [defaultCallHandler]; see [shouldHandleWithBusinessHandler].
     */
    var businessCallHandler: BusinessCallHandler? = null

    /** Debug hook that can rewrite or answer this container's calls; unset in production. */
    var jsbMockInterceptor: BridgeMockInterceptor? = null
    var sparklingBridge: SparklingBridge? = null
    var lynxView: LynxView? = null
    var lynxBackgroundRuntime: LynxBackgroundRuntime? = null
    var containerId: String? = ""
    var protocols: MutableList<IBridgeProtocol> = mutableListOf()
    /** Debug and host observers for this container's calls and native-to-JS events. */
    val bridgeMonitors: MutableSet<IBridgeMonitor> = mutableSetOf()
    /** Dispatches JSB calls for this container. The runtime instance may be a host subclass. */
    var dispatcher: BridgeDispatcher? = null

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

    /**
     * Registers [bridgeLifeClient] with the default life client. A host that holds a life client of
     * its own type in [bridgeLifeClientImp] registers through that client instead.
     */
    fun registerIBridgeLifeClient(bridgeLifeClient: IBridgeLifeClient) {
        (bridgeLifeClientImp as? DefaultBridgeLifeClientImp)?.registerIBridgeLifeClient(bridgeLifeClient)
    }

    open fun getCurrentUrl(): String? =
        lynxView?.templateUrl ?: lynxBackgroundRuntime?.lastScriptUrl
}
