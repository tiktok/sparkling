// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.handler

import com.tiktok.sparkling.method.protocol.BridgeContext
import com.tiktok.sparkling.method.protocol.entity.BridgeCall

/**
 * Host hook for lazily creating a bridge method on the current container.
 *
 * The host overrides [checkAndInitBridge] and binds the instance on the bridge companion.
 */
open class BridgeFactoryManager {
    /** Creates and registers the named method if it is missing on this container. */
    open fun checkAndInitBridge(bridgeContext: BridgeContext, call: BridgeCall) {}
}
