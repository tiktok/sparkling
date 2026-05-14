// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.interfaces

import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.protocol.entity.BridgeResult
import org.json.JSONObject

interface IBridgeClient {
    fun shouldInterceptRequest(call: BridgeCall): BridgeResult?

    fun onBridgeInvoked(
        protocol: IBridgeProtocol,
        detail: JSONObject,
    )

    fun onBridgeDispatched(call: BridgeCall)

    fun onBridgeResultReceived(
        name: String,
        handler: IBridgeHandler,
        detail: JSONObject,
    )

    fun onBridgeCallback()

    fun onBridgeRejected()
}
