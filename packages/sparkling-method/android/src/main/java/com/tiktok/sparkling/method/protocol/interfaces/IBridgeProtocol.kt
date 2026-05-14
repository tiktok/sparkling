// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.interfaces

import com.tiktok.sparkling.method.protocol.BridgeContext
import com.tiktok.sparkling.method.protocol.entity.BridgeCall

abstract class IBridgeProtocol(
    context: BridgeContext,
) {
    val mContext = context

    /**
     * params: JSONObject or JavaOnlyArray
     */
    abstract fun sendEvent(
        name: String,
        params: Any?,
    )

    abstract fun init()

    abstract fun createBridgeCall(msg: String): BridgeCall

    open fun sendJSRuntimeEvent(
        name: String,
        params: Any?,
    ) {}
}
