// Copyright (c) 2023 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 */
object IDLMethodRegistryCacheManager {
    /**
     * containerID to cache
     */
    private val map = ConcurrentHashMap<String, WeakReference<IDLMethodRegistryCache>>()

    fun provideIDLMethodRegistryCache(containerID: String?): IDLMethodRegistryCache? =
        if (containerID == null) {
            IDLRegistryCache.getRealIDLRegistryCache()
        } else {
            map[containerID]?.get() ?: IDLRegistryCache.getRealIDLRegistryCache()
        }

    fun registerIDLMethodRegistryCache(
        containerID: String,
        cache: IDLMethodRegistryCache,
    ) {
        map[containerID] = WeakReference(cache)
    }

    fun unregisterIDLMethodRegistryCache(containerID: String) {
        map.remove(containerID)
    }
}
