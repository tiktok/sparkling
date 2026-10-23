// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

import java.util.concurrent.ConcurrentHashMap

/** Preserves the Sparkling global-cache API while the shared manager owns the cache. */
object IDLRegistryCache {
    const val TAG = "IDLRegistryCache"

    /** The live annotation table of the process-wide cache. */
    val BRIDGE_ANNOTATION_MAP: ConcurrentHashMap<Class<out IDLBridgeMethod>, IDLAnnotationData>
        get() = getRealIDLRegistryCache().BRIDGE_ANNOTATION_MAP

    /** The live result-model index of the process-wide cache. */
    val BRIDGE_RESULT_MAP: ConcurrentHashMap<Class<*>, Class<*>>
        get() = getRealIDLRegistryCache().BRIDGE_RESULT_MAP

    /** Returns the shared manager's default or host-installed process-wide cache. */
    fun getRealIDLRegistryCache(): IDLMethodRegistryCache =
        checkNotNull(IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(null))

    /** Retains the existing static JVM entry point for resolving a method's name. */
    @JvmStatic
    fun find(clazz: Class<out IDLBridgeMethod>): String = getRealIDLRegistryCache().find(clazz)

    /** Reads result metadata from the same cache exposed by the legacy map getters. */
    fun getAnnotationDataByResultClass(resultModelClazz: Class<*>): IDLAnnotationData? =
        getRealIDLRegistryCache().getAnnotationDataByResultClass(resultModelClazz)
}
