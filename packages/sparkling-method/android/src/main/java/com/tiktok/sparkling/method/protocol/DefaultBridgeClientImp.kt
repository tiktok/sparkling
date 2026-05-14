// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol

import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.protocol.entity.BridgeResult
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeClient
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeHandler
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeProtocol
import com.tiktok.sparkling.method.protocol.utils.LogUtils
import org.json.JSONObject

/**
 *  default implementation
 */
class DefaultBridgeClientImp(
    val bridgeContext: BridgeContext,
) : IBridgeClient {
    private val TAG = "DefaultBridgeClientImp"

    override fun shouldInterceptRequest(call: BridgeCall): BridgeResult? {
        // TODO: implement by injection
        return null
    }

    override fun onBridgeInvoked(
        protocol: IBridgeProtocol,
        detail: JSONObject,
    ) {
        // FIXME pending
    }

    override fun onBridgeDispatched(call: BridgeCall) {
        LogUtils.e(TAG, "onBridgeCallback: bridgeName: ${call.bridgeName}")
    }

    override fun onBridgeResultReceived(
        name: String,
        handler: IBridgeHandler,
        detail: JSONObject,
    ) {
        LogUtils.e(TAG, "onBridgeCallback: bridgeName: $name")
    }

    override fun onBridgeCallback() {
        LogUtils.e(TAG, "onBridgeCallback")
    }

    override fun onBridgeRejected() {
        LogUtils.e(TAG, "onBridgeRejected")
    }
}
