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
import com.tiktok.sparkling.method.registry.core.IDLMethodProvider
import com.tiktok.sparkling.method.registry.core.IDLMethodRegistry
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.api.util.BridgeMethodCallbackHelper
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 */
class BusinessCallHandler(
    val nameSpace: String,
) : IBridgeHandler {
    private var context: IBridgeContext? = null
    private var registry =
        IDLMethodRegistry(inferMethodNameFromInstance = true).apply { namespace = nameSpace }
    private var pool: ConcurrentHashMap<BridgePlatformType, ConcurrentHashMap<String, IDLBridgeMethod>> = ConcurrentHashMap()
    private var isRelease = false

    override fun handle(
        bridgeContext: BridgeContext,
        call: BridgeCall,
        callback: IBridgeMethodCallback,
    ) {
        val bridge = getBridge(bridgeContext, call.bridgeName) ?: return BridgeMethodCallbackHelper.bridgeNotFound(callback)

        context?.let {
            bridge.setBridgeContext(it)
        }
        val params = call.params
        when (params) {
            is JSONObject -> {
                JsonProcessor(bridge, params, call.platform)
            }

            is ReadableMap -> {
                ReadableMapProcessor(bridge, unWrapperParams(params) ?: params)
            }

            else -> {
                return
            }
        }.handle(callback)
    }

    fun registerMethod(
        clazz: Class<out IDLBridgeMethod>,
        scope: BridgePlatformType = BridgePlatformType.ALL,
    ) {
        registry.registerMethod(clazz, scope)
    }

    fun registerMethod(
        name: String,
        scope: BridgePlatformType = BridgePlatformType.ALL,
        clazz: Class<out IDLBridgeMethod>? = null,
        factory: () -> IDLBridgeMethod,
    ) {
        registry.registerMethodProvider(name, IDLMethodProvider { factory() }, scope, clazz)
    }

    fun getBridge(
        bridgeContext: BridgeContext,
        bridgeName: String,
    ): IDLBridgeMethod? {
        val platformType = BridgeContext.getPlatformByBridgeContext(bridgeContext)
        val provider = registry.findMethodProvider(platformType, bridgeName) ?: return null
        val newInstance = provider.provideMethod()
        getPlatformTypeCache(platformType)[bridgeName] = newInstance
        return newInstance
    }

    fun isMethodExists(
        name: String,
        platformType: BridgePlatformType = BridgePlatformType.ALL,
    ): Boolean = registry.isMethodExists(name, platformType)

    fun setBridgeContext(bridgeContext: IBridgeContext) {
        context = bridgeContext
    }

    private fun getPlatformTypeCache(platformType: BridgePlatformType): ConcurrentHashMap<String, IDLBridgeMethod> {
        val concurrentHashMap = pool[platformType]
        return if (concurrentHashMap == null) {
            pool[platformType] = ConcurrentHashMap()
            pool[platformType]!!
        } else {
            concurrentHashMap
        }
    }

    private fun unWrapperParams(params: ReadableMap?): ReadableMap? = params?.getMap("data") ?: params

    override fun onRelease() {
        pool.clear()
        isRelease = true
    }

    override fun isReleased(): Boolean = isRelease
}
