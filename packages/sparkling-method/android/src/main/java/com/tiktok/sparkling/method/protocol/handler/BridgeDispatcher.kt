// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.handler

import com.tiktok.sparkling.method.protocol.BridgeContext
import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.protocol.entity.BridgeResult
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeCallback
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeHandler
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeMethodCallback
import com.tiktok.sparkling.method.protocol.utils.BridgeConstants
import com.tiktok.sparkling.method.protocol.utils.LogUtils
import com.tiktok.sparkling.method.registry.api.SparklingBridge
import com.tiktok.sparkling.method.registry.api.SparklingMethodInvocationCenter
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import java.util.UUID

open class BridgeDispatcher {
    companion object {
        val TAG = "BridgeDispatcher"
    }

    protected var bridgeHandler: IBridgeHandler? = null

    /** Binds the handler that resolves and invokes a registered bridge method. */
    fun registerHandler(handler: IBridgeHandler) {
        bridgeHandler = handler
    }

    /**
     * Shared JSB entry. When the context has a mock interceptor, a mocked result answers the call
     * without running it; otherwise the call the interceptor returns is the one dispatched.
     */
    open fun onDispatchBridgeMethod(
        call: BridgeCall,
        callback: IBridgeCallback,
        context: BridgeContext,
    ) {
        val mockInterceptor = context.jsbMockInterceptor
        if (mockInterceptor == null) {
            dispatchBridgeMethod(call, callback, context)
            return
        }
        if (invokeMockResult(call, callback, context)) {
            return
        }
        dispatchBridgeMethod(mockInterceptor.interceptBridgeCall(call), callback, context)
    }

    /**
     * Answers [call] with the mock interceptor's result if it has one, and returns whether it did.
     * Only called when the context has a mock interceptor.
     */
    protected open fun invokeMockResult(
        call: BridgeCall,
        callback: IBridgeCallback,
        context: BridgeContext,
    ): Boolean {
        val mockResult = context.jsbMockInterceptor?.invokeBridgeResult(call) ?: return false
        val invocation = MethodInvocationSnapshot.from(context, call)
        invocation.notifyStart(call)
        invocation.notifyEnd(call, mockResult)
        callback.onBridgeResult(mockResult, call)
        return true
    }

    /** After the mock interceptor: records the invocation for the debug tool, then dispatches. */
    protected open fun dispatchBridgeMethod(
        call: BridgeCall,
        callback: IBridgeCallback,
        context: BridgeContext,
    ) {
        LogUtils.d(TAG, "realDispatchBridgeMethod: ${Thread.currentThread()} and call is \n$call")

        val invocation = MethodInvocationSnapshot.from(context, call)
        invocation.notifyStart(call)

        // Records the end of the invocation before the result reaches the caller.
        val invocationCallback =
            object : IBridgeCallback {
                override fun onBridgeResult(
                    result: BridgeResult,
                    call: BridgeCall?,
                ) {
                    call?.let { invocation.notifyEnd(it, result) }
                    callback.onBridgeResult(result, call)
                }
            }
        dispatchAfterRegistration(call, invocationCallback, context)
    }

    /** Lazily creates the named method, then gates the call and invokes the handler. */
    protected open fun dispatchAfterRegistration(
        call: BridgeCall,
        callback: IBridgeCallback,
        context: BridgeContext,
    ) {
        SparklingBridge.bridgeFactoryManager?.checkAndInitBridge(context, call)
        if (shouldRejectDispatch(call, callback, context)) {
            return
        }
        dispatchToBridgeHandler(call, callback, context)
    }

    /**
     * Gate after the factory hook: asks the context's bridge client, then its
     * [BridgeContext.bridgeLifeClientImp], whether to run the call. The default bridge client lets
     * every call through.
     *
     * Return true if this call already produced a result and must not reach [dispatchToBridgeHandler].
     */
    protected open fun shouldRejectDispatch(
        call: BridgeCall,
        callback: IBridgeCallback,
        context: BridgeContext,
    ): Boolean {
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
            callback.onBridgeResult(callbackResult, call)
            return true
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
            callback.onBridgeResult(callbackResult, call)
            return true
        }
        return false
    }

    /**
     * Invokes the context's business handler when it serves [call], [bridgeHandler] otherwise, and
     * records which one in [BridgeCall.hitBusinessHandler]. Fails the call if that handler is unset.
     */
    protected open fun dispatchToBridgeHandler(
        call: BridgeCall,
        callback: IBridgeCallback,
        context: BridgeContext,
    ) {
        val jsbHandler = if (context.shouldHandleWithBusinessHandler(call)) {
            call.hitBusinessHandler = true
            context.businessCallHandler
        } else {
            call.hitBusinessHandler = false
            bridgeHandler
        }
        if (jsbHandler == null) {
            callback.onBridgeResult(
                createBridgeResult(
                    BridgeResult.newResultJson(IDLBridgeMethod.UNKNOWN_ERROR, "JSB handler is empty", null),
                ),
                call,
            )
            return
        }
        jsbHandler.handle(context, call, createMethodCallback(call, callback, context))
        if (call.hitBusinessHandler) {
            LogUtils.d(TAG, "[JSBHit] Business JSB Handler(${context.businessCallHandler?.nameSpace}), $call")
        } else {
            LogUtils.d(TAG, "[JSBHit] Default JSB Handler, $call")
        }
    }

    /**
     * The callback [dispatchToBridgeHandler] hands the handler: it passes each result, as rewritten
     * by the mock interceptor if any, on to [callback]. The bridge client is notified first. A host
     * overrides it to do its own work around each result.
     */
    protected open fun createMethodCallback(
        call: BridgeCall,
        callback: IBridgeCallback,
        context: BridgeContext,
    ): IBridgeMethodCallback = object : IBridgeMethodCallback {
        override fun onBridgeResult(parcel: Any) {
            context.bridgeClient.onBridgeCallback()
            val result = createBridgeResult(parcel)
            val bridgeResult = context.jsbMockInterceptor?.interceptBridgeResult(call, result) ?: result
            callback.onBridgeResult(bridgeResult, call)
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
                        callback.onBridgeResult(createBridgeResult(parcel), call)
                    }
                },
            )
        }.onFailure {
            it.printStackTrace()
            LogUtils.i(TAG, "handleRawJSBCall ${call.bridgeName} fail, msg: ${it.message}")
        }
    }

    /** The result type the host's callbacks expect to receive. */
    protected open fun createBridgeResult(parcel: Any): BridgeResult = BridgeResult(parcel)

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
