// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import android.content.Context
import android.util.Size
import android.view.View
import androidx.appcompat.widget.Toolbar
import com.lynx.tasm.LynxView
import com.tiktok.sparkling.hybridkit.HybridContext
import com.tiktok.sparkling.hybridkit.base.HybridKitError
import com.tiktok.sparkling.hybridkit.base.HybridKitType
import com.tiktok.sparkling.hybridkit.base.IKitView

/**
 * Observes a created Lynx view after Sparkling initializes its bridge and before template loading.
 */
fun interface SparklingLynxViewCreatedListener {
    fun onCreated(lynxView: LynxView)
}

interface SparklingUIProvider {
    fun getLoadingView(context: Context): View

    fun getErrorView(context: Context): View

    fun getToolBar(context: Context): Toolbar?
}

interface SparklingLifecycleDelegate {
    fun onLoadStart(
        view: IKitView,
        url: String,
    ) {
    }

    fun onLoadFinish(view: IKitView) {
    }

    fun onLoadFailed(
        view: IKitView,
        url: String,
        error: HybridKitError,
    ) {
    }

    fun onRuntimeReady(kitType: HybridKitType) {
    }

    fun onFirstScreen(view: IKitView) {
    }

    fun onPageUpdate(view: IKitView) {
    }

    fun onUpdate(view: IKitView) {
    }

    fun onReceivedError(
        view: IKitView,
        error: HybridKitError,
    ) {
    }

    fun onReceivedPerformance(
        view: IKitView,
        perf: Map<String, Any>?,
    ) {
    }

    fun onResourceLoadFinish(
        templateArray: ByteArray?,
        baseUrl: String?,
    ) {
    }

    fun onResourceLoadStart(
        view: IKitView,
        url: String,
    ) {
    }

    fun onResourceLoadFinish(
        view: IKitView,
        url: String,
        templateArray: ByteArray?,
        error: HybridKitError?,
    ) {
    }

    fun onIntrinsicContentSizeChanged(
        view: IKitView,
        size: Size,
    ) {
    }

    fun onReload(view: IKitView) {
    }
}

class SparklingContext : HybridContext() {
    var sparklingUIProvider: SparklingUIProvider? = null
    var lifecycleDelegate: SparklingLifecycleDelegate? = null
    var lynxViewCreatedListener: SparklingLynxViewCreatedListener? = null
}
