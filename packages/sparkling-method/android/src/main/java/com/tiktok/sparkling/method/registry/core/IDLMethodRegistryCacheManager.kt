// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * Which annotation cache answers for which container.
 *
 * A container that registers methods of its own gets its own [IDLMethodRegistryCache], so that what
 * one container read off a method class is not handed to another. The reference is weak because the
 * container owns the cache: a container that goes away must not be held here. A container that
 * registered none is answered from the process-wide cache.
 *
 * A host whose method classes carry their own annotations installs its own [IDLMethodRegistryCache]
 * subclass as the process-wide one through [installProcessWideCache]. A host that needs no subclass
 * does not have to call anything.
 *
 * Deviation from open source: upstream keeps the process-wide cache in a separate `IDLRegistryCache`
 * singleton and reads it from here. That singleton is not mirrored yet — nothing upstream calls it
 * except this lookup, and it cannot hold a plain [IDLMethodRegistryCache] in a host build — so the
 * process-wide cache lives here instead, behind the same name this object already answers with.
 */
object IDLMethodRegistryCacheManager {

    private val map = ConcurrentHashMap<String, WeakReference<IDLMethodRegistryCache>>()

    @Volatile
    private var processWideCache: IDLMethodRegistryCache? = null

    /**
     * Installs the cache that answers for containers that registered none, replacing whatever was
     * answering before.
     *
     * Replacing is safe here in a way it would not be for a registry: a cache is derived from the
     * method class, and every reader of it reads the class directly when the cache misses, so the
     * cache installed here refills itself and whatever the replaced one held is recomputed rather
     * than lost. That is what frees this from any order between installing and the first lookup.
     */
    fun installProcessWideCache(cache: IDLMethodRegistryCache) {
        synchronized(this) {
            processWideCache = cache
        }
    }

    /**
     * The cache to read a method class through: the one [containerID] registered, or the
     * process-wide one when it registered none or the one it registered has been collected.
     */
    fun provideIDLMethodRegistryCache(containerID: String?): IDLMethodRegistryCache? =
        findIDLMethodRegistryCache(containerID) ?: processWide()

    fun registerIDLMethodRegistryCache(
        containerID: String,
        cache: IDLMethodRegistryCache,
    ) {
        map[containerID] = WeakReference(cache)
    }

    fun unregisterIDLMethodRegistryCache(containerID: String) {
        map.remove(containerID)
    }

    /** The cache [containerID] registered, or null if it registered none or it has been collected. */
    private fun findIDLMethodRegistryCache(containerID: String?): IDLMethodRegistryCache? {
        val id = containerID ?: return null
        return map[id]?.get()
    }

    /**
     * The installed cache, or a plain one from here on for a host that installs none.
     *
     * Creating the fallback takes the same lock [installProcessWideCache] does, so an install that
     * lands while one is being created cannot be overwritten by it.
     */
    private fun processWide(): IDLMethodRegistryCache =
        processWideCache ?: synchronized(this) {
            processWideCache ?: IDLMethodRegistryCache().also { processWideCache = it }
        }
}
