// Copyright (c) 2023 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

import java.util.concurrent.ConcurrentHashMap

/**
 */
class LocalBridge {
    val TAG = "LocalBridge"

    private var isLocalBridge: Boolean = false
    private var context: IBridgeContext? = null
    private val idlRegistryMap: ConcurrentHashMap<String, IDLMethodRegistry> = ConcurrentHashMap()

    fun setIsLocalBridge(isLocalBridge: Boolean) {
        this.isLocalBridge = isLocalBridge
    }

    fun setContext(context: IBridgeContext?) {
        this.context = context
    }

    fun registerRegistry(
        registry: IDLMethodRegistry,
    ) {
        // Allow overriding existing registries
        idlRegistryMap[registry.namespace] = registry
    }

    fun findIDLMethodClass(
        platformType: BridgePlatformType,
        name: String,
        namespace: String = DEFAULT_NAMESPACE,
    ): Class<out IDLBridgeMethod>? {
        idlRegistryMap[namespace] ?: return null

        return idlRegistryMap[namespace]?.findMethodClass(platformType, name)
    }

    fun registerIDLMethod(
        clazz: Class<out IDLBridgeMethod>?,
        scope: BridgePlatformType = BridgePlatformType.ALL,
        namespace: String = DEFAULT_NAMESPACE,
    ) {
        clazz?.let { nonNullClazz ->
            if (idlRegistryMap[namespace] == null) {
                idlRegistryMap[namespace] = IDLMethodRegistry(isLocalBridge, this.context)
            }
            idlRegistryMap[namespace]?.registerMethod(nonNullClazz, scope)
        }
    }

    fun getIDLMethodList(
        platformType: BridgePlatformType,
        namespace: String = DEFAULT_NAMESPACE,
    ): Map<String, Class<out IDLBridgeMethod>>? {
        idlRegistryMap[namespace] ?: return null

        return idlRegistryMap[namespace]?.getMethodList(platformType)
    }

    companion object {
        const val DEFAULT_NAMESPACE = "DEFAULT"
    }
}
