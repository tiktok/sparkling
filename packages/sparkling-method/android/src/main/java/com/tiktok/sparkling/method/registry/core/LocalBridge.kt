// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

import java.util.concurrent.ConcurrentHashMap

/**
 * Holds one registry per namespace. Hosts can supply a registry subclass through [createRegistry].
 */
open class LocalBridge {
    open val TAG = "LocalBridge"

    protected var isLocalBridge: Boolean = false
        private set
    protected var bridgeContext: IBridgeContext? = null
        private set
    private val idlRegistryMap: ConcurrentHashMap<String, IDLMethodRegistry> = ConcurrentHashMap()

    /** Selects local or global registration for subsequently created registries. */
    fun setIsLocalBridge(isLocalBridge: Boolean) {
        this.isLocalBridge = isLocalBridge
    }

    /** Supplies the context used by subsequently created registries. */
    fun setContext(context: IBridgeContext?) {
        this.bridgeContext = context
    }

    /** Replaces any registry already registered under the same namespace. */
    fun registerRegistry(
        registry: IDLMethodRegistry,
    ) {
        // Allow overriding existing registries
        idlRegistryMap[registry.namespace] = registry
    }

    /** Finds a method class within the requested namespace and platform. */
    fun findIDLMethodClass(
        platformType: BridgePlatformType,
        name: String,
        namespace: String = DEFAULT_NAMESPACE,
    ): Class<out IDLBridgeMethod>? {
        idlRegistryMap[namespace] ?: return null

        return idlRegistryMap[namespace]?.findMethodClass(platformType, name)
    }

    /** Finds a method factory within the requested namespace and platform. */
    fun findIDLMethodProvider(
        platformType: BridgePlatformType,
        name: String,
        namespace: String = DEFAULT_NAMESPACE,
    ): IDLMethodProvider? {
        idlRegistryMap[namespace] ?: return null

        return idlRegistryMap[namespace]?.findMethodProvider(platformType, name)
    }

    /** Registers a method class, creating the namespace's registry on first use. */
    fun registerIDLMethod(
        clazz: Class<out IDLBridgeMethod>?,
        scope: BridgePlatformType = BridgePlatformType.ALL,
        namespace: String = DEFAULT_NAMESPACE,
    ) {
        clazz?.let { nonNullClazz ->
            if (idlRegistryMap[namespace] == null) {
                idlRegistryMap[namespace] = createRegistry()
            }
            idlRegistryMap[namespace]?.registerMethod(nonNullClazz, scope)
        }
    }

    /** Registers a factory, creating the namespace's registry on first use. */
    fun registerIDLMethod(
        name: String,
        methodProvider: IDLMethodProvider,
        scope: BridgePlatformType = BridgePlatformType.ALL,
        namespace: String = DEFAULT_NAMESPACE,
        clazz: Class<out IDLBridgeMethod>? = null,
    ) {
        if (idlRegistryMap[namespace] == null) {
            idlRegistryMap[namespace] = createRegistry()
        }
        idlRegistryMap[namespace]?.registerMethodProvider(name, methodProvider, scope, clazz)
    }

    /** Returns the registered classes for the requested namespace and platform. */
    fun getIDLMethodList(
        platformType: BridgePlatformType,
        namespace: String = DEFAULT_NAMESPACE,
    ): Map<String, Class<out IDLBridgeMethod>>? {
        idlRegistryMap[namespace] ?: return null

        return idlRegistryMap[namespace]?.getMethodList(platformType)
    }

    /** Creates the registry used by both class and provider registration. */
    protected open fun createRegistry(): IDLMethodRegistry = IDLMethodRegistry(isLocalBridge, bridgeContext)

    /** Shares the actual namespace table with host extensions. */
    protected fun registryTable(): ConcurrentHashMap<String, IDLMethodRegistry> = idlRegistryMap

    companion object {
        const val DEFAULT_NAMESPACE = "DEFAULT"
    }
}
