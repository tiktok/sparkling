// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.base

import android.content.Context
import com.tiktok.sparkling.hybridkit.HybridContext
import com.tiktok.sparkling.hybridkit.scheme.HybridSchemeParam

interface IKitViewProvider<T : IKitView?> {
    val viewType: HybridKitType

    fun createKitView(
        scheme: HybridSchemeParam,
        hybridContext: HybridContext,
        context: Context,
        lifeCycle: IHybridKitLifeCycle?,
    ): T

    fun createKitView(
        url: String,
        param: HybridContext,
        context: Context,
        lifeCycle: IHybridKitLifeCycle? = null,
    ): T
}
