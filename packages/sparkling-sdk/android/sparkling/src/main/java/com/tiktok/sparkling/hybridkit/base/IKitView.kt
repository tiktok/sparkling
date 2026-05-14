// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.base

import android.content.Context
import android.view.View
import androidx.annotation.CallSuper
import com.tiktok.sparkling.hybridkit.HybridContext
import com.tiktok.sparkling.hybridkit.api.iterator
import com.tiktok.sparkling.hybridkit.scheme.HybridSchemeParam
import org.json.JSONObject

interface IKitView {
    /**
     * Kit context
     */
    var hybridContext: HybridContext

    /**
     * get real KitView
     */
    fun realView(): View?

    /**
     * set default value as true to release cache storage
     */
    fun readyToSendEvent(): Boolean = true

    fun load()

    /**
     * load template/webview
     */
    fun load(uri: String)

    /**
     * reload itself
     */
    fun reload()

    fun refreshSchemeParam(hybridSchemeParam: HybridSchemeParam) {}

    fun refreshContext(context: Context) {}

    /**
     * send event
     * @params This parameter will be sent directly to lynx.
     */
    @Deprecated("deprecated, replace with sendEventByJSON", ReplaceWith("sendEventByJSON"))
    @CallSuper
    fun sendEvent(
        eventName: String,
        params: List<Any>?,
    ) {
        hybridContext.sendEventListener?.let { it(this, eventName, params) }
        hybridContext.absSendEventListener?.iterator { it(this, eventName, params) }
    }

    /**
     * send event
     */
    @CallSuper
    fun sendEventByJSON(
        eventName: String,
        params: JSONObject?,
    ) {
        hybridContext.sendEventListener?.let { it(this, eventName, params) }
        hybridContext.absSendEventListener?.iterator { it(this, eventName, params) }
    }

    /**
     * send event
     * This method doesn't work in Lynx page, please use sendEventByJSON instead.
     */
    @CallSuper
    fun sendEventByMap(
        eventName: String,
        params: Map<String, Any?>?,
    ) {
        hybridContext.sendEventListener?.let { it(this, eventName, params) }
        hybridContext.absSendEventListener?.iterator { it(this, eventName, params) }
    }

    /**
     * update date, the main purpose of adding to the interface is to decouple the plug-in scene and LynxView
     */
    fun updateData(data: Map<String, Any>) {}

    fun updateDataByJson(data: String) {}

    fun updateDataWithExtra(
        data: String,
        extra: Map<String, Any>,
    ) {}

    fun updateDataWithExtra(
        dataList: List<String>,
        extra: Map<String, Any>?,
    ) {}

    /**
     * clear current data and update with the given data in LynxView
     */
    fun resetData(data: Map<String, Any>) {}

    fun resetDataByJson(data: String) {}

    fun resetDataWithExtra(
        data: String,
        extra: Map<String, Any>,
    ) {}

    fun resetDataWithExtra(
        dataList: List<String>,
        extra: Map<String, Any>?,
    ) {}

    // update global props by increment, for lynx it only works above version 2.3.
    fun updateGlobalPropsByIncrement(data: Map<String, Any>) {}

    fun getCurrentData(callback: IGetDataCallback?) {}

    /**
     * show LynxView, the business side needs to call
     */
    fun onShow()

    /**
     * hide LynxView, the business side needs to call
     */
    fun onHide()

    /**
     * destroy LynxView, the business side must call, normally clearContext need to be true, if it is destroyed by accident clearContext needs to be false.
     */
    fun destroy(clearContext: Boolean = true)

    fun destroyWhenJSRuntimeCallback() {}

    /**
     * @return Boolean
     */
    fun hasDestroyed(): Boolean

    /**
     * return globalprops
     */
    fun getGlobalProps(): MutableMap<String, Any>?

    fun getScheme(): String?

    fun onLoadSuccess()

    fun getAndRemoveForestResponse(): Any? = null
}

interface IGetDataCallback {
    fun onSuccess(data: HashMap<String, Any>?)

    fun onFail(msg: String?)
}
