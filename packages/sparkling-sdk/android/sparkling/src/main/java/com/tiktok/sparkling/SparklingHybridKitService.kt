// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import android.content.Context
import android.os.Bundle
import com.tiktok.sparkling.hybridkit.base.HybridKitInitCallback
import com.tiktok.sparkling.hybridkit.base.HybridKitInitStatus
import com.tiktok.sparkling.hybridkit.base.IHybridKitBaseService
import com.tiktok.sparkling.hybridkit.utils.HybridKitInitManager
import java.util.concurrent.atomic.AtomicBoolean

object SparklingHybridKitService : IHybridKitBaseService {
    private val initialized = AtomicBoolean(false)

    override fun getHybridInitStatus(): HybridKitInitStatus = HybridKitInitManager.status

    override fun addHybridKitInitCallback(callback: HybridKitInitCallback) {
        HybridKitInitManager.addCallback(callback)
    }

    override fun removeHybridInitCallback(callback: HybridKitInitCallback) {
        HybridKitInitManager.removeCallback(callback)
    }

    override fun open(
        context: Context,
        url: String,
        targetHandlerName: String?,
        bundle: Bundle?,
    ) {
        TODO("Not yet implemented")
    }

    override fun initHybridCoreSDK(forceInit: Boolean) {
//        if (!initialized.get()) {
//
//            initialized.set(true)
//        }
    }
}
