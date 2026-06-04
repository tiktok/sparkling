// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.handler

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.tiktok.sparkling.method.protocol.BridgeContext
import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.protocol.entity.BridgeResult
import com.tiktok.sparkling.method.protocol.impl.monitor.BridgeSDKMonitor
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeCallback
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeHandler
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeMethodCallback
import com.tiktok.sparkling.method.protocol.utils.BridgeConstants
import com.tiktok.sparkling.method.protocol.utils.LogUtils
import com.tiktok.sparkling.method.registry.api.SparklingBridge
import com.tiktok.sparkling.method.registry.api.SparklingBridge.Companion.getToastSetting
import com.tiktok.sparkling.method.registry.api.SparklingMethodInvocationCenter
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import org.json.JSONObject
import java.util.UUID

class BridgeDispatcher {
    companion object {
        val TAG = "BridgeDispatcher"
        val handler = Handler(Looper.getMainLooper())
    }

    private var bridgeHandler: IBridgeHandler? = null

    fun registerHandler(handler: IBridgeHandler) {
        bridgeHandler = handler
    }

    @JvmOverloads
    fun onDispatchBridgeMethod(
        call: BridgeCall,
        callback: IBridgeCallback,
        context: BridgeContext,
        monitorBuilder: BridgeSDKMonitor.MonitorModel.Builder? = null,
    ) {
        if (context.jsbMockInterceptor != null) {
            if (!shouldInvokeResult(context, call, callback, monitorBuilder)) {
                val bridgeCall = context.jsbMockInterceptor!!.interceptBridgeCall(call)
                realDispatchBridgeMethod(bridgeCall, callback, context, monitorBuilder)
            }
        } else {
            realDispatchBridgeMethod(call, callback, context, monitorBuilder)
        }
    }

    private fun shouldInvokeResult(
        context: BridgeContext,
        call: BridgeCall,
        callback: IBridgeCallback,
        monitorBuilder: BridgeSDKMonitor.MonitorModel.Builder?,
    ): Boolean {
        val result = context.jsbMockInterceptor!!.invokeBridgeResult(call)
        result?.let {
            val invocation = MethodInvocationSnapshot.from(context, call)
            invocation.notifyStart(call)
            invocation.notifyEnd(call, it)
            callback.onBridgeResult(it, call, monitorBuilder)
            return true
        }
        return false
    }

    private fun realDispatchBridgeMethod(
        call: BridgeCall,
        callback: IBridgeCallback,
        context: BridgeContext,
        monitorBuilder: BridgeSDKMonitor.MonitorModel.Builder?,
    ) {
        LogUtils.d(TAG, "realDispatchBridgeMethod: ${Thread.currentThread()} and call is \n$call")

        val invocation = MethodInvocationSnapshot.from(context, call)
        invocation.notifyStart(call)

        SparklingBridge.bridgeFactoryManager?.checkAndInitBridge(context, call)

        val client = context.bridgeClient
        client.onBridgeDispatched(call)
        val result = client.shouldInterceptRequest(call)

        if (result != null && (
                result
                    .toJSONObject()
                    .optInt("code") == BridgeConstants.ERROR_BRIDGE_NO_AUTHORITY
            )
        ) {
            client.onBridgeCallback()
            val callbackResult = context.jsbMockInterceptor?.interceptBridgeResult(call, result) ?: result
            invocation.notifyEnd(call, callbackResult)
            callback.onBridgeResult(callbackResult, call, monitorBuilder)
            if (getToastSetting()) toastJsbError(call, result.toJSONObject(), context)
            return
        }

        val shouldHandleBridgeCallResultModel =
            context.bridgeLifeClientImp.shouldHandleBridgeCall(call, context)
        if (!shouldHandleBridgeCallResultModel.shouldHandleBridgeCall) {
            val callbackResult =
                BridgeResult.toJsonResult(
                    IDLBridgeMethod.BRIDGE_CALL_BE_INTERCEPTED,
                    IDLBridgeMethod.BRIDGE_CALL_BE_INTERCEPTED_MSG + ", reason: " + shouldHandleBridgeCallResultModel.reason,
                    null,
                )
            invocation.notifyEnd(call, callbackResult)
            callback.onBridgeResult(callbackResult, call, monitorBuilder)
            return
        }

        val callbackHandler =
            object : IBridgeMethodCallback {
                override fun onBridgeResult(parcel: Any) {
                    client.onBridgeCallback()
                    val bridgeResult =
                        context.jsbMockInterceptor?.interceptBridgeResult(call, BridgeResult(parcel))
                            ?: BridgeResult(parcel)
                    invocation.notifyEnd(call, bridgeResult)
                    callback.onBridgeResult(bridgeResult, call, monitorBuilder)
                    if (getToastSetting()) toastJsbError(call, bridgeResult.toJSONObject(), context)
                }
            }

        if (context.shouldHandleWithBusinessHandler(call)) {
            call.hitBusinessHandler = true
            context.businessCallHandler!!.handle(context, call, callbackHandler)
            LogUtils.d(
                TAG,
                "[JSBHit] Business JSB Handler(${context.businessCallHandler?.nameSpace}), $call",
            )
        } else {
            call.hitBusinessHandler = false
            bridgeHandler!!.handle(context, call, callbackHandler)
            LogUtils.d(TAG, "[JSBHit] Default JSB Handler, $call")
        }
    }

    fun handleRawJSBCall(
        call: BridgeCall,
        context: BridgeContext,
        callback: IBridgeCallback,
    ) {
        runCatching {
            bridgeHandler?.handle(
                context,
                call,
                object : IBridgeMethodCallback {
                    override fun onBridgeResult(parcel: Any) {
                        callback.onBridgeResult(BridgeResult(parcel), call, null)
                    }
                },
            )
        }.onFailure {
            it.printStackTrace()
            LogUtils.i(TAG, "handleRawJSBCall ${call.bridgeName} fail, msg: ${it.message}")
        }
    }

    private fun toastJsbError(
        call: BridgeCall,
        json: JSONObject,
        context: BridgeContext,
    ) {
        val appContext = context.lynxView?.context
        appContext?.let {
            handler.post {
                if (json.length() == 0) {
                    Toast
                        .makeText(
                            it,
                            "bridgeName: ${call.bridgeName}, callback info is null, check it!",
                            Toast.LENGTH_SHORT,
                        ).show()
                }

                val code = json.optInt("code", -2345)
                if (code != -1234 && code != BridgeConstants.BRIDGE_CALL_SUCCESS) {
                    when (code) {
                        BridgeConstants.BRIDGE_CALL_FAIL -> {
                            Toast
                                .makeText(
                                    it,
                                    "${call.bridgeName}, code=$code, bridge call fail",
                                    Toast.LENGTH_SHORT,
                                ).show()
                        }

                        BridgeConstants.ERROR_BRIDGE_NO_AUTHORITY -> {
                            Toast
                                .makeText(
                                    it,
                                    "${call.bridgeName}, code=$code, no authority",
                                    Toast.LENGTH_SHORT,
                                ).show()
                        }

                        BridgeConstants.BRIDGE_NOT_FOUND -> {
                            Toast
                                .makeText(
                                    it,
                                    "${call.bridgeName}, code=$code, bridge not found",
                                    Toast.LENGTH_SHORT,
                                ).show()
                        }

                        else -> {
                            Toast
                                .makeText(
                                    it,
                                    "${call.bridgeName}, code=$code, may not success, check it.",
                                    Toast.LENGTH_SHORT,
                                ).show()
                        }
                    }
                }
                if (code == -2345) {
                    Toast
                        .makeText(
                            it,
                            "${call.bridgeName}, seems no code callback",
                            Toast.LENGTH_SHORT,
                        ).show()
                }
            }
        }
    }

    private data class MethodInvocationSnapshot(
        val id: String,
        val namespace: String?,
        val platform: BridgePlatformType,
        val startTimeMs: Long,
    ) {
        fun notifyStart(call: BridgeCall) {
            SparklingMethodInvocationCenter.notifyStart(
                id = id,
                call = call,
                namespace = namespace,
                platform = platform,
                startTimeMs = startTimeMs,
            )
        }

        fun notifyEnd(
            call: BridgeCall,
            result: BridgeResult,
        ) {
            val endTimeMs = System.currentTimeMillis()
            SparklingMethodInvocationCenter.notifyEnd(
                id = id,
                call = call,
                namespace = namespace,
                platform = platform,
                startTimeMs = startTimeMs,
                endTimeMs = endTimeMs,
                result = result.parcel,
                code =
                    result
                        .toJSONObject()
                        .takeIf { it.has(IDLBridgeMethod.PARAM_CODE) }
                        ?.optInt(IDLBridgeMethod.PARAM_CODE),
            )
        }

        companion object {
            fun from(
                context: BridgeContext,
                call: BridgeCall,
            ): MethodInvocationSnapshot =
                MethodInvocationSnapshot(
                    id = UUID.randomUUID().toString(),
                    namespace = call.nameSpace.takeIf { it.isNotBlank() } ?: context.getNamespace() ?: "bridge-dispatcher",
                    platform = BridgeContext.getPlatformByBridgeContext(context),
                    startTimeMs = System.currentTimeMillis(),
                )
        }
    }
}
