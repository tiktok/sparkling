// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.protocol.handler

import com.tiktok.sparkling.method.protocol.BridgeContext
import com.tiktok.sparkling.method.protocol.entity.BridgeCall

/**
 * @Author: linshuhao
 * @Time: 2022/5/12 15:31
 * @Description:
 */
open class BridgeFactoryManager {
    open fun checkAndInitBridge(
        bridgeContext: BridgeContext,
        call: BridgeCall,
    ) {}
}
