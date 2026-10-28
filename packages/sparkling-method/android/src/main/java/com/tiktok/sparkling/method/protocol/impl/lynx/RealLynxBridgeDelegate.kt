// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.impl.lynx

import com.lynx.react.bridge.Callback
import com.lynx.react.bridge.JavaOnlyMap
import com.lynx.react.bridge.ReadableMap
import com.tiktok.sparkling.method.protocol.BridgeContext
import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.protocol.entity.BridgeResult
import com.tiktok.sparkling.method.protocol.entity.Platform
import com.tiktok.sparkling.method.protocol.handler.BridgeThreadDispatcher
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeCallback
import com.tiktok.sparkling.method.protocol.utils.BridgeConverter
import com.tiktok.sparkling.method.protocol.utils.LogUtils
import com.tiktok.sparkling.method.registry.api.SparklingBridge
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.model.context.BridgeCallThreadTypeConfig
import com.tiktok.sparkling.method.registry.core.model.context.threadStringToThreadTypeEnum
import org.json.JSONObject

/**
 * Lynx JSB entry of the minimum runtime: build a [BridgeCall], dispatch it, and convert the result
 * back to a Lynx [Callback].
 *
 * A host adds its own call fields, result logging and result markers through the protected hooks.
 */
open class RealLynxBridgeDelegate(@Volatile var obj: Any?) {
    private val TAG = "RealLynxBridgeDelegate"

    private val bridgeThreadDispatcher: BridgeThreadDispatcher = SparklingBridge.bridgeThreadDispatcher

    /** Rebinds the container context, used by embedded Lynx. */
    internal open fun setBridgeContext(bridgeContext: BridgeContext) {
        obj = bridgeContext
    }

    /** Accepts a Lynx JSB invocation and dispatches it on the configured thread. */
    fun call(
        bridgeName: String,
        params: ReadableMap? = null,
        callback: Callback? = null,
        fromEngine: String,
    ) {
        val current = obj
        if (current !is BridgeContext) {
            return
        }
        val call = createBridgeCall(current, bridgeName, params, fromEngine)
        if (current.defaultCallHandler.isReleased()) {
            LogUtils.d(TAG, "Bridge is released. bridgeName = $bridgeName")
            onLynxBridgeResult(
                createReleasedResult(),
                call,
                current,
                false,
                callback,
            )
            return
        }
        LogUtils.d(TAG, "Bridge is alive. bridgeName = $bridgeName")
        current.bridgeLifeClientImp.onBridgeCalledStart(call, current)
        bridgeThreadDispatcher.dispatchLynxBridgeThread(call) { isInMainThread ->
            current.dispatcher?.onDispatchBridgeMethod(
                call,
                object : IBridgeCallback {
                    override fun onBridgeResult(result: BridgeResult, receivedCall: BridgeCall?) {
                        onLynxBridgeResult(result, receivedCall, current, isInMainThread, callback)
                    }
                },
                current,
            )
        }
    }

    /** Builds the call the dispatcher will run. Hosts may return a subclass. */
    protected open fun createBridgeCall(
        context: BridgeContext,
        bridgeName: String,
        params: ReadableMap?,
        fromEngine: String,
    ): BridgeCall {
        val call = newBridgeCall(context)
        call.callbackId = "lynx"
        call.bridgeName = bridgeName
        call.params = params
        call.platform = Platform.Lynx
        call.jsbEngine = fromEngine
        call.url = context.getCurrentUrl() ?: ""
        params?.let {
            call.timestamp = it.getLong("__timestamp", System.currentTimeMillis())
            try {
                val threadTypeParam = if (it.getMap("data")?.hasKey("threadType") == true) {
                    it.getMap("data").getString("threadType", "")
                } else {
                    it.getString("threadType", "")
                }
                if (threadTypeParam.isNotEmpty()) {
                    call.bridgeCallThreadType = threadStringToThreadTypeEnum(threadTypeParam)
                }
            } catch (e: Exception) {
                onThreadTypeParseFailed(call, e)
            }
            if (call.bridgeCallThreadType == null) {
                val threadConfig = context.sparklingBridge
                    ?.getBridgeSDKContext()
                    ?.getObject(BridgeCallThreadTypeConfig::class.java)
                call.bridgeCallThreadType = threadConfig?.getBridgeCallThreadType(call.bridgeName, call)
            }
        }
        onBridgeCallCreated(call, context, params)
        return call
    }

    /** Called when the thread type in [BridgeCall.params] cannot be read; the call keeps the default thread. */
    protected open fun onThreadTypeParseFailed(call: BridgeCall, e: Exception) {}

    /** Hosts that extend [BridgeCall] override this to allocate their subclass. */
    protected open fun newBridgeCall(context: BridgeContext): BridgeCall = BridgeCall(context)

    /** Host extras such as a secure token, applied after the shared fields. */
    protected open fun onBridgeCallCreated(
        call: BridgeCall,
        context: BridgeContext,
        params: ReadableMap?,
    ) {}

    /** Result returned once the container's handler is released. Hosts may return a subclass. */
    protected open fun createReleasedResult(): BridgeResult {
        return BridgeResult.toJsonResult(
            IDLBridgeMethod.BRIDGE_HAS_BEEN_RELEASED,
            "Bridge is released, please check it with container's owner.",
        )
    }

    protected open fun shouldLogBridgeResult(): Boolean = true

    /** Host extras written onto the Lynx result map, such as a token-check flag. */
    protected open fun decorateResultMap(map: JavaOnlyMap, call: BridgeCall?) {}

    protected open fun onLynxBridgeResult(
        result: BridgeResult,
        call: BridgeCall?,
        context: BridgeContext,
        isInMainThread: Boolean,
        callback: Callback?,
    ) {
        if (shouldLogBridgeResult()) {
            LogUtils.d(TAG, "onBridgeResult,result:$result,call:$call")
        }
        if (call != null) {
            context.bridgeLifeClientImp.onBridgeCallbackCallStart(result, call, context)
        }
        try {
            val map = if (result.parcel is JavaOnlyMap) {
                result.parcel
            } else if (result.parcel is JSONObject) {
                BridgeConverter.convertJSONObject2JavaOnlyMap(result.parcel)
            } else if (result.parcel is Map<*, *>) {
                JavaOnlyMap.from(result.parcel)
            } else {
                JavaOnlyMap()
            }
            decorateResultMap(map, call)
            call?.apply {
                lynxCallbackMap = map
                this.isInMainThread = isInMainThread
            }
            if (call != null) {
                context.bridgeLifeClientImp.onBridgeCallbackInvokeStart(result, call)
            }
            callback?.invoke(map)
            if (call != null) {
                context.bridgeLifeClientImp.onBridgeCalledEnd(call, context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
