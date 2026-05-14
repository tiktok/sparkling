// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.hybridkit.base

import android.content.Context
import android.view.View
import com.tiktok.sparkling.hybridkit.HybridContext
import org.json.JSONObject

interface IPerformanceView {
    enum class LoadStatus(
        status: Int,
    ) {
        INIT(0),
        LOADING(1),
        SUCCESS(2),
        FAIL(3),
    }

    /**
     * The business side needs to implement this method to return the corresponding view context.
     * It will be replaced with the real context when fetching.
     */
    fun getHybridViewContext(): Context

    /**
     * The business side needs to implement this method to define the action of loadUrl.
     * The framework will actively call this method to perform the loadUrl operation before preloading.
     */
    fun loadUrl()

    /**
     * The business side needs to implement this method to perform targeted operations for data updates after fetching.
     * The framework will actively call this method when fetching.
     * @param context The context is the real context passed in when fetching.
     * @param customData The custom data for other usages
     */
    fun refreshData(
        context: Context,
        hybridContext: HybridContext? = null,
    )

    fun processAfterUseCached(hybridContext: HybridContext?)

    fun obtainHybridContext(): HybridContext?

    // update global props, for lynx it only works above version 2.3.
    fun updateGlobalPropsByIncrement(data: Map<String, Any>)

    fun sendEventByJSON(
        eventName: String,
        params: JSONObject?,
    )

    fun actualView(): View

    fun onShowEvent()

    fun onHideEvent()

    // release resources
    fun release()

    fun getPerformanceViewHybridContext(): HybridContext?

    /**
     * check if released
     */
    fun hasRelease(): Boolean
}

interface IHybridView : IPerformanceView {
    // determine whether the page is loaded successfully
    fun isLoadSuccess(): Boolean
}

enum class Theme {
    LIGHT,
    DARK,
}
