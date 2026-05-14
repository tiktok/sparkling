// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.impl.interceptor

import com.tiktok.sparkling.method.protocol.BridgeContext
import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.protocol.entity.BridgeResult
import org.json.JSONObject

/**
 * use for JSB Mock, only invoke in Debug mode
 */
open class BridgeMockInterceptor {
    open fun interceptBridgeCall(originCall: BridgeCall): BridgeCall = originCall

    open fun interceptBridgeResult(
        jsbCall: BridgeCall,
        originResult: BridgeResult,
    ): BridgeResult = originResult

    /**
     * skip real jsb call and invoke callback to FE when the BridgeResult != null
     */
    open fun invokeBridgeResult(originCall: BridgeCall): BridgeResult? = null

    open fun interceptJSBEvent(
        context: BridgeContext,
        event: String,
        data: JSONObject?,
    ) {}
}
