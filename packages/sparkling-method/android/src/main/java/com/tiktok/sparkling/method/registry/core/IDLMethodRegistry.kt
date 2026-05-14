// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

import com.tiktok.sparkling.method.registry.api.BridgeSettings
import java.util.concurrent.ConcurrentHashMap

/**
 * Desc:
 */
class IDLMethodRegistry(
    private val isLocalBridgeRegistry: Boolean = false,
    private val bridgeContext: IBridgeContext? = null,
) {
    companion object {
        @JvmStatic
        fun copyWith(bridgeRegistry: IDLMethodRegistry): IDLMethodRegistry =
            IDLMethodRegistry().apply {
                namespace = bridgeRegistry.namespace
                bridgeRegistry.methodMap.entries.forEach { outerEntry ->
                    val platformType = outerEntry.key
                    val innerMap =
                        ConcurrentHashMap<String, Class<out IDLBridgeMethod>>().apply {
                            this.putAll(outerEntry.value)
                        }
                    this.methodMap[platformType] = innerMap
                }
            }
    }

    var namespace: String = "DEFAULT"

    private val methodMap: ConcurrentHashMap<BridgePlatformType, ConcurrentHashMap<String, Class<out IDLBridgeMethod>>> =
        ConcurrentHashMap()
    private var methodRegistryCache: IDLMethodRegistryCache? = null

    init {
        if (isLocalBridgeRegistry) {
            methodRegistryCache =
                IDLMethodRegistryCache().apply {
                    bridgeContext?.containerID?.let {
                        IDLMethodRegistryCacheManager.registerIDLMethodRegistryCache(
                            it,
                            this,
                        )
                    }
                }
        }
    }

    private fun innerRegisterMethod(
        clazz: Class<out IDLBridgeMethod>,
        scope: BridgePlatformType,
    ) {
        (methodMap[scope] ?: ConcurrentHashMap()).also {
            val name =
                if (!isLocalBridgeRegistry) {
                    (IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(null))?.find(clazz)
                } else {
                    methodRegistryCache?.find(clazz)
                }
            if (!name.isNullOrEmpty()) {
                it[name] = clazz
                methodMap[scope] = it
            }
        }
    }

    fun registerMethod(
        clazz: Class<out IDLBridgeMethod>,
        scope: BridgePlatformType = BridgePlatformType.ALL,
    ) {
        if (!BridgeSettings.bridgeRegistryOptimize) {
            (
                if (scope == BridgePlatformType.ALL) {
                    listOf(
                        BridgePlatformType.ALL,
                        BridgePlatformType.WEB,
                        BridgePlatformType.LYNX,
                    )
                } else {
                    listOf(scope)
                }
            ).forEach {
                innerRegisterMethod(clazz, it)
            }
        } else {
            val name =
                if (!isLocalBridgeRegistry) {
                    (IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(null))?.find(clazz)
                } else {
                    methodRegistryCache?.find(clazz)
                }
            if (!name.isNullOrEmpty()) {
                (
                    if (scope == BridgePlatformType.ALL) {
                        listOf(
                            BridgePlatformType.ALL,
                            BridgePlatformType.WEB,
                            BridgePlatformType.LYNX,
                        )
                    } else {
                        listOf(scope)
                    }
                ).forEach {
                    innerRegisterMethod(clazz, it, name)
                }
            }
        }
    }

    private fun innerRegisterMethod(
        clazz: Class<out IDLBridgeMethod>,
        scope: BridgePlatformType,
        name: String,
    ) {
        (methodMap[scope] ?: ConcurrentHashMap<String, Class<out IDLBridgeMethod>>()).also {
            it[name] = clazz
            methodMap[scope] = it
        }
    }

    fun findMethodClass(
        platformType: BridgePlatformType,
        name: String,
    ): Class<out IDLBridgeMethod>? {
        if (platformType == BridgePlatformType.NONE) {
            return null
        }

        return methodMap[platformType]?.run {
            this[name]
        }
    }

    fun isMethodExists(
        name: String,
        platformType: BridgePlatformType = BridgePlatformType.ALL,
    ): Boolean = findMethodClass(platformType, name) != null

    fun getMethodList(platformType: BridgePlatformType): MutableMap<String, Class<out IDLBridgeMethod>>? {
        if (platformType == BridgePlatformType.NONE) {
            return null
        }

        return methodMap[platformType]
    }
}
