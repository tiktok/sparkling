// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.api

import com.tiktok.sparkling.method.protocol.entity.Platform
import com.lynx.react.bridge.JavaOnlyMap
import com.lynx.react.bridge.ReadableMap
import com.tiktok.sparkling.method.protocol.BridgeContext
import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeMethodCallback
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IBridgeContext
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.IDLMethodProvider
import com.tiktok.sparkling.method.registry.core.IDLMethodRegistry
import com.tiktok.sparkling.method.registry.core.LocalBridge
import com.tiktok.sparkling.method.registry.core.model.context.ContextProviderFactory
import io.mockk.every
import io.mockk.mockk
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class DefaultCallHandlerExtensionTest {
    @Test
    fun registrationLookupContextAndReleaseUseTheSameInjectedPool() {
        val pool = HostPool()
        val handler = HostHandler(pool)
        assertEquals(0, pool.holderCalls)
        val sdkContext = mockk<IBridgeContext>()
        every { sdkContext.containerID } returns null
        handler.setBridgeContext(sdkContext)
        assertSame(sdkContext, pool.currentContext())

        handler.registerLocalIDLMethod(TrackingMethod::class.java, BridgePlatformType.WEB)
        val fromFactory = TrackingMethod()
        handler.registerLocalIDLMethod("factory", BridgePlatformType.WEB) { fromFactory }
        val context = BridgeContext().apply { platform = BridgePlatformType.WEB }
        val fromClass = requireNotNull(handler.getBridge(context, "host.class")) as TrackingMethod
        assertSame(fromClass, pool.getBridge("host.class", BridgePlatformType.WEB))
        assertSame(fromFactory, handler.getBridge(context, "factory"))

        val call = BridgeCall(context).apply {
            bridgeName = "factory"
            params = JSONObject().put("value", "host")
            platform = Platform.Web
        }
        val callback = CapturingCallback()
        handler.handle(context, call, callback)
        assertSame(sdkContext, fromFactory.receivedContext)
        assertEquals(mapOf("value" to "host"), fromFactory.receivedParams)
        assertSame(fromFactory.reply, callback.result)

        assertFalse(handler.isReleased())
        handler.onRelease()
        assertTrue(handler.isReleased())
        assertEquals(1, pool.releaseCalls)
        assertEquals(1, fromClass.releaseCalls)
        assertEquals(1, fromFactory.releaseCalls)
    }

    @Test
    fun handlingAndLookupReachTheInjectedProviderHooksAndReuseTheInstance() {
        val method = TrackingMethod()
        val lookups = mutableListOf<Pair<BridgePlatformType, String>>()
        val pool = object : BridgeLocalPool() {
            override fun findLocalMethodProvider(platformType: BridgePlatformType, bridgeName: String): IDLMethodProvider {
                lookups.add(platformType to bridgeName)
                return IDLMethodProvider { method }
            }
        }
        val handler = HostHandler(pool)
        val context = BridgeContext().apply { platform = BridgePlatformType.LYNX }
        val nested = JavaOnlyMap().apply { putString("value", "injected") }
        val call = BridgeCall(context).apply {
            bridgeName = "custom"
            params = JavaOnlyMap().apply { putMap("data", nested) }
            platform = Platform.Lynx
        }
        val callback = CapturingCallback()

        handler.handle(context, call, callback)

        assertEquals(mapOf("value" to "injected"), method.receivedParams)
        assertSame(method.reply, callback.result)
        assertSame(method, handler.getBridge(context, "custom"))
        assertEquals(listOf(BridgePlatformType.LYNX to "custom"), lookups)
    }

    @Test
    fun failedInjectedPoolReleaseDoesNotMarkTheHandlerReleased() {
        val failure = IllegalStateException("host release failed")
        val pool = object : BridgeLocalPool() {
            var shouldFail = true
            override fun release() {
                if (shouldFail) throw failure
                super.release()
            }
        }
        val handler = HostHandler(pool)
        assertSame(failure, runCatching { handler.onRelease() }.exceptionOrNull())
        assertFalse(handler.isReleased())
        pool.shouldFail = false
        handler.onRelease()
        assertTrue(handler.isReleased())
    }

    @Test
    fun hostSubclassesShareTheExistingWrapperExtraction() {
        val handler = HostHandler(BridgeLocalPool())
        val direct = JavaOnlyMap().apply { putString("value", "direct") }
        val wrapped = JavaOnlyMap().apply { putMap("data", direct) }

        assertNull(handler.unwrap(null))
        assertSame(direct, handler.unwrap(direct))
        assertSame(direct, handler.unwrap(wrapped))
    }

    private class HostHandler(pool: BridgeLocalPool) : DefaultCallHandler(pool) {
        fun unwrap(params: ReadableMap?): ReadableMap? = unWrapperParams(params)
    }

    private class HostPool : BridgeLocalPool() {
        private val holder = object : LocalBridge() {
            override fun createRegistry(): IDLMethodRegistry = object : IDLMethodRegistry() {
                override fun resolveMethodName(clazz: Class<out IDLBridgeMethod>): String = "host.class"
            }
        }
        var holderCalls = 0
        var releaseCalls = 0

        override fun localBridge(): LocalBridge {
            holderCalls++
            return holder
        }

        fun currentContext(): IBridgeContext? = bridgeContext

        override fun release() {
            releaseCalls++
            super.release()
        }
    }

    class TrackingMethod : IDLBridgeMethod {
        override val name: String = "tracking"
        override val compatibility: IDLBridgeMethod.Compatibility = IDLBridgeMethod.Compatibility.Compatible
        val reply = mapOf("code" to IDLBridgeMethod.SUCCESS)
        var receivedContext: IBridgeContext? = null
        var receivedParams: Map<String, Any?>? = null
        var releaseCalls = 0

        override fun setBridgeContext(bridgeContext: IBridgeContext) { receivedContext = bridgeContext }
        override fun setProviderFactory(contextProviderFactory: ContextProviderFactory?) = Unit
        override fun realHandle(params: Map<String, Any?>, callback: IDLBridgeMethod.Callback, type: BridgePlatformType) {
            receivedParams = params
            callback.invoke(reply)
        }
        override fun release() { releaseCalls++ }
    }

    private class CapturingCallback : IBridgeMethodCallback {
        var result: Any? = null
        override fun onBridgeResult(parcel: Any) { result = parcel }
    }
}
