// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.lynx

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import com.lynx.react.bridge.JavaOnlyArray
import com.lynx.react.bridge.JavaOnlyMap
import com.lynx.tasm.LynxGetDataCallback
import com.lynx.tasm.LynxView
import com.lynx.tasm.LynxViewBuilder
import com.lynx.tasm.TemplateData
import com.tiktok.sparkling.hybridkit.HybridContext
import com.tiktok.sparkling.hybridkit.KitViewManager
import com.tiktok.sparkling.hybridkit.base.IGetDataCallback
import com.tiktok.sparkling.hybridkit.base.IHybridKitLifeCycle
import com.tiktok.sparkling.hybridkit.base.IKitView
import com.tiktok.sparkling.hybridkit.utils.GlobalPropsUtils
import com.tiktok.sparkling.hybridkit.utils.LogLevel
import com.tiktok.sparkling.hybridkit.utils.LogUtils
import com.tiktok.sparkling.hybridkit.utils.ViewEventUtils
import org.json.JSONObject

@SuppressLint("ViewConstructor")
class SimpleLynxKitView : LynxView, IKitView {
    companion object {
        private const val TAG = "SimpleLynxKitView"
    }

    override var hybridContext: HybridContext
    var lynxKitLifeCycle: IHybridKitLifeCycle? = null
    var rawUrl: String? = null
    var lynxKitInitParams: LynxKitInitParams? = null
    private var hasDestroyed = false


    constructor(
        context: Context,
        hybridContext: HybridContext,
        builder: LynxViewBuilder,
        lynxKitInitParams: LynxKitInitParams?,
        lifeCycle: IHybridKitLifeCycle?
    ) : super(context, builder) {
        this.hybridContext = hybridContext
        this.lynxKitLifeCycle = lifeCycle
        addLynxViewClient(SimpleLynxViewClient(this, this.lynxKitLifeCycle))
        KitViewManager.addKitView(this)
        rawUrl = hybridContext.hybridSchemeParam?.bundle
    }

    override fun realView(): View? {
        return this
    }

    override fun load() {
        rawUrl?.let { load(it) } ?: {
            lynxKitLifeCycle?.onLoadFailed(this, "", "bundle path is null")
        }
    }

    override fun load(uri: String) {
        lynxKitLifeCycle?.onLoadStart(this, uri)
        if (uri.isEmpty()) {
            lynxKitLifeCycle?.onLoadFailed(this, "", "uri is null")
        }
        rawUrl = uri
        this.renderTemplateUrl(uri, hybridContext.initData())
        updateGlobalProps(GlobalPropsUtils.instance.getGlobalProps(hybridContext.containerId))
        lynxKitLifeCycle?.onLoadFinish(this)
    }

    override fun reload() {
        hybridContext.tryResetTemplateResData(System.currentTimeMillis())
        lynxKitLifeCycle?.onLoadStart(this, rawUrl ?: "")
        lynxKitInitParams?.obtainGlobalProps()?.let {
            updateData(it)
        }
        rawUrl?.let {
            load(it)
        }
    }

    override fun updateData(data: Map<String, Any>) {
        super<LynxView>.updateData(data)
    }

    override fun updateData(json: String?, processorName: String?) {
        val templateData = TemplateData.fromString(json)
        templateData.markState(processorName)
        templateData.markReadOnly()
        updateData(templateData)
    }

    override fun updateDataByJson(data: String) = updateData(data, null)

    override fun updateDataWithExtra(data: String, extra: Map<String, Any>){
        val templateData = TemplateData.fromString(data)
        extra.entries.forEach {
            templateData.put(it.key, it.value)
        }
        templateData.markReadOnly()
        updateData(templateData)
    }

    override fun updateDataWithExtra(dataList: List<String>, extra: Map<String, Any>?) {
        if (dataList.isEmpty()) {
            return
        }
        var templateData: TemplateData? = null
        dataList.forEachIndexed { index, data ->
            if (index == 0) {
                templateData = TemplateData.fromString(data)
            } else {
                val newData = TemplateData.fromString(data)
                templateData?.updateWithTemplateData(newData)
            }
        }
        templateData?.apply {
            extra?.entries?.forEach {
                this.put(it.key, it.value)
            }
            this.markReadOnly()
        }
        updateData(templateData)
    }

    override fun resetData(data: Map<String, Any>) {
        resetData(TemplateData.fromMap(data))
    }

    override fun resetDataByJson(data: String) {
        resetData(TemplateData.fromString(data))
    }

    override fun resetDataWithExtra(data: String, extra: Map<String, Any>) {
        val templateData = TemplateData.fromString(data)
        data.apply {
            extra.entries.forEach {
                templateData.put(it.key, it.value)
            }
        }
        resetData(templateData)
    }

    override fun resetDataWithExtra(dataList: List<String>, extra: Map<String, Any>?) {
        if (dataList.isEmpty()) {
            return
        }
        var templateData: TemplateData? = null
        dataList.forEachIndexed { index, data ->
            if (index == 0) {
                templateData = TemplateData.fromString(data)
            } else {
                val newData = TemplateData.fromString(data)
                templateData?.updateWithTemplateData(newData)
            }
        }
        extra?.entries?.forEach {
            templateData?.put(it.key, it.value)
        }
        resetData(templateData)
    }


    override fun onShow() {
        ViewEventUtils.onShow(hybridContext)
        onEnterForeground()
    }

    override fun onHide() {
        ViewEventUtils.onPause(hybridContext)
        onEnterBackground()
    }

    override fun destroy(clearContext: Boolean) {
        super.destroy()
        ViewEventUtils.onDestroy(hybridContext)
        hasDestroyed = true
    }

    override fun destroyWhenJSRuntimeCallback() {
        hybridContext.bridge?.release()
        lynxKitLifeCycle?.onDestroy(this)
        GlobalPropsUtils.instance.flushGlobalProps(hybridContext.containerId)
        KitViewManager.removeKitView(hybridContext.containerId)
    }

    override fun hasDestroyed(): Boolean {
        return hasDestroyed
    }

    override fun getGlobalProps(): MutableMap<String, Any>? {
        return hybridContext.globalProps
    }

    override fun getScheme(): String? {
        return hybridContext.getFullScheme()
    }

    override fun onLoadSuccess() {
        lynxKitLifeCycle?.onLoadFinish(this)
    }

    override fun updateData(data: TemplateData?) {
        super<LynxView>.updateData(data)
    }

    override fun sendEvent(eventName: String, params: List<Any>?) {
        super.sendEvent(eventName, params)
        sendGlobalEventInternal(eventName, params)
    }

    override fun sendEventByJSON(eventName: String, params: JSONObject?) {
        super.sendEventByJSON(eventName, params)
        hybridContext.bridge?.sendEvent(eventName, params)
    }

    private fun sendGlobalEventInternal(eventName: String, params: List<Any>?) {
        val data = if (params != null) {
            JavaOnlyArray.from(params)
        } else {
            JavaOnlyArray()
        }
        sendGlobalEvent(eventName, data)
    }

    override fun updateGlobalPropsByIncrement(data: Map<String, Any>) {
        runCatching {
            updateGlobalProps(data)
            lynxKitInitParams?.setGlobalProps(data)
            sendEventByJSON("globalPropsUpdated", null)
        }.onFailure {
            LogUtils.printLog("updateGlobalPropsByIncrement failed, error = ${it.message}", LogLevel.E, TAG)
        }
    }

    override fun getCurrentData(callback: IGetDataCallback?) {
        this.getCurrentData(object:LynxGetDataCallback{
            override fun onSuccess(data: JavaOnlyMap?) {
                callback?.onSuccess(data as HashMap<String, Any>?)
            }

            override fun onFail(msg: String?) {
                callback?.onFail(msg)
            }
        })
    }


}