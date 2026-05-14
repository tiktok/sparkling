// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit

import com.tiktok.sparkling.hybridkit.base.IKitView
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

object KitViewManager {
    private val kitViewMap = ConcurrentHashMap<String, WeakReference<IKitView>>()

    fun addKitView(kitView: IKitView) {
        kitViewMap[kitView.hybridContext.containerId] = WeakReference(kitView)
    }

    fun getKitView(containerId: String): IKitView? = kitViewMap[containerId]?.get()

    fun removeKitView(containerId: String) {
        kitViewMap.remove(containerId)
    }
}
