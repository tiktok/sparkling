// Copyright (c) 2023 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol

import com.lynx.react.bridge.JavaOnlyMap
import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.protocol.entity.BridgeResult
import com.tiktok.sparkling.method.protocol.impl.monitor.MonitorEntity
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeLifeClient
import com.tiktok.sparkling.method.protocol.interfaces.ShouldHandleBridgeCallResultModel
import com.tiktok.sparkling.method.protocol.utils.BridgeConstants
import com.tiktok.sparkling.method.protocol.utils.BridgeConverter
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The life client a container holds: it passes every callback on to the clients registered with it,
 * in registration order, and lets the first client that refuses a call decide its result.
 *
 * A client is registered at most once, and a client may register while the list is being visited.
 * Each call's result is also reported to the container's [BridgeContext.bridgeMonitors].
 */
class DefaultBridgeLifeClientImp(bridgeContext: BridgeContext) : IBridgeLifeClient() {

    private val bridgeLifeClients = CopyOnWriteArrayList<IBridgeLifeClient>()

    /**
     * The registered clients in registration order, for a host that has to visit them itself.
     *
     * TikTok does, for [shouldHandleBridgeCall]: it reports which client refused a call by an id
     * this base type does not have.
     */
    val registeredClients: List<IBridgeLifeClient>
        get() = bridgeLifeClients

    fun registerIBridgeLifeClient(bridgeLifeClient: IBridgeLifeClient) {
        bridgeLifeClients.addIfAbsent(bridgeLifeClient)
    }

    override fun shouldHandleBridgeCall(
        call: BridgeCall,
        bridgeContext: BridgeContext
    ): ShouldHandleBridgeCallResultModel {
        bridgeLifeClients.forEach {
            val currentHandleModel = it.shouldHandleBridgeCall(call, bridgeContext)
            if (!currentHandleModel.shouldHandleBridgeCall) {
                return currentHandleModel
            }
        }
        return ShouldHandleBridgeCallResultModel(true, null)
    }

    override fun onBridgeCalledStart(call: BridgeCall, bridgeContext: BridgeContext) {
        bridgeLifeClients.forEach {
            it.onBridgeCalledStart(call, bridgeContext)
        }
    }

    override fun onBridgeImplHandleStart(call: BridgeCall?, bridgeContext: BridgeContext) {
        bridgeLifeClients.forEach {
            it.onBridgeImplHandleStart(call, bridgeContext)
        }
    }

    override fun onBridgeImplHandleEnd(call: BridgeCall?, bridgeContext: BridgeContext) {
        bridgeLifeClients.forEach {
            it.onBridgeImplHandleEnd(call, bridgeContext)
        }
    }

    override fun onBridgeCallbackCallStart(
        result: BridgeResult,
        call: BridgeCall,
        mContext: BridgeContext
    ) {
        bridgeLifeClients.forEach {
            it.onBridgeCallbackCallStart(result, call, mContext)
        }
    }

    override fun onBridgeCallbackInvokeStart(result: BridgeResult, call: BridgeCall) {
        bridgeLifeClients.forEach {
            it.onBridgeCallbackInvokeStart(result, call)
        }
        notifyBridgeMonitors(result, call)
    }

    override fun onBridgeCalledEnd(call: BridgeCall, bridgeContext: BridgeContext) {
        bridgeLifeClients.forEach {
            it.onBridgeCalledEnd(call, bridgeContext)
        }
    }

    override fun onBridgeEventStart(eventName: String, data: Any?) {
        bridgeLifeClients.forEach {
            it.onBridgeEventStart(eventName, data)
        }
    }

    override fun onBridgeEventEnd(eventName: String, data: Any?) {
        bridgeLifeClients.forEach {
            it.onBridgeEventEnd(eventName, data)
        }
    }

    private fun notifyBridgeMonitors(result: BridgeResult, call: BridgeCall) {
        val monitors = call.context.bridgeMonitors
        if (monitors.isEmpty()) {
            return
        }
        val json = result.toJSONObject()
        val entity = MonitorEntity().apply {
            name = call.bridgeName
            url = call.url
            beginTime = call.timestamp
            endTime = System.currentTimeMillis()
            code = json.opt("code") as? Int
            message = json.optString("msg")
            rawResult = json
            rawRequest = call.invocationJson
                ?: rawRequestOrNull(call.rawReq)
                ?: lynxParamsToJson(call.params)
            hitBusinessHandler = call.hitBusinessHandler
            nameSpace = call.nameSpace
            isRunInMainThread = call.isInMainThread
        }
        val resolved = entity.code == BridgeConstants.BRIDGE_CALL_SUCCESS
        monitors.forEach { monitor ->
            runCatching {
                if (resolved) {
                    monitor.onBridgeResolved(entity)
                } else {
                    monitor.onBridgeRejected(entity)
                }
            }
        }
    }

    private fun rawRequestOrNull(rawReq: String): JSONObject? {
        if (rawReq.isEmpty()) {
            return null
        }
        return runCatching { JSONObject(rawReq) }.getOrNull()
    }

    private fun lynxParamsToJson(params: Any?): JSONObject? {
        val map = params as? JavaOnlyMap ?: return null
        return BridgeConverter.revertJavaOnlyMap2JSONObject(map)
    }
}
