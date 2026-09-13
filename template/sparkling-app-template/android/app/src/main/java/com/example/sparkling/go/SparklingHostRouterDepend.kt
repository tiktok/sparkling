// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.example.sparkling.go

import android.content.Context
import com.tiktok.sparkling.Sparkling
import com.tiktok.sparkling.SparklingBackPress
import com.tiktok.sparkling.SparklingContext
import com.tiktok.sparkling.hybridkit.service.HybridActivityStackManager
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IBridgeContext
import com.tiktok.sparkling.method.router.utils.IHostRouterDepend

class SparklingHostRouterDepend : IHostRouterDepend {
    override fun openScheme(
        bridgeContext: IBridgeContext?,
        scheme: String,
        extraParams: Map<String, Any>,
        platformType: BridgePlatformType,
        context: Context?,
    ): Boolean {
        val sparklingContext = SparklingContext()
        sparklingContext.scheme = scheme
        context?.let { Sparkling.Companion.build(it, sparklingContext).navigate() }
        return true
    }

    override fun closeView(
        bridgeContext: IBridgeContext?,
        type: BridgePlatformType,
        containerID: String?,
        animated: Boolean?,
    ): Boolean {
        val ownerActivity = bridgeContext?.ownerActivity
        if (ownerActivity != null) {
            ownerActivity.finish()
        } else {
            HybridActivityStackManager.getTopActivity()?.finish()
        }
        return true
    }

    override fun setBackPressIntercept(
        bridgeContext: IBridgeContext?,
        containerID: String?,
        intercept: Boolean,
    ): Boolean {
        // The container consults this on every back press. While it is on, the
        // page gets an `onBackPress` event and decides for itself - closing is
        // then `router.close`, not the container acting on its own.
        SparklingBackPress.setIntercepting(containerID ?: bridgeContext?.containerID, intercept)
        return true
    }
}
