// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.protocol.impl.lynx

import android.content.Context
import com.lynx.react.bridge.Callback
import com.lynx.react.bridge.ReadableMap
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class LynxRuntimeBridgeDelegateModuleTest {
    private lateinit var lynxRuntimeBridgeDelegateModule: LynxRuntimeBridgeDelegateModule
    private val mockContext = Mockito.mock(Context::class.java)
    private val mockObj = Mockito.mock(Any::class.java)

    @Before
    fun setUp() {
        lynxRuntimeBridgeDelegateModule = LynxRuntimeBridgeDelegateModule(mockContext, mockObj)
    }

    @Test
    fun testCall() {
        val bridgeName = "testBridge"
        val params = Mockito.mock(ReadableMap::class.java)
        val callback = Mockito.mock(Callback::class.java)

        // Since realLynxBridgeDelegate is private and final, we can't mock it directly.
        // We'll test the behavior of the call method assuming realLynxBridgeDelegate works as expected.
        // This test primarily ensures that the call method is invoked without errors.
        lynxRuntimeBridgeDelegateModule.call(bridgeName, params, callback)
        // No direct assertion possible without mocking the private field.
        // This test serves as a basic sanity check.
    }
}
