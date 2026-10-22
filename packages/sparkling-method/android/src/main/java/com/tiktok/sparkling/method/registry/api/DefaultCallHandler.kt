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

/** Resolves calls through one container's pool, which host subclasses can supply. */
open class DefaultCallHandler protected constructor(
    private val pool: BridgeLocalPool,
) : IBridgeHandler {
    private var context: IBridgeContext? = null
    private var isReleased = false

    /** Creates a handler with the default container-scoped pool. */
    constructor() : this(BridgeLocalPool())

    /** Dispatches through the existing compatibility path or the matching parameter processor. */
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

    /** Registers a method class in this handler's pool. */
    fun registerLocalIDLMethod(
        clazz: Class<out IDLBridgeMethod>?,
        scope: BridgePlatformType = BridgePlatformType.ALL,
    ) {
        pool.registerLocalIDLMethod(clazz, scope)
    }

    /** Registers a factory in this handler's pool. */
    fun registerLocalIDLMethod(
        name: String,
        scope: BridgePlatformType = BridgePlatformType.ALL,
        clazz: Class<out IDLBridgeMethod>? = null,
        factory: () -> IDLBridgeMethod,
    ) {
        pool.registerLocalIDLMethod(name, scope, clazz, factory)
    }

    /** Supplies the SDK context to the pool and subsequently handled methods. */
    fun setBridgeContext(context: IBridgeContext) {
        this.context = context
        this.pool.setBridgeContext(context)
    }

    /** Finds a method in this handler's pool for the context's platform. */
    fun getBridge(
        bridgeContext: BridgeContext,
        bridgeName: String,
    ): IDLBridgeMethod? {
        val platform = BridgeContext.getPlatformByBridgeContext(bridgeContext)
        return pool.getBridge(bridgeName, platform)
    }

    /** Releases the pool before marking this handler released. */
    override fun onRelease() {
        pool.release()
        isReleased = true
    }

    /** Whether the pool has completed a release through this handler. */
    override fun isReleased(): Boolean = isReleased

    /** Shares the existing data-wrapper extraction with host handler subclasses. */
    protected fun unWrapperParams(params: ReadableMap?): ReadableMap? = params?.getMap("data") ?: params
}
