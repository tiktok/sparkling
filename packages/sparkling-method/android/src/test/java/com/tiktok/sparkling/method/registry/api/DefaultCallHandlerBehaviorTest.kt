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
import com.tiktok.sparkling.method.registry.core.model.context.ContextProviderFactory
import io.mockk.every
import io.mockk.mockk
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Existing handler behavior, independent of the new pool-injection constructor. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class DefaultCallHandlerBehaviorTest {
    @Test
    fun factoriesUseTheContextPlatformAndMethodsReceiveTheLatestContextBeforeInvocation() {
        val handler = DefaultCallHandler()
        val firstContext = sdkContext()
        val secondContext = sdkContext()
        val method = CapturingMethod("web-method-with-required-constructor-argument")
        var webCreations = 0
        var lynxCreations = 0
        handler.setBridgeContext(firstContext)
        handler.registerLocalIDLMethod("platform.method", BridgePlatformType.WEB) {
            webCreations++
            method
        }
        handler.registerLocalIDLMethod("platform.method", BridgePlatformType.LYNX) {
            lynxCreations++
            CapturingMethod("lynx")
        }
        val bridgeContext = context(BridgePlatformType.WEB)
        val request = call(bridgeContext, "platform.method", JSONObject().put("value", 3), Platform.Lynx)
        val callback = CapturingCallback()

        handler.handle(bridgeContext, request, callback)
        handler.setBridgeContext(secondContext)
        handler.handle(bridgeContext, request, callback)

        assertEquals(1, webCreations)
        assertEquals(0, lynxCreations)
        assertEquals(listOf(firstContext, secondContext), method.contextsAtInvocation)
        assertEquals(listOf(BridgePlatformType.ALL, BridgePlatformType.ALL), method.platforms)
        assertEquals(2, callback.results.size)
        callback.results.forEach { assertSame(method.result, it) }
    }

    @Test
    fun compatibleReadableMapsUnwrapDataButJsonKeepsItsDataProperty() {
        val handler = DefaultCallHandler()
        val method = CapturingMethod("payload")
        handler.registerLocalIDLMethod("payload") { method }
        val bridgeContext = context(BridgePlatformType.LYNX)
        val nested = JavaOnlyMap().apply { putString("value", "nested") }
        val wrapped = JavaOnlyMap().apply {
            putMap("data", nested)
            putString("outer", "discarded")
        }
        val direct = JavaOnlyMap().apply { putString("value", "direct") }
        val json = JSONObject().put("data", JSONObject().put("value", "json")).put("outer", "kept")
        val callback = CapturingCallback()

        handler.handle(bridgeContext, call(bridgeContext, "payload", wrapped, Platform.Lynx), callback)
        handler.handle(bridgeContext, call(bridgeContext, "payload", direct, Platform.Lynx), callback)
        handler.handle(bridgeContext, call(bridgeContext, "payload", json, Platform.Web), callback)

        assertEquals(mapOf("value" to "nested"), method.inputs[0])
        assertEquals(mapOf("value" to "direct"), method.inputs[1])
        assertEquals(setOf("data", "outer"), method.inputs[2].keys)
        assertEquals("kept", method.inputs[2]["outer"])
        assertEquals(3, callback.results.size)
        callback.results.forEach { assertSame(method.result, it) }
    }

    @Test
    fun compatibleMethodFailuresPropagateWithoutASyntheticCallback() {
        val handler = DefaultCallHandler()
        val failure = IllegalStateException("method failed")
        val method = CapturingMethod("throwing").apply { handleFailure = failure }
        handler.registerLocalIDLMethod("throwing") { method }
        val bridgeContext = context(BridgePlatformType.WEB)
        val callback = CapturingCallback()

        val thrown = runCatching {
            handler.handle(bridgeContext, call(bridgeContext, "throwing", JSONObject()), callback)
        }.exceptionOrNull()

        assertSame(failure, thrown)
        assertEquals(1, method.inputs.size)
        assertTrue(callback.results.isEmpty())
    }

    @Test
    fun unsupportedParamsAreEmptyForCompatibleMethodsAndIgnoredForIdlMethods() {
        val handler = DefaultCallHandler()
        val sdkContext = sdkContext()
        handler.setBridgeContext(sdkContext)
        val compatible = CapturingMethod("compatible")
        val idl = CapturingMethod("idl", compatible = false)
        handler.registerLocalIDLMethod("compatible") { compatible }
        handler.registerLocalIDLMethod("idl") { idl }
        val bridgeContext = context(BridgePlatformType.LYNX)
        val compatibleCallback = CapturingCallback()
        val idlCallback = CapturingCallback()

        handler.handle(bridgeContext, call(bridgeContext, "compatible", "unsupported"), compatibleCallback)
        handler.handle(bridgeContext, call(bridgeContext, "idl", "unsupported"), idlCallback)

        assertEquals(listOf(emptyMap<String, Any?>()), compatible.inputs)
        assertEquals(listOf(BridgePlatformType.ALL), compatible.platforms)
        assertSame(compatible.result, compatibleCallback.results.single())
        assertSame(sdkContext, idl.currentContext)
        assertTrue(idl.inputs.isEmpty())
        assertTrue(idlCallback.results.isEmpty())
    }

    @Test
    fun idlProcessorErrorsKeepTheJsonAndReadableMapResultCarriers() {
        val handler = DefaultCallHandler()
        // This method intentionally has no generated parameter model.
        val method = CapturingMethod("no.model", compatible = false)
        handler.registerLocalIDLMethod("no.model") { method }
        val bridgeContext = context(BridgePlatformType.WEB)
        val jsonCallback = CapturingCallback()
        val lynxCallback = CapturingCallback()

        handler.handle(bridgeContext, call(bridgeContext, "no.model", JSONObject()), jsonCallback)
        handler.handle(
            bridgeContext,
            call(bridgeContext, "no.model", JavaOnlyMap(), Platform.Lynx),
            lynxCallback,
        )

        val jsonError = jsonCallback.results.single() as JSONObject
        val lynxError = lynxCallback.results.single() as ReadableMap
        assertEquals(IDLBridgeMethod.ANNOTATION_ERROR, jsonError.getInt("code"))
        assertEquals(IDLBridgeMethod.ANNOTATION_ERROR, lynxError.getInt("code"))
        assertTrue(method.inputs.isEmpty())
    }

    @Test
    fun releaseKeepsRegistrationsAndDoesNotBlockDirectHandlerCalls() {
        val handler = DefaultCallHandler()
        val created = mutableListOf<CapturingMethod>()
        handler.registerLocalIDLMethod("release") {
            CapturingMethod("instance-${created.size}").also { created.add(it) }
        }
        val bridgeContext = context(BridgePlatformType.WEB)
        val original = handler.getBridge(bridgeContext, "release") as CapturingMethod
        assertFalse(handler.isReleased())

        handler.onRelease()

        assertEquals(1, original.releaseCalls)
        assertTrue(handler.isReleased())
        val callback = CapturingCallback()
        // The Lynx delegate checks isReleased; the handler itself has no release guard.
        handler.handle(bridgeContext, call(bridgeContext, "release", JSONObject()), callback)
        assertEquals(2, created.size)
        assertNotSame(original, created[1])
        assertSame(created[1].result, callback.results.single())
        assertTrue(handler.isReleased())
    }

    @Test
    fun aFailedPoolReleasePropagatesAndDoesNotMarkTheHandlerReleased() {
        val handler = DefaultCallHandler()
        val failure = IllegalStateException("release failed")
        val method = CapturingMethod("release.failure").apply { releaseFailure = failure }
        handler.registerLocalIDLMethod("release.failure") { method }
        val bridgeContext = context(BridgePlatformType.WEB)
        assertSame(method, handler.getBridge(bridgeContext, "release.failure"))

        assertSame(failure, runCatching { handler.onRelease() }.exceptionOrNull())
        assertFalse(handler.isReleased())
        assertSame(method, handler.getBridge(bridgeContext, "release.failure"))

        method.releaseFailure = null
        handler.onRelease()
        assertEquals(2, method.releaseCalls)
        assertTrue(handler.isReleased())
    }

    private fun sdkContext(): IBridgeContext = mockk<IBridgeContext>().also {
        every { it.containerID } returns null
    }

    private fun context(platform: BridgePlatformType): BridgeContext = BridgeContext().apply { this.platformType = platform }

    private fun call(
        context: BridgeContext,
        name: String,
        params: Any?,
        platform: Platform = Platform.Web,
    ): BridgeCall = BridgeCall(context).apply {
        bridgeName = name
        this.params = params
        this.platform = platform
    }

    private class CapturingCallback : IBridgeMethodCallback {
        val results = mutableListOf<Any>()
        override fun onBridgeResult(parcel: Any) {
            results.add(parcel)
        }
    }

    private class CapturingMethod(override val name: String, compatible: Boolean = true) : IDLBridgeMethod {
        override val compatibility = if (compatible) IDLBridgeMethod.Compatibility.Compatible else IDLBridgeMethod.Compatibility.InCompatible
        val inputs = mutableListOf<Map<String, Any?>>()
        val platforms = mutableListOf<BridgePlatformType>()
        val contextsAtInvocation = mutableListOf<IBridgeContext?>()
        val result: Map<String, Any?> = mapOf("code" to IDLBridgeMethod.SUCCESS, "data" to mapOf("value" to name))
        var currentContext: IBridgeContext? = null
        var handleFailure: Throwable? = null
        var releaseFailure: Throwable? = null
        var releaseCalls = 0

        override fun realHandle(params: Map<String, Any?>, callback: IDLBridgeMethod.Callback, type: BridgePlatformType) {
            inputs.add(params)
            platforms.add(type)
            contextsAtInvocation.add(currentContext)
            handleFailure?.let { throw it }
            callback.invoke(result)
        }

        override fun setProviderFactory(contextProviderFactory: ContextProviderFactory?) = Unit

        override fun setBridgeContext(bridgeContext: IBridgeContext) {
            currentContext = bridgeContext
        }

        override fun release() {
            releaseCalls++
            releaseFailure?.let { throw it }
        }
    }
}
