// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.base

import android.content.Context
import android.os.Bundle

interface IHybridKitBaseService {
    // isFinished for HybridKitInitTask
    fun getHybridInitStatus(): HybridKitInitStatus

    // add callback
    fun addHybridKitInitCallback(callback: HybridKitInitCallback)

    // remove callback
    fun removeHybridInitCallback(callback: HybridKitInitCallback)

    fun open(
        context: Context,
        url: String,
        targetHandlerName: String? = null,
        bundle: Bundle? = null,
    )

    /**
     * must init
     */
    fun initHybridCoreSDK(forceInit: Boolean = false)
}

enum class HybridKitInitStatus {
    INIT,
    LOADING,
    FINISHED,
}

interface HybridKitInitCallback {
    //
    fun isFinished()
}
