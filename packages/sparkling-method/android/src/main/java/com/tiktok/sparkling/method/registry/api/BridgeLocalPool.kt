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

class BridgeLocalPool : IReleasable {
    private var context: IBridgeContext? = null
    private val map: ConcurrentHashMap<BridgePlatformType, ConcurrentHashMap<String, IDLBridgeMethod>> = ConcurrentHashMap()

    private val localBridge: LocalBridge by lazy {
        LocalBridge().apply { setIsLocalBridge(true) }
    }

    fun setBridgeContext(context: IBridgeContext?) {
        this.context = context
        localBridge.setContext(context)
    }

    fun registerLocalIDLMethod(
        clazz: Class<out IDLBridgeMethod>?,
        scope: BridgePlatformType = BridgePlatformType.ALL,
    ) {
        localBridge.registerIDLMethod(clazz, scope)
    }

    fun registerLocalIDLMethod(
        name: String,
        scope: BridgePlatformType = BridgePlatformType.ALL,
        clazz: Class<out IDLBridgeMethod>? = null,
        factory: () -> IDLBridgeMethod,
    ) {
        localBridge.registerIDLMethod(name, IDLMethodProvider { factory() }, scope, clazz = clazz)
    }

    fun getBridge(
        bridgeName: String,
        platformType: BridgePlatformType,
    ): IDLBridgeMethod? {
        val method = map[platformType]?.get(bridgeName) ?: map[BridgePlatformType.ALL]?.get(bridgeName)
        if (method != null) {
            return method
        } else {
            val provider =
                localBridge.findIDLMethodProvider(platformType, bridgeName)
                    ?: SparklingBridgeManager.findIDLMethodProvider(platformType, bridgeName)
                    ?: return null
            val newInstance = provider.provideMethod()
            getPlatformTypeCache(platformType)[bridgeName] = newInstance
            return newInstance
        }
    }

    private fun getPlatformTypeCache(platformType: BridgePlatformType): ConcurrentHashMap<String, IDLBridgeMethod> {
        val concurrentHashMap = map[platformType]
        return if (concurrentHashMap == null) {
            map[platformType] = ConcurrentHashMap()
            map[platformType]!!
        } else {
            concurrentHashMap
        }
    }

    override fun release() {
        map.forEach { entry ->
            entry.value.forEach {
                it.value.release()
            }
        }
        map.clear()
    }
}
