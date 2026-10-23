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

/** Handles one business namespace through a registry that host subclasses can supply. */
open class BusinessCallHandler protected constructor(
    val nameSpace: String,
    private val registry: IDLMethodRegistry,
) : IBridgeHandler {
    private var context: IBridgeContext? = null
    private val pool: ConcurrentHashMap<BridgePlatformType, ConcurrentHashMap<String, IDLBridgeMethod>> = ConcurrentHashMap()
    private var isRelease = false

    /** Creates the default registry, including method-name inference from instances. */
    constructor(nameSpace: String) : this(nameSpace, IDLMethodRegistry(inferMethodNameFromInstance = true))

    init {
        registry.namespace = nameSpace
    }

    /** Dispatches a fresh method instance through the matching parameter processor. */
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

    /** Registers a method class in this handler's registry. */
    fun registerMethod(
        clazz: Class<out IDLBridgeMethod>,
        scope: BridgePlatformType = BridgePlatformType.ALL,
    ) {
        registry.registerMethod(clazz, scope)
    }

    /** Registers a factory in this handler's registry. */
    fun registerMethod(
        name: String,
        scope: BridgePlatformType = BridgePlatformType.ALL,
        clazz: Class<out IDLBridgeMethod>? = null,
        factory: () -> IDLBridgeMethod,
    ) {
        registry.registerMethodProvider(name, IDLMethodProvider { factory() }, scope, clazz)
    }

    /** Creates a fresh instance and retains it as the latest for this platform and name. */
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

    /** Whether the registry contains this method on the requested platform. */
    fun isMethodExists(
        name: String,
        platformType: BridgePlatformType = BridgePlatformType.ALL,
    ): Boolean = registry.isMethodExists(name, platformType)

    /** Supplies the SDK context to subsequently handled methods. */
    fun setBridgeContext(bridgeContext: IBridgeContext) {
        context = bridgeContext
    }

    /** The live table retains only the latest instance for each platform and method name. */
    protected fun methodTable(): ConcurrentHashMap<BridgePlatformType, ConcurrentHashMap<String, IDLBridgeMethod>> = pool

    /** Returns the platform's live table, creating it on first use. */
    protected fun getPlatformTypeCache(
        platformType: BridgePlatformType,
    ): ConcurrentHashMap<String, IDLBridgeMethod> = pool.getOrPut(platformType) { ConcurrentHashMap() }

    /** Shares the existing data-wrapper extraction with host handler subclasses. */
    protected fun unWrapperParams(params: ReadableMap?): ReadableMap? = params?.getMap("data") ?: params

    /** Drops retained instances without releasing methods or clearing registrations. */
    override fun onRelease() {
        pool.clear()
        isRelease = true
    }

    /** Whether this handler has cleared its instance table through onRelease. */
    override fun isReleased(): Boolean = isRelease
}
