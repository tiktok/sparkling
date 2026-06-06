// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.registry.api

import com.lynx.react.bridge.JavaOnlyMap
import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IBridgeContext
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.model.context.ContextProviderFactory
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class JsonAndReadableMapProcessorTest {
    private class StubBridge : IDLBridgeMethod {
        override val name: String = "stub"

        override fun realHandle(
            params: Map<String, Any?>,
            callback: IDLBridgeMethod.Callback,
            type: BridgePlatformType,
        ) = Unit

        override fun setProviderFactory(contextProviderFactory: ContextProviderFactory?) = Unit

        override fun setBridgeContext(bridgeContext: IBridgeContext) = Unit
    }

    @Test
    fun jsonProcessorPlatformTypeAndErrorPayload() {
        val processor = JsonProcessor(StubBridge(), JSONObject(), BridgeCall.PlatForm.Web)
        assertEquals(BridgePlatformType.WEB, processor.getPlatformType())

        val err = processor.onError(IDLBridgeMethod.INVALID_PARAM, "bad")
        assertEquals(IDLBridgeMethod.INVALID_PARAM, err.optInt(IDLBridgeMethod.PARAM_CODE))
        assertEquals("bad", err.optString(IDLBridgeMethod.PARAM_MSG))

        // Default platform parameter should also be Web.
        val defaulted = JsonProcessor(StubBridge(), JSONObject())
        assertEquals(BridgeCall.PlatForm.Web, defaulted.platForm)
    }

    @Test
    fun readableMapProcessorPlatformTypeAndErrorPayload() {
        val processor = ReadableMapProcessor(StubBridge(), JavaOnlyMap())
        assertEquals(BridgePlatformType.LYNX, processor.getPlatformType())

        val err = processor.onError(IDLBridgeMethod.UNKNOWN_ERROR, "boom")
        assertEquals(IDLBridgeMethod.UNKNOWN_ERROR, err.getInt(IDLBridgeMethod.PARAM_CODE))
        assertEquals("boom", err.getString(IDLBridgeMethod.PARAM_MSG))
    }
}
