// Copyright (c) 2023 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol

import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.api.containsPiperData
import com.lynx.react.bridge.JavaOnlyMap
import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.protocol.entity.BridgeResult
import com.tiktok.sparkling.method.protocol.impl.lifecycle.fe.FeCallMonitorModel
import com.tiktok.sparkling.method.protocol.impl.monitor.MonitorEntity
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeLifeClient
import com.tiktok.sparkling.method.protocol.interfaces.ShouldHandleBridgeCallResultModel
import com.tiktok.sparkling.method.protocol.utils.BridgeConstants
import com.tiktok.sparkling.method.protocol.utils.BridgeConverter

/**
 */
class DefaultBridgeLifeClientImp(
    bridgeContext: BridgeContext,
) : IBridgeLifeClient() {
    private var bridgeLifeClients = ArrayList<IBridgeLifeClient>()

    fun registerIBridgeLifeClient(bridgeLifeClient: IBridgeLifeClient) {
        bridgeLifeClients.add(bridgeLifeClient)
    }

    override fun shouldHandleBridgeCall(
        call: BridgeCall,
        bridgeContext: BridgeContext,
    ): ShouldHandleBridgeCallResultModel {
        // call other lifeClient
        bridgeLifeClients.forEach {
            val currentHandleModel = it.shouldHandleBridgeCall(call, bridgeContext)
            if (!currentHandleModel.shouldHandleBridgeCall) {
                // TODO
            }
        }
        return ShouldHandleBridgeCallResultModel(true, null)
    }

    override fun onBridgeCalledStart(
        bridgeCall: BridgeCall,
        bridgeContext: BridgeContext,
    ) {
        val endConvertParamsTime = System.currentTimeMillis()
        bridgeCall.jsbSDKErrorReportModel.apply {
            setJsbBridgeSdk("SparklingBridge")
            setJsbUrl(bridgeCall.url)
            setJsbMethodName(bridgeCall.bridgeName)
            setJsbEngine(bridgeCall.jsbEngine)
            when (bridgeCall.platform) {
                BridgeCall.PlatForm.Web -> Unit

                // TODO: Web related abilities will implement later
                BridgeCall.PlatForm.Lynx -> setView(bridgeCall.context.lynxView)

                else -> Unit
            }
        }

        bridgeCall.feCallMonitorModel.view = bridgeContext.lynxView
        bridgeCall.feCallMonitorModel.jsbNativeCallStart = System.currentTimeMillis()
        bridgeCall.feCallMonitorModel.bridgeName = bridgeCall.bridgeName
        bridgeCall.feCallMonitorModel.containerID = bridgeContext.containerId

        when (bridgeCall.platform) {
            BridgeCall.PlatForm.Web -> {
                // TODO: Web related abilities will implement later
            }

            else -> {
                (bridgeCall.params as? JavaOnlyMap)?.let { params ->
                    (params?.getMap("data") ?: params)
                        .getMap(FeCallMonitorModel._JSB_CALLER_INFO)
                        ?.asHashMap()
                        ?.forEach {
                            bridgeCall.feCallMonitorModel.addCategory(
                                "${FeCallMonitorModel.CALLER_PREFIX}${it.key}",
                                it.value,
                            )
                        }
                    (params?.getMap("data") ?: params)
                        .getMap(FeCallMonitorModel._JSB_PERF_METRICS)
                        ?.asHashMap()
                        ?.let {
                            it.get(FeCallMonitorModel.JSB_FUNC_CALL_START).let {
                                if (it is Long) {
                                    bridgeCall.feCallMonitorModel.jsbFuncCallStart = it
                                }
                            }
                            it.get(FeCallMonitorModel.JSB_FUNC_CALL_END).let {
                                if (it is Long) {
                                    bridgeCall.feCallMonitorModel.jsbFuncCallEnd = it
                                }
                            }
                        }
                }
            }
        }

        // call other lifeClient
        bridgeLifeClients.forEach {
            it.onBridgeCalledStart(bridgeCall, bridgeContext)
        }
    }

    override fun onBridgeImplHandleStart(
        call: BridgeCall?,
        bridgeContext: BridgeContext,
    ) {
        // call other lifeClient
        bridgeLifeClients.forEach {
            it.onBridgeImplHandleStart(call, bridgeContext)
        }
    }

    override fun onBridgeImplHandleEnd(
        call: BridgeCall?,
        bridgeContext: BridgeContext,
    ) {
        // call other lifeClient
        bridgeLifeClients.forEach {
            it.onBridgeImplHandleEnd(call, bridgeContext)
        }
    }

    override fun onBridgeCallbackCallStart(
        result: BridgeResult,
        call: BridgeCall,
        mContext: BridgeContext,
    ) {
        when (call.platform) {
            BridgeCall.PlatForm.Web -> {
                // TODO
            }

            BridgeCall.PlatForm.Lynx -> {
            }

            else -> {}
        }

        // call other lifeClient
        bridgeLifeClients.forEach {
            it.onBridgeCallbackCallStart(result, call, mContext)
        }
    }

    override fun onBridgeCallbackInvokeStart(
        result: BridgeResult,
        call: BridgeCall,
    ) {
        when (call.platform) {
            BridgeCall.PlatForm.Web -> {
                // TODO
            }

            BridgeCall.PlatForm.Lynx -> {
                call.lynxCallbackMap?.let { map ->
                    val monitor =
                        MonitorEntity().apply {
                            name = call?.bridgeName
                            url = call?.url
                            beginTime = call?.timestamp
                            endTime = System.currentTimeMillis()
                            if (map.hasKey("code")) {
                                val code = map.getInt("code", IDLBridgeMethod.UNKNOWN_ERROR)
                                if (code != IDLBridgeMethod.UNKNOWN_ERROR) this.code = code
                            }
                            message = map.getString("msg", "")

                            rawResult =
                                BridgeConverter.revertJavaOnlyMap2JSONObject(map).apply {
                                    put("usePiperData", map.containsPiperData())
                                    put("bridgeCallThreadType", call.bridgeCallThreadType)
                                }
                            call.params?.let {
                                rawRequest =
                                    BridgeConverter.revertJavaOnlyMap2JSONObject(call.params as JavaOnlyMap)
                            }
                            hitBusinessHandler = call?.hitBusinessHandler ?: false
                            nameSpace = call?.nameSpace
                            isRunInMainThread = call.isInMainThread
                        }
                    if (monitor.code == BridgeConstants.BRIDGE_CALL_SUCCESS) {
                        call.context.monitor.forEach {
                            it.onBridgeResolved(monitor)
                        }
                    } else {
                        call.context.monitor.forEach {
                            it.onBridgeRejected(monitor)
                        }
                        call.jsbSDKErrorReportModel.apply {
                            setJsbErrorCode(monitor.code)
                            putJsbExtension("error_message", monitor.message)
                            putJsbExtension("bridgeCallThreadType", call.bridgeCallThreadType)
                        }
                        call.jsbSDKErrorReportModel.reportJSBErrorModel(call.context.errorReportModel)
                    }
                }
            }

            else -> {}
        }

        // call other lifeClient
        bridgeLifeClients.forEach {
            it.onBridgeCallbackInvokeStart(result, call)
        }

        call.feCallMonitorModel.jsbCallbackStart = System.currentTimeMillis()
    }

    override fun onBridgeCalledEnd(
        call: BridgeCall,
        bridgeContext: BridgeContext,
    ) {
        when (call.platform) {
            BridgeCall.PlatForm.Web -> {
                // TODO
            }

            else -> {}
        }

        // call other lifeClient
        bridgeLifeClients.forEach {
            it.onBridgeCalledEnd(call, bridgeContext)
        }
        call.feCallMonitorModel.jsbCallbackEnd = System.currentTimeMillis()
        call.feCallMonitorModel.reportFeCallInfo()
    }

    // Event
    override fun onBridgeEventStart(
        eventName: String,
        data: Any?,
    ) {
        // call other lifeClient
        bridgeLifeClients.forEach {
            it.onBridgeEventStart(eventName, data)
        }
    }

    override fun onBridgeEventEnd(
        eventName: String,
        data: Any?,
    ) {
        // call other lifeClient
        bridgeLifeClients.forEach {
            it.onBridgeEventEnd(eventName, data)
        }
    }
}
