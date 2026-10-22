// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.api

import com.tiktok.sparkling.method.registry.core.IBridgeContext
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.IDLMethodProvider
import com.tiktok.sparkling.method.registry.core.LocalBridge
import com.tiktok.sparkling.method.registry.core.SparklingBridgeManager
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.interfaces.IReleasable
import java.util.concurrent.ConcurrentHashMap

/** Caches method instances by platform and name, preferring local registrations over global ones. */
open class BridgeLocalPool : IReleasable {
    protected var bridgeContext: IBridgeContext? = null
        private set
    private val map: ConcurrentHashMap<BridgePlatformType, ConcurrentHashMap<String, IDLBridgeMethod>> = ConcurrentHashMap()

    private val defaultLocalBridge: LocalBridge by lazy {
        LocalBridge().apply { setIsLocalBridge(true) }
    }

    /** The holder used for local registration and lookup. */
    protected open fun localBridge(): LocalBridge = defaultLocalBridge

    /** The fallback platform cache to consult after a platform-specific cache miss. */
    protected open fun allPlatform(): BridgePlatformType = BridgePlatformType.ALL

    /** Looks up the local factory after an instance-cache miss. */
    protected open fun findLocalMethodProvider(
        platformType: BridgePlatformType,
        bridgeName: String,
    ): IDLMethodProvider? = localBridge().findIDLMethodProvider(platformType, bridgeName)

    /** Looks up the process-wide factory when no local registration exists. */
    protected open fun findGlobalMethodProvider(
        platformType: BridgePlatformType,
        bridgeName: String,
    ): IDLMethodProvider? = SparklingBridgeManager.findIDLMethodProvider(platformType, bridgeName)

    /** Updates the context on this pool and its local holder. */
    fun setBridgeContext(context: IBridgeContext?) {
        this.bridgeContext = context
        localBridge().setContext(context)
    }

    /** Registers a local method class through the pool's holder. */
    fun registerLocalIDLMethod(
        clazz: Class<out IDLBridgeMethod>?,
        scope: BridgePlatformType = BridgePlatformType.ALL,
    ) {
        localBridge().registerIDLMethod(clazz, scope)
    }

    /** Registers a local factory, including methods without a no-argument constructor. */
    fun registerLocalIDLMethod(
        name: String,
        scope: BridgePlatformType = BridgePlatformType.ALL,
        clazz: Class<out IDLBridgeMethod>? = null,
        factory: () -> IDLBridgeMethod,
    ) {
        localBridge().registerIDLMethod(name, IDLMethodProvider { factory() }, scope, clazz = clazz)
    }

    /** Returns a cached instance or creates one synchronously using the registered provider. */
    fun getBridge(
        bridgeName: String,
        platformType: BridgePlatformType,
    ): IDLBridgeMethod? {
        val method = map[platformType]?.get(bridgeName) ?: map[allPlatform()]?.get(bridgeName)
        if (method != null) {
            return method
        } else {
            val provider =
                findLocalMethodProvider(platformType, bridgeName)
                    ?: findGlobalMethodProvider(platformType, bridgeName)
                    ?: return null
            val newInstance = provider.provideMethod()
            getPlatformTypeCache(platformType)[bridgeName] = newInstance
            return newInstance
        }
    }

    /** Shares the actual instance table with host extensions. */
    protected fun methodTable(): ConcurrentHashMap<BridgePlatformType, ConcurrentHashMap<String, IDLBridgeMethod>> = map

    /** Returns the platform bucket, creating it on first use. */
    protected fun getPlatformTypeCache(platformType: BridgePlatformType): ConcurrentHashMap<String, IDLBridgeMethod> =
        map.getOrPut(platformType) { ConcurrentHashMap() }

    /** Releases cached instances and clears their table while keeping the registrations. */
    override fun release() {
        map.forEach { entry ->
            entry.value.forEach {
                it.value.release()
            }
        }
        map.clear()
    }
}
