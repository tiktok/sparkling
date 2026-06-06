// Copyright (c) 2023 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.interfaces

import com.tiktok.sparkling.method.protocol.BridgeContext
import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.protocol.entity.BridgeResult

/**
 * when shouldHandleBridgeCall is false, you should give a reason to explain it
 */
class ShouldHandleBridgeCallResultModel(
    val shouldHandleBridgeCall: Boolean,
    val reason: String?,
)

abstract class IBridgeLifeClient {
    /**
     * if return false, the jsb call will be intercepted, and will callback to FE
     * with error code : -10 BRIDGE_CALL_BE_INTERCEPTED and message : "intercepted by lifeClient"
     */
    open fun shouldHandleBridgeCall(
        call: BridgeCall,
        bridgeContext: BridgeContext,
    ): ShouldHandleBridgeCallResultModel = ShouldHandleBridgeCallResultModel(true, null)

    /**
     * you can register jsb in there
     */
    open fun onBridgeCalledStart(
        call: BridgeCall,
        bridgeContext: BridgeContext,
    ) {}

    open fun onBridgeImplHandleStart(
        call: BridgeCall?,
        bridgeContext: BridgeContext,
    ) {}

    open fun onBridgeImplHandleEnd(
        call: BridgeCall?,
        bridgeContext: BridgeContext,
    ) {}

    open fun onBridgeCallbackCallStart(
        result: BridgeResult,
        call: BridgeCall,
        mContext: BridgeContext,
    ) {}

    open fun onBridgeCallbackInvokeStart(
        result: BridgeResult,
        call: BridgeCall,
    ) {}

    open fun onBridgeCalledEnd(
        call: BridgeCall,
        bridgeContext: BridgeContext,
    ) {}

    /**
     * data is any.
     * when it is webview, data will be a JSONObject
     * when it is lynxview, data will be JavaOnlyArray
     * you can change data in onBridgeEventBegin.
     */
    open fun onBridgeEventStart(
        eventName: String,
        data: Any?,
    ) {}

    open fun onBridgeEventEnd(
        eventName: String,
        data: Any?,
    ) {}
}
