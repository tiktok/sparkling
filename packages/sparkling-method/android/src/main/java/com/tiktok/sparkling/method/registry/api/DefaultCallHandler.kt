// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.api

import com.lynx.react.bridge.ReadableMap
import com.tiktok.sparkling.method.protocol.BridgeContext
import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeHandler
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeMethodCallback
import com.tiktok.sparkling.method.registry.core.IBridgeContext
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.api.util.BridgeMethodCallbackHelper
import org.json.JSONObject

class DefaultCallHandler : IBridgeHandler {
    private var context: IBridgeContext? = null
    private var pool: BridgeLocalPool = BridgeLocalPool()
    private var isReleased = false

    override fun handle(
        bridgeContext: BridgeContext,
        call: BridgeCall,
        callback: IBridgeMethodCallback,
    ) {
        val platform = BridgeContext.getPlatformByBridgeContext(bridgeContext)
        val bridge =
            pool.getBridge(call.bridgeName, platform)
                ?: return BridgeMethodCallbackHelper.bridgeNotFound(callback)

        val params = call.params

        context?.let {
            bridge.setBridgeContext(it)
        }

        if (!bridge.compatibility.value) {
            when (params) {
                is JSONObject -> {
                    JsonProcessor(bridge, params, call.platform).apply { this@apply.context = this@DefaultCallHandler.context }
                }

                is ReadableMap -> {
                    ReadableMapProcessor(bridge, unWrapperParams(params) ?: params).apply { this@apply.context = this@DefaultCallHandler.context }
                }

                else -> {
                    return
                }
            }.handle(callback)
        } else { // compatible bridge
            val idlCallback =
                object : IDLBridgeMethod.Callback {
                    override fun invoke(data: Map<String, Any?>) {
                        callback.onBridgeResult(data)
                    }
                }
            when (params) {
                is JSONObject -> {
                    bridge.realHandle(Utils.jsonToMap(params), idlCallback, BridgePlatformType.ALL)
                }

                is ReadableMap -> {
                    bridge.realHandle(
                        (unWrapperParams(params) ?: params).toHashMap(),
                        idlCallback,
                        BridgePlatformType.ALL,
                    )
                }

                else -> {
                    bridge.realHandle(emptyMap(), idlCallback, BridgePlatformType.ALL)
                }
            }
        }
    }

    fun registerLocalIDLMethod(
        clazz: Class<out IDLBridgeMethod>?,
        scope: BridgePlatformType = BridgePlatformType.ALL,
    ) {
        pool.registerLocalIDLMethod(clazz, scope)
    }

    fun registerLocalIDLMethod(
        name: String,
        scope: BridgePlatformType = BridgePlatformType.ALL,
        clazz: Class<out IDLBridgeMethod>? = null,
        factory: () -> IDLBridgeMethod,
    ) {
        pool.registerLocalIDLMethod(name, scope, clazz, factory)
    }

    fun setBridgeContext(context: IBridgeContext) {
        this.context = context
        this.pool.setBridgeContext(context)
    }

    fun getBridge(
        bridgeContext: BridgeContext,
        bridgeName: String,
    ): IDLBridgeMethod? {
        val platform = BridgeContext.getPlatformByBridgeContext(bridgeContext)
        return pool.getBridge(bridgeName, platform)
    }

    override fun onRelease() {
        pool.release()
        isReleased = true
    }

    override fun isReleased(): Boolean = isReleased

    private fun unWrapperParams(params: ReadableMap?): ReadableMap? = params?.getMap("data") ?: params
}
