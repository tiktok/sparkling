// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

import com.tiktok.sparkling.method.registry.api.BridgeSettings
import java.util.concurrent.ConcurrentHashMap

/**
 * Registers method classes and providers by platform and name.
 * Hosts can customize name resolution while sharing registration and lookup behavior.
 */
open class IDLMethodRegistry(
    protected val isLocalBridgeRegistry: Boolean = false,
    protected val bridgeContext: IBridgeContext? = null,
    private val inferMethodNameFromInstance: Boolean = isLocalBridgeRegistry,
) {
    companion object {
        /** Copies the namespace and registrations into a new global registry. */
        @JvmStatic
        fun copyWith(bridgeRegistry: IDLMethodRegistry): IDLMethodRegistry =
            IDLMethodRegistry().apply {
                copyFrom(bridgeRegistry)
            }
    }

    var namespace: String = "DEFAULT"

    private val methodMap: ConcurrentHashMap<BridgePlatformType, ConcurrentHashMap<String, IDLMethodProvider>> =
        ConcurrentHashMap()
    protected val methodClassMap: ConcurrentHashMap<BridgePlatformType, ConcurrentHashMap<String, Class<out IDLBridgeMethod>>> =
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
        val name = resolveMethodName(clazz)
        if (!name.isNullOrEmpty()) {
            innerRegisterMethod(name, methodProviderFor(clazz), scope, clazz)
        }
    }

    /** Registers a class on the requested platforms after resolving its method name. */
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
            val name = resolveMethodName(clazz)
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

    /** Registers a factory, optionally associating a class for metadata and class lookups. */
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

    /** Returns the registered class, or null for an unknown method or the NONE platform. */
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

    /** Returns the registered factory, or null for an unknown method or the NONE platform. */
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

    /** Whether either a provider or a class is registered for this name and platform. */
    fun isMethodExists(
        name: String,
        platformType: BridgePlatformType = BridgePlatformType.ALL,
    ): Boolean = findMethodProvider(platformType, name) != null || findMethodClass(platformType, name) != null

    /** Returns the live class table for a platform, excluding NONE. */
    fun getMethodList(platformType: BridgePlatformType): MutableMap<String, Class<out IDLBridgeMethod>>? {
        if (platformType == BridgePlatformType.NONE) {
            return null
        }

        return methodClassMap[platformType]
    }

    /**
     * Copies the namespace and replaces both tables on every platform present in [other].
     * Platforms absent from the source are retained. The new maps share their provider objects
     * and class values with the source.
     */
    fun copyFrom(other: IDLMethodRegistry) {
        namespace = other.namespace
        (other.methodMap.keys + other.methodClassMap.keys).forEach { platform ->
            val providers = other.methodMap[platform]
            val classes = other.methodClassMap[platform]
            if (providers != null) {
                methodMap[platform] = ConcurrentHashMap(providers)
            } else {
                methodMap.remove(platform)
            }
            if (classes != null) {
                methodClassMap[platform] = ConcurrentHashMap(classes)
            } else {
                methodClassMap.remove(platform)
            }
        }
    }

    /** Resolves cached annotations first, then uses an instance only when configured to do so. */
    protected open fun resolveMethodName(clazz: Class<out IDLBridgeMethod>): String? {
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
