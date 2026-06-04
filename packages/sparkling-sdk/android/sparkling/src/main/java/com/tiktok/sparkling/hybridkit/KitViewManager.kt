// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit

import com.tiktok.sparkling.debug.SparklingDebugToolRegistry
import com.tiktok.sparkling.hybridkit.base.IKitView
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object KitViewManager {
    private val kitViewMap = ConcurrentHashMap<String, WeakReference<IKitView>>()
    private val createdListeners = CopyOnWriteArrayList<KitViewCreatedListener>()
    private val destroyedListeners = CopyOnWriteArrayList<KitViewDestroyedListener>()

    fun interface KitViewCreatedListener {
        fun onKitViewCreated(kitView: IKitView)
    }

    fun interface KitViewDestroyedListener {
        fun onKitViewDestroyed(
            containerId: String,
            kitView: IKitView?,
        )
    }

    /** Register a listener that fires once per [IKitView] right after creation. */
    fun addKitViewCreatedListener(listener: KitViewCreatedListener) {
        createdListeners.addIfAbsent(listener)
    }

    fun removeKitViewCreatedListener(listener: KitViewCreatedListener) {
        createdListeners.remove(listener)
    }

    fun addKitViewDestroyedListener(listener: KitViewDestroyedListener) {
        destroyedListeners.addIfAbsent(listener)
    }

    fun removeKitViewDestroyedListener(listener: KitViewDestroyedListener) {
        destroyedListeners.remove(listener)
    }

    fun addKitView(kitView: IKitView) {
        val containerId = kitView.hybridContext.containerId
        kitViewMap[containerId] = WeakReference(kitView)
        if (createdListeners.isNotEmpty()) {
            createdListeners.forEach { runCatching { it.onKitViewCreated(kitView) } }
        }
        SparklingDebugToolRegistry.notifyKitViewCreated(containerId, kitView)
    }

    fun getKitView(containerId: String): IKitView? = kitViewMap[containerId]?.get()

    /**
     * Snapshot every currently-tracked, still-live [IKitView] keyed by its
     * containerId. Garbage-collected entries are dropped from the map.
     */
    fun getKitViews(): Map<String, IKitView> {
        val result = LinkedHashMap<String, IKitView>(kitViewMap.size)
        val staleKeys = mutableListOf<String>()
        kitViewMap.forEach { (id, ref) ->
            val view = ref.get()
            if (view != null) result[id] = view else staleKeys += id
        }
        staleKeys.forEach { kitViewMap.remove(it) }
        return result
    }

    fun removeKitView(containerId: String) {
        val ref = kitViewMap.remove(containerId) ?: return
        val view = ref.get()
        if (destroyedListeners.isNotEmpty()) {
            destroyedListeners.forEach { runCatching { it.onKitViewDestroyed(containerId, view) } }
        }
        SparklingDebugToolRegistry.notifyKitViewDestroyed(containerId, view)
    }
}
