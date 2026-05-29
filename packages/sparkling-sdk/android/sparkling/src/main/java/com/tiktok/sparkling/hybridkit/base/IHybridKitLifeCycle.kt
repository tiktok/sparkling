// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.base

import com.tiktok.sparkling.hybridkit.api.IDependencyIterator

abstract class AbsHybridKitLifeCycle(
    override var loadStatusLifeCycle: ILoadStatusLifeCycle? = null,
) : IHybridKitLifeCycle(loadStatusLifeCycle),
    IDependencyIterator<AbsHybridKitLifeCycle> {
    var next: AbsHybridKitLifeCycle? = null

    override fun next(): AbsHybridKitLifeCycle? = next

    override fun next(t: AbsHybridKitLifeCycle?) {
        next = t
    }
}

abstract class IHybridKitLifeCycle(
    open var loadStatusLifeCycle: ILoadStatusLifeCycle? = null,
) {
    private var hasFailed = false

    /**
     * callback before KitView created
     */
    open fun onPreKitCreate() {}

    /**
     * callback after KitView created
     */
    open fun onPostKitCreated(view: IKitView) {}

    /**
     * callback when KitView start loading
     */
    open fun onLoadStart(
        view: IKitView,
        url: String,
    ) {
    }

    /**
     * callback when KitView load failed
     */
    open fun onLoadFailed(
        view: IKitView,
        url: String,
    ) {
    }

    /**
     * callback when KitView load failed
     */
    open fun onLoadFailed(
        view: IKitView,
        url: String,
        reason: String?,
    ) {
        onLoadFailed(view, url)
    }

    open fun onLoadFailed(
        view: IKitView,
        url: String,
        hybridKitError: HybridKitError,
    ) {
        onLoadFailed(view, url, hybridKitError.errorReason)
    }

    /**
     * callback when KitView Runtime Ready
     * @see HybridKitType.LYNX
     * @see HybridKitType.WEB
     * @see HybridKitType.UNKNOWN
     */
    open fun onRuntimeReady(kitType: HybridKitType) {}

    /**
     * callback when Lynx first screen layout completed
     */
    open fun onFirstScreen(view: IKitView) {}

    /**
     * callback when Lynx page update completed
     */
    open fun onPageUpdate(view: IKitView) {}

    /**
     * callback when KitView data/update event completed
     */
    open fun onUpdate(view: IKitView) {}

    /**
     * callback when KitView receives a non-load-finish error
     */
    open fun onReceivedError(
        view: IKitView,
        hybridKitError: HybridKitError,
    ) {
    }

    /**
     * callback when KitView receives performance data
     */
    open fun onReceivedPerformance(
        view: IKitView,
        perf: Map<String, Any>?,
    ) {
    }

    /**
     * callback when load finish
     */
    open fun onLoadFinish(view: IKitView) {}

    /**
     * callback before KitView reloads the current url
     */
    open fun onReload(view: IKitView) {}

    /**
     * callback when kit destroy
     */
    open fun onDestroy(kitView: IKitView) {}

    /**
     * from finish, we need to clean context
     */
    open fun onClearContext() {
    }

    /**
     * now only for webview security, can add to Lynx if needed
     */
    open fun onLoadUrlCheck(
        view: IKitView,
        willLoadUrl: String,
    ): LoadUrlCheck? = null

    open fun onResourceLoadFinish(
        templateArray: ByteArray?,
        baseUrl: String?,
    ) {}

    open fun onResourceLoadStart(
        view: IKitView,
        url: String,
    ) {
    }

    open fun onResourceLoadFinish(
        view: IKitView,
        url: String,
        templateArray: ByteArray?,
        hybridKitError: HybridKitError?,
    ) {
        if (hybridKitError == null) {
            onResourceLoadFinish(templateArray, url)
        }
    }

    /**
     * this callback would be invoked when first dispatchDraw after load/reload/updateData/updateGlobalProps/updateMetaData
     */
    open fun onEffectiveDrawCallback() {}
}

interface ILoadStatusLifeCycle {
    fun onLoadStart(
        view: IKitView,
        url: String,
    )

    fun onLoadSuccess(view: IKitView)

    fun onLoadFailed(
        view: IKitView,
        url: String,
        hybridKitError: HybridKitError,
    )
}

class LoadUrlCheck(
    val modified: Boolean,
    val changedUrl: String,
)
