// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.registry.api

import com.lynx.react.bridge.JavaOnlyMap
import com.tiktok.sparkling.method.protocol.BridgeContext
import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeMethodCallback
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IBridgeContext
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.model.context.ContextProviderFactory
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Public stub bridges defined at top level so they can be instantiated via
 * `Class.newInstance()` from registry lookups inside the production code.
 */
class CallHandlersStubCompatibleBridge : IDLBridgeMethod {
    override val name: String = "stub.compatible.bridge"
    override val compatibility: IDLBridgeMethod.Compatibility =
        IDLBridgeMethod.Compatibility.Compatible

    override fun realHandle(
        params: Map<String, Any?>,
        callback: IDLBridgeMethod.Callback,
        type: BridgePlatformType,
    ) {
        callback.invoke(
            mapOf(
                IDLBridgeMethod.PARAM_CODE to IDLBridgeMethod.SUCCESS,
                "echo" to params,
            ),
        )
    }

    override fun setProviderFactory(contextProviderFactory: ContextProviderFactory?) = Unit

    override fun setBridgeContext(bridgeContext: IBridgeContext) = Unit
}

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CallHandlersTest {
    private class CapturingCallback : IBridgeMethodCallback {
        var lastResult: Any? = null

        override fun onBridgeResult(parcel: Any) {
            lastResult = parcel
        }
    }

    private fun newBridgeContext(platform: BridgePlatformType = BridgePlatformType.ALL): BridgeContext = BridgeContext().apply { this.platform = platform }

    @Test
    fun defaultCallHandlerLifecycleAndRegistration() {
        val handler = DefaultCallHandler()
        assertFalse(handler.isReleased())

        // Registering a known class makes getBridge() succeed.
        handler.registerLocalIDLMethod(
            CallHandlersStubCompatibleBridge::class.java,
            BridgePlatformType.ALL,
        )

        val ctx = newBridgeContext(BridgePlatformType.ALL)
        val resolved = handler.getBridge(ctx, "stub.compatible.bridge")
        assertNotNull(resolved)
        assertTrue(resolved is CallHandlersStubCompatibleBridge)

        handler.onRelease()
        assertTrue(handler.isReleased())
    }

    @Test
    fun defaultCallHandlerHandleReportsBridgeNotFoundWhenUnregistered() {
        val handler = DefaultCallHandler()
        val callback = CapturingCallback()
        val call =
            BridgeCall(BridgeContext()).apply {
                bridgeName = "missing.bridge"
                params = JSONObject()
                platform = BridgeCall.PlatForm.Web
            }

        handler.handle(newBridgeContext(BridgePlatformType.WEB), call, callback)

        val payload = callback.lastResult as JSONObject
        assertEquals(-2, payload.getInt("code"))
    }

    @Test
    fun defaultCallHandlerHandleRoutesCompatibleBridgeWithJsonParams() {
        val handler = DefaultCallHandler()
        handler.registerLocalIDLMethod(
            CallHandlersStubCompatibleBridge::class.java,
            BridgePlatformType.ALL,
        )

        val callback = CapturingCallback()
        val ctx = newBridgeContext(BridgePlatformType.ALL)
        val call =
            BridgeCall(ctx).apply {
                bridgeName = "stub.compatible.bridge"
                params = JSONObject().put("k", "v")
                platform = BridgeCall.PlatForm.Web
            }

        handler.handle(ctx, call, callback)

        @Suppress("UNCHECKED_CAST")
        val result = callback.lastResult as Map<String, Any?>
        assertEquals(IDLBridgeMethod.SUCCESS, result[IDLBridgeMethod.PARAM_CODE])
        @Suppress("UNCHECKED_CAST")
        val echoed = result["echo"] as Map<String, Any?>
        assertEquals("v", echoed["k"])
    }

    @Test
    fun defaultCallHandlerHandleRoutesCompatibleBridgeWithReadableMapParams() {
        val handler = DefaultCallHandler()
        handler.registerLocalIDLMethod(
            CallHandlersStubCompatibleBridge::class.java,
            BridgePlatformType.ALL,
        )

        val callback = CapturingCallback()
        val ctx = newBridgeContext(BridgePlatformType.LYNX)
        val params = JavaOnlyMap()
        params.putString("kind", "readable")
        val call =
            BridgeCall(ctx).apply {
                bridgeName = "stub.compatible.bridge"
                this.params = params
                platform = BridgeCall.PlatForm.Lynx
            }

        handler.handle(ctx, call, callback)

        @Suppress("UNCHECKED_CAST")
        val result = callback.lastResult as Map<String, Any?>
        assertEquals(IDLBridgeMethod.SUCCESS, result[IDLBridgeMethod.PARAM_CODE])
    }

    @Test
    fun defaultCallHandlerHandleRoutesCompatibleBridgeWithUnknownParams() {
        val handler = DefaultCallHandler()
        handler.registerLocalIDLMethod(
            CallHandlersStubCompatibleBridge::class.java,
            BridgePlatformType.ALL,
        )

        val callback = CapturingCallback()
        val ctx = newBridgeContext(BridgePlatformType.ALL)
        val call =
            BridgeCall(ctx).apply {
                bridgeName = "stub.compatible.bridge"
                this.params = "ignored"
                platform = BridgeCall.PlatForm.Other
            }

        handler.handle(ctx, call, callback)

        @Suppress("UNCHECKED_CAST")
        val result = callback.lastResult as Map<String, Any?>
        assertEquals(IDLBridgeMethod.SUCCESS, result[IDLBridgeMethod.PARAM_CODE])
    }

    @Test
    fun businessCallHandlerLifecycleAndRegistration() {
        val handler = BusinessCallHandler("ns.test")
        assertEquals("ns.test", handler.nameSpace)
        assertFalse(handler.isReleased())

        handler.registerMethod(
            CallHandlersStubCompatibleBridge::class.java,
            BridgePlatformType.ALL,
        )

        assertTrue(handler.isMethodExists("stub.compatible.bridge"))
        assertFalse(handler.isMethodExists("not.registered"))

        val ctx = newBridgeContext(BridgePlatformType.ALL)
        val resolved = handler.getBridge(ctx, "stub.compatible.bridge")
        assertNotNull(resolved)

        handler.onRelease()
        assertTrue(handler.isReleased())
    }

    @Test
    fun businessCallHandlerHandleSendsBridgeNotFoundWhenUnregistered() {
        val handler = BusinessCallHandler("ns")
        val callback = CapturingCallback()
        val ctx = newBridgeContext()
        val call =
            BridgeCall(ctx).apply {
                bridgeName = "missing.bridge"
                params = JSONObject()
                platform = BridgeCall.PlatForm.Web
            }

        handler.handle(ctx, call, callback)
        val payload = callback.lastResult as JSONObject
        assertEquals(-2, payload.getInt("code"))
    }

    @Test
    fun businessCallHandlerHandleHonorsUnknownParamShape() {
        val handler = BusinessCallHandler("ns")
        handler.registerMethod(CallHandlersStubCompatibleBridge::class.java)

        val callback = CapturingCallback()
        val ctx = newBridgeContext()
        val call =
            BridgeCall(ctx).apply {
                bridgeName = "stub.compatible.bridge"
                this.params = 42
                platform = BridgeCall.PlatForm.Other
            }

        handler.handle(ctx, call, callback)
        // Unknown param branch returns silently.
        assertNull(callback.lastResult)
    }
}
