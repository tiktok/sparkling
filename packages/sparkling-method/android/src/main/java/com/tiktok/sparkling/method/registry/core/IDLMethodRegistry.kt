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
    private val inferMethodNameFromInstance: Boolean = isLocalBridgeRegistry,
) {
    companion object {
        @JvmStatic
        fun copyWith(bridgeRegistry: IDLMethodRegistry): IDLMethodRegistry =
            IDLMethodRegistry().apply {
                namespace = bridgeRegistry.namespace
                bridgeRegistry.methodMap.entries.forEach { outerEntry ->
                    val platformType = outerEntry.key
                    val innerMap =
                        ConcurrentHashMap<String, IDLMethodProvider>().apply {
                            this.putAll(outerEntry.value)
                        }
                    this.methodMap[platformType] = innerMap
                }
                bridgeRegistry.methodClassMap.entries.forEach { outerEntry ->
                    val platformType = outerEntry.key
                    val innerMap =
                        ConcurrentHashMap<String, Class<out IDLBridgeMethod>>().apply {
                            this.putAll(outerEntry.value)
                        }
                    this.methodClassMap[platformType] = innerMap
                }
            }
    }

    var namespace: String = "DEFAULT"

    private val methodMap: ConcurrentHashMap<BridgePlatformType, ConcurrentHashMap<String, IDLMethodProvider>> =
        ConcurrentHashMap()
    private val methodClassMap: ConcurrentHashMap<BridgePlatformType, ConcurrentHashMap<String, Class<out IDLBridgeMethod>>> =
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
        val name = methodNameFor(clazz)
        if (!name.isNullOrEmpty()) {
            innerRegisterMethod(name, methodProviderFor(clazz), scope, clazz)
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
            val name = methodNameFor(clazz)
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
        innerRegisterMethod(name, methodProviderFor(clazz), scope, clazz)
    }

    fun registerMethodProvider(
        name: String,
        methodProvider: IDLMethodProvider,
        scope: BridgePlatformType = BridgePlatformType.ALL,
        clazz: Class<out IDLBridgeMethod>? = null,
    ) {
        if (name.isEmpty()) return
        clazz?.let {
            if (!isLocalBridgeRegistry) {
                IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(null)?.cacheMethodClass(it)
            } else {
                methodRegistryCache?.cacheMethodClass(it)
            }
        }
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
            innerRegisterMethod(name, methodProvider, it, clazz)
        }
    }

    private fun innerRegisterMethod(
        name: String,
        methodProvider: IDLMethodProvider,
        scope: BridgePlatformType,
        clazz: Class<out IDLBridgeMethod>?,
    ) {
        (methodMap[scope] ?: ConcurrentHashMap()).also {
            it[name] = methodProvider
            methodMap[scope] = it
        }
        if (clazz != null) {
            (methodClassMap[scope] ?: ConcurrentHashMap()).also {
                it[name] = clazz
                methodClassMap[scope] = it
            }
        }
    }

    fun findMethodClass(
        platformType: BridgePlatformType,
        name: String,
    ): Class<out IDLBridgeMethod>? {
        if (platformType == BridgePlatformType.NONE) {
            return null
        }

        return methodClassMap[platformType]?.run {
            this[name]
        }
    }

    fun findMethodProvider(
        platformType: BridgePlatformType,
        name: String,
    ): IDLMethodProvider? {
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
    ): Boolean = findMethodProvider(platformType, name) != null || findMethodClass(platformType, name) != null

    fun getMethodList(platformType: BridgePlatformType): MutableMap<String, Class<out IDLBridgeMethod>>? {
        if (platformType == BridgePlatformType.NONE) {
            return null
        }

        return methodClassMap[platformType]
    }

    private fun methodNameFor(clazz: Class<out IDLBridgeMethod>): String? {
        val cache =
            if (!isLocalBridgeRegistry) {
                IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(null)
            } else {
                methodRegistryCache
            }
        val cachedName = cache?.find(clazz)
        if (!cachedName.isNullOrEmpty()) {
            return cachedName
        }
        if (!inferMethodNameFromInstance) {
            return null
        }
        return runCatching { methodProviderFor(clazz).provideMethod().name }.getOrNull()
    }

    @Suppress("DEPRECATION")
    private fun methodProviderFor(clazz: Class<out IDLBridgeMethod>): IDLMethodProvider = IDLMethodProvider { clazz.newInstance() }
}
