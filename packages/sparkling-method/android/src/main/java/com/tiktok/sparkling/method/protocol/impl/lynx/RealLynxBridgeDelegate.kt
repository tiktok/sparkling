// Copyright (c) 2024 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.impl.lynx

import com.tiktok.sparkling.method.protocol.BridgeContext
import com.tiktok.sparkling.method.protocol.impl.monitor.BridgeSDKMonitor
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeCallback
import com.tiktok.sparkling.method.protocol.utils.BridgeConverter
import com.tiktok.sparkling.method.protocol.utils.LogUtils
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.model.context.BridgeCallThreadTypeConfig
import com.tiktok.sparkling.method.registry.core.model.context.threadStringToThreadTypeEnum
import com.tiktok.sparkling.method.registry.api.SparklingBridge
import com.lynx.react.bridge.Callback
import com.lynx.react.bridge.JavaOnlyMap
import com.lynx.react.bridge.ReadableMap
import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.protocol.entity.BridgeResult
import com.tiktok.sparkling.method.protocol.handler.BridgeThreadDispatcher
import org.json.JSONObject

/**
 *
 */
class RealLynxBridgeDelegate(
    val obj: Any?,
) {
    private val TAG = "RealLynxBridgeDelegate"

    private val bridgeThreadDispatcher: BridgeThreadDispatcher = SparklingBridge.bridgeThreadDispatcher

    fun call(
        bridgeName: String,
        params: ReadableMap? = null,
        callback: Callback? = null,
        fromEngine: String,
    ) {
        if (obj is BridgeContext) {
            val call =
                BridgeCall(obj).apply {
                    this.callbackId = "lynx"
                    this.bridgeName = bridgeName
                    this.params = params
                    this.platform = BridgeCall.PlatForm.Lynx
                    this.jsbEngine = fromEngine
                    this.url = obj.getCurrentUrl() ?: ""
                    params?.let {
                        this.timestamp = it.getLong("__timestamp", System.currentTimeMillis())

                        try {
                            val threadTypeParam =
                                if (params.getMap("data")?.hasKey("threadType") == true) {
                                    params.getMap("data").getString("threadType", "")
                                } else {
                                    params.getString("threadType", "")
                                }
                            if (threadTypeParam.isNotEmpty()) {
                                this.bridgeCallThreadType =
                                    threadStringToThreadTypeEnum(threadTypeParam)
                            }
                        } catch (e: Exception) {
                            this.jsbSDKErrorReportModel.putJsbExtension(
                                "jsb_thread_type_getter_error",
                                e.message,
                            )
                        }
                        // the params priority is higher than the BridgeCallThreadTypeConfig
                        if (this.bridgeCallThreadType == null) {
                            val threadConfig =
                                obj.sparklingBridge?.getBridgeSDKContext()?.getObject(
                                    BridgeCallThreadTypeConfig::class.java,
                                )
                            this.bridgeCallThreadType =
                                threadConfig?.getBridgeCallThreadType(this.bridgeName, this)
                        }
                    }
                }

            if (obj.defaultCallHandler.isReleased()) {
                LogUtils.d(TAG, "Bridge is released. bridgeName = $bridgeName")
                onLynxBridgeResult(
                    BridgeResult.toJsonResult(
                        IDLBridgeMethod.BRIDGE_HAS_BEEN_RELEASED,
                        "Bridge is released, please check it with container's owner.",
                    ),
                    call,
                    obj,
                    false,
                    callback,
                )
                return
            }
            LogUtils.d(TAG, "Bridge is alive. bridgeName = $bridgeName")

            obj.bridgeLifeClientImp.onBridgeCalledStart(call, obj)

            bridgeThreadDispatcher.dispatchLynxBridgeThread(call) { isInMainThread ->
                obj.dispatcher?.onDispatchBridgeMethod(
                    call,
                    object : IBridgeCallback {
                        override fun onBridgeResult(
                            result: BridgeResult,
                            call: BridgeCall?,
                            monitorBuilder: BridgeSDKMonitor.MonitorModel.Builder?,
                        ) {
                            onLynxBridgeResult(result, call, obj, isInMainThread, callback)
                        }
                    },
                    obj,
                    null,
                )
            }
        }
    }

    private fun onLynxBridgeResult(
        result: BridgeResult,
        call: BridgeCall?,
        obj: BridgeContext,
        isInMainThread: Boolean,
        callback: Callback?,
    ) {
        LogUtils.d(TAG, "onBridgeResult,result:$result,call:$call")

        call?.let { obj.bridgeLifeClientImp.onBridgeCallbackCallStart(result, it, obj) }

        try {
            val map =
                if (result.parcel is JavaOnlyMap) {
                    result.parcel
                } else if (result.parcel is JSONObject) {
                    BridgeConverter.convertJSONObject2JavaOnlyMap(result.parcel)
                } else if (result.parcel is Map<*, *>) {
                    JavaOnlyMap.from(result.parcel)
                } else {
                    JavaOnlyMap()
                }

            call?.apply {
                this.lynxCallbackMap = map
                this.isInMainThread = isInMainThread
            }
            if (call != null) {
                obj.bridgeLifeClientImp.onBridgeCallbackInvokeStart(result, call)
            }
            callback?.invoke(map)

            if (call != null) {
                obj.bridgeLifeClientImp.onBridgeCalledEnd(call, obj)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
