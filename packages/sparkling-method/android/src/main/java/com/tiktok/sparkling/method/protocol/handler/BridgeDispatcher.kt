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
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.api.SparklingBridge
import com.tiktok.sparkling.method.registry.api.SparklingBridge.Companion.getToastSetting
import org.json.JSONObject

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
            if (!shouldInvokeResult(context, call, callback)) {
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
    ): Boolean {
        val result = context.jsbMockInterceptor!!.invokeBridgeResult(call)
        result?.let {
            callback.onBridgeResult(it, call, null)
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
            callback.onBridgeResult(
                context.jsbMockInterceptor?.interceptBridgeResult(call, result) ?: result,
                call,
                monitorBuilder,
            )
            if (getToastSetting()) toastJsbError(call, result.toJSONObject(), context)
            return
        }

        val shouldHandleBridgeCallResultModel =
            context.bridgeLifeClientImp.shouldHandleBridgeCall(call, context)
        if (!shouldHandleBridgeCallResultModel.shouldHandleBridgeCall) {
            callback.onBridgeResult(
                BridgeResult.toJsonResult(
                    IDLBridgeMethod.BRIDGE_CALL_BE_INTERCEPTED,
                    IDLBridgeMethod.BRIDGE_CALL_BE_INTERCEPTED_MSG + ", reason: " + shouldHandleBridgeCallResultModel.reason,
                    null,
                ),
                call,
                monitorBuilder,
            )
            return
        }

        val callbackHandler =
            object : IBridgeMethodCallback {
                override fun onBridgeResult(parcel: Any) {
                    client.onBridgeCallback()
                    val result =
                        context.jsbMockInterceptor?.interceptBridgeResult(call, BridgeResult(parcel))
                            ?: BridgeResult(parcel)
                    callback.onBridgeResult(result, call, monitorBuilder)
                    if (getToastSetting()) toastJsbError(call, result.toJSONObject(), context)
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
}
