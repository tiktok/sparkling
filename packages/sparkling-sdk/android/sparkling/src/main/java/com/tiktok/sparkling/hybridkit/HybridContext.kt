// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import com.tiktok.sparkling.hybridkit.base.IKitInitParam
import com.tiktok.sparkling.hybridkit.base.IKitView
import com.tiktok.sparkling.hybridkit.base.Theme
import com.tiktok.sparkling.hybridkit.config.RuntimeInfo
import com.tiktok.sparkling.hybridkit.event.AbsSendEventListener
import com.tiktok.sparkling.hybridkit.event.SendEventListener
import com.tiktok.sparkling.hybridkit.scheme.HybridSchemeParam
import com.tiktok.sparkling.method.registry.api.SparklingBridge
import kotlinx.coroutines.Deferred
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

open class HybridContext {
    var containerId = generateID()
    var scheme: String? = null

    var extra: Map<String, String>? = null

    var bundle: Bundle? = null

    var fullScheme: String? = null
    var templateResData: JSONObject = JSONObject()
    var runtimeInfo: RuntimeInfo = RuntimeInfo()
    var hybridSchemeParam: HybridSchemeParam? = null
    val globalProps = mutableMapOf<String, Any>()
    var hybridParams: IKitInitParam? = null
    private var lynxViewConfig: MutableMap<String, String>? = null
    open var sendEventListener: SendEventListener? = null
    open var absSendEventListener: AbsSendEventListener? = null

    private var initData: String? = null
    // init_data_url works only Lynx
    var initDataUrlDeferred: Deferred<Map<String, Any>?>? = null
    private val initDataRes = AtomicReference<Map<String, Any>?>(null)

    var bridge: SparklingBridge? = null

    fun initData() : String? = initData

    fun withInitData(initData: String){
        this.initData = initData
    }

    private fun generateID(): String {
        return System.currentTimeMillis().toString() + "-" + UUID.randomUUID().toString()
    }

    fun withLynxViewConfig(lynxViewConfig: MutableMap<String, String>?): HybridContext {
        this.lynxViewConfig = lynxViewConfig
        return this
    }

    fun withExtra(extra: Map<String, String>?): HybridContext {
        this.extra = extra
        return this
    }

    fun withBundle(bundle: Bundle?): HybridContext {
        this.bundle = bundle
        return this
    }

    fun getFullScheme(): String? = fullScheme ?: scheme

    fun withSendEventListener(sendEventListener: SendEventListener?): HybridContext {
        this.sendEventListener = sendEventListener
        return this
    }

    fun withMultiSendEventListener(absSendEventListener: AbsSendEventListener?): HybridContext {
        this.absSendEventListener = absSendEventListener
        return this
    }

    fun getLynxViewConfig() = lynxViewConfig


    /**
     * initialize template res data and append <code>container_init_cost</code> field to <code>templateResData</code>
     */
    fun tryResetTemplateResData(loadTime: Long) {
        templateResData.let {
            if (it.length() == 0) {
                // if templateResData is empty, append container_init_cost directly
                it.put("container_init_cost", loadTime)
            } else if (
                (it.length() == 1 && it.optLong("container_init_cost") == 0L) ||
                it.length() > 1
            ) {
                // if templateResData is not empty and container_init_cost not set, reset
                // templateResData and append container_init_cost
                templateResData = JSONObject().apply {
                    put("container_init_cost", loadTime)
                }
            } else
                return
        }
    }

    fun <T> putDependency(clazz: Class<T>, instance: T?) {
        HybridEnvironment.instance.putDependency(this.containerId, clazz, instance)
    }

    fun <T> getDependency(clazz: Class<T>): T? {
        return HybridEnvironment.instance.getDependency(this.containerId, clazz)
    }

    fun <T> removeDependency(clazz: Class<T>) {
        HybridEnvironment.instance.removeDependency(this.containerId, clazz)
    }

    fun <T> removeDependency(clazz: Class<T>, instance: T) {
        HybridEnvironment.instance.removeDependency(this.containerId, clazz, instance)
    }

    /**
     * remove all dependency instance associate to containerId
     * */
    fun removeAllDependencies() {
        HybridEnvironment.instance.removeDependency(containerId, true)
    }

    fun kitView(): IKitView? {
        return KitViewManager.getKitView(containerId)
    }

    fun getTheme(context: Context?): Theme {
        return when (hybridSchemeParam?.forceThemeStyle?.lowercase()) {
            "light" -> Theme.LIGHT
            "dark" -> Theme.DARK
            else -> {
                val nightModeFlags = context?.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)
                when (nightModeFlags) {
                    Configuration.UI_MODE_NIGHT_YES -> Theme.DARK
                    else -> Theme.LIGHT
                }
            }
        }
    }

    /**
     * send event to JS
     * @param params must be List, Map or JSONObject
     */
    fun sendEvent(eventName: String, params: Any?) {
        kitView()?.let {
            when (params) {
                null -> it.sendEvent(eventName, null)
                is List<*> -> it.sendEvent(eventName, params as? List<Any>)
                is JSONObject -> it.sendEventByJSON(eventName, params)
                is Map<*, *> -> it.sendEventByMap(eventName, params as? Map<String, Any?>)
            }
            return
        }
    }

}