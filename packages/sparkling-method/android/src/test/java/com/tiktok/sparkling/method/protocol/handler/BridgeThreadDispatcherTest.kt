// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.protocol.handler

import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BridgeThreadDispatcherTest {
    @Test
    fun testDispatchLynxBridgeThread() {
        val dispatcher = BridgeThreadDispatcher()
        val mockBridgeCall = Mockito.mock(BridgeCall::class.java)
        dispatcher.dispatchLynxBridgeThread(mockBridgeCall) { _ -> }
        // This will run on the main looper, so we can't directly assert here.
        // We can only ensure no exception is thrown.
    }

    @Test
    fun testDispatchWebBridgeThread() {
        val dispatcher = BridgeThreadDispatcher()
        val mockBridgeCall = Mockito.mock(BridgeCall::class.java)
        dispatcher.dispatchWebBridgeThread(mockBridgeCall) { _ -> }
        // This will run on the main looper, so we can't directly assert here.
        // We can only ensure no exception is thrown.
    }
}
