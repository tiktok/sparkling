// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

/**
 * Desc:
 */
object SparklingBridgeManager {
    const val DEFAULT_NAMESPACE = "DEFAULT"
    private val localBridge = LocalBridge()

    fun registerRegistry(
        registry: IDLMethodRegistry,
    ) {
        localBridge.registerRegistry(registry)
    }

    @JvmStatic
    @JvmOverloads
    fun findIDLMethodClass(
        platformType: BridgePlatformType,
        name: String,
        namespace: String = DEFAULT_NAMESPACE,
    ): Class<out IDLBridgeMethod>? = localBridge.findIDLMethodClass(platformType, name, namespace)

    @JvmStatic
    @JvmOverloads
    fun findIDLMethodProvider(
        platformType: BridgePlatformType,
        name: String,
        namespace: String = DEFAULT_NAMESPACE,
    ): IDLMethodProvider? = localBridge.findIDLMethodProvider(platformType, name, namespace)

    @JvmStatic
    @JvmOverloads
    fun registerIDLMethod(
        clazz: Class<out IDLBridgeMethod>?,
        scope: BridgePlatformType = BridgePlatformType.ALL,
        namespace: String = DEFAULT_NAMESPACE,
    ) {
        localBridge.registerIDLMethod(clazz, scope, namespace)
    }

    @JvmStatic
    @JvmOverloads
    fun registerIDLMethod(
        name: String,
        scope: BridgePlatformType = BridgePlatformType.ALL,
        namespace: String = DEFAULT_NAMESPACE,
        factory: IDLMethodProvider,
    ) {
        localBridge.registerIDLMethod(name, factory, scope, namespace)
    }

    fun registerIDLMethod(
        name: String,
        scope: BridgePlatformType = BridgePlatformType.ALL,
        namespace: String = DEFAULT_NAMESPACE,
        clazz: Class<out IDLBridgeMethod>? = null,
        factory: () -> IDLBridgeMethod,
    ) {
        localBridge.registerIDLMethod(name, IDLMethodProvider { factory() }, scope, namespace, clazz)
    }

    @JvmStatic
    @JvmOverloads
    fun getIDLMethodList(
        platformType: BridgePlatformType,
        namespace: String = DEFAULT_NAMESPACE,
    ): Map<String, Class<out IDLBridgeMethod>>? = localBridge.getIDLMethodList(platformType, namespace)
}
