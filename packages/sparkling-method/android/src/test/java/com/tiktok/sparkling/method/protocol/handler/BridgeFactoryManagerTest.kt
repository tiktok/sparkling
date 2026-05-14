// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.protocol.handler

import com.tiktok.sparkling.method.protocol.BridgeContext
import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import org.junit.Test
import org.mockito.Mockito

class BridgeFactoryManagerTest {
    @Test
    fun testCheckAndInitBridge() {
        val manager = BridgeFactoryManager()
        val mockBridgeContext = Mockito.mock(BridgeContext::class.java)
        val mockBridgeCall = Mockito.mock(BridgeCall::class.java)
        manager.checkAndInitBridge(mockBridgeContext, mockBridgeCall)
        // No assertion, just ensuring no exception is thrown
    }
}
