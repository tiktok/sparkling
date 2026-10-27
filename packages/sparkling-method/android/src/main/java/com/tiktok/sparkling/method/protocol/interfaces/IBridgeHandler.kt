// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.interfaces

import com.tiktok.sparkling.method.protocol.BridgeContext
import com.tiktok.sparkling.method.protocol.entity.BridgeCall

/**
 * Dispatch boundary between the bridge protocol and the method registry that resolves and invokes
 * a call, reports its result through [IBridgeMethodCallback], and owns the registry lifecycle.
 *
 * Hosts can extend [BridgeContext] with platform-specific state without changing this dispatch
 * contract.
 */
interface IBridgeHandler {
    fun handle(
        bridgeContext: BridgeContext,
        call: BridgeCall,
        callback: IBridgeMethodCallback
    )

    fun onRelease()

    /**
     * for check release status
     */
    fun isReleased(): Boolean
}
