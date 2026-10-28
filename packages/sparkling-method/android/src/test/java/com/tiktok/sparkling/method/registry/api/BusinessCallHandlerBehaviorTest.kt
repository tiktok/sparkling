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
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamField
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamModel
import com.tiktok.sparkling.method.registry.core.model.context.ContextProviderFactory
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseParamModel
import io.mockk.mockk
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Existing namespace-handler behavior, independent of registry injection. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class BusinessCallHandlerBehaviorTest {
    @Test
    fun factoryOnlyRegistrationsKeepPlatformSelectionAndCreateAnInstanceForEveryLookup() {
        val handler = BusinessCallHandler("business")
        val created = mutableListOf<CapturingMethod>()
        handler.registerMethod("method") {
            CapturingMethod("default-${created.size}").also { created.add(it) }
        }
        handler.registerMethod("method", BridgePlatformType.WEB) {
            CapturingMethod("web-${created.size}").also { created.add(it) }
        }
        assertTrue(created.isEmpty())
        assertTrue(handler.isMethodExists("method"))
        assertFalse(handler.isMethodExists("method", BridgePlatformType.NONE))

        val first = handler.getBridge(context(BridgePlatformType.WEB), "method")
        val second = handler.getBridge(context(BridgePlatformType.WEB), "method")
        val lynx = handler.getBridge(context(BridgePlatformType.LYNX), "method")
        val fallback = handler.getBridge(context(BridgePlatformType.NONE), "method")

        assertEquals(listOf("web-0", "web-1", "default-2", "default-3"), created.map { it.name })
        assertSame(created[0], first)
        assertSame(created[1], second)
        assertSame(created[2], lynx)
        assertSame(created[3], fallback)
        assertNotSame(first, second)
        assertNull(handler.getBridge(context(BridgePlatformType.WEB), "missing"))
        assertEquals(4, created.size)
    }

    @Test
    fun namespaceRoutingAcceptsMatchingOrEmptyNamespacesAndCreatesSeparateRoutingAndHandlingInstances() {
        val handler = BusinessCallHandler("business")
        val created = mutableListOf<CapturingMethod>()
        handler.registerMethod("method") {
            CapturingMethod("instance-${created.size}").also { created.add(it) }
        }
        val context = context(BridgePlatformType.LYNX).apply { businessCallHandler = handler }
        val request = call(context, "method", JavaOnlyMap()).apply { nameSpace = "business" }
        assertEquals("business", context.getNamespace())

        assertTrue(context.shouldHandleWithBusinessHandler(request))
        val callback = CapturingCallback()
        handler.handle(context, request, callback)
        assertEquals(2, created.size)
        assertTrue(created[0].inputs.isEmpty())
        assertEquals(1, created[1].inputs.size)
        assertEquals(1, callback.results.size)

        request.nameSpace = ""
        assertTrue(context.shouldHandleWithBusinessHandler(request))
        request.nameSpace = "other"
        assertFalse(context.shouldHandleWithBusinessHandler(request))
        request.nameSpace = "business"
        request.bridgeName = "missing"
        assertFalse(context.shouldHandleWithBusinessHandler(request))
        assertEquals(3, created.size)
    }

    @Test
    fun handlingKeepsContextHandoffWrapperConversionAndProcessorResultCarriers() {
        val handler = BusinessCallHandler("business")
        val method = CapturingMethod("payload")
        handler.registerMethod("payload") { method }
        val firstContext = mockk<IBridgeContext>()
        val secondContext = mockk<IBridgeContext>()
        val context = context(BridgePlatformType.LYNX)
        val wrapped = JavaOnlyMap().apply {
            putMap("data", JavaOnlyMap().apply { putString("value", "nested") })
            putString("outer", "discarded")
        }
        val direct = JavaOnlyMap().apply { putString("value", "direct") }
        val json = JSONObject().put("data", JSONObject().put("value", "json")).put("outer", "kept")
        val callback = CapturingCallback()
        handler.setBridgeContext(firstContext)
        handler.handle(context, call(context, "payload", wrapped), callback)
        handler.setBridgeContext(secondContext)
        handler.handle(context, call(context, "payload", direct), callback)
        // Payload type selects the processor, even when BridgeCall.platform is Lynx.
        handler.handle(context, call(context, "payload", json), callback)

        assertEquals(listOf(firstContext, secondContext, secondContext), method.contextsAtInvocation)
        assertEquals(mapOf("value" to "nested"), method.inputs[0])
        assertEquals(mapOf("value" to "direct"), method.inputs[1])
        assertEquals(mapOf("data" to mapOf("value" to "json"), "outer" to "kept"), method.inputs[2])
        assertEquals(listOf(BridgePlatformType.LYNX, BridgePlatformType.LYNX, BridgePlatformType.WEB), method.platforms)
        assertEquals(IDLBridgeMethod.SUCCESS, (callback.results[0] as ReadableMap).getInt("code"))
        assertEquals(IDLBridgeMethod.SUCCESS, (callback.results[1] as ReadableMap).getInt("code"))
        assertEquals(IDLBridgeMethod.SUCCESS, (callback.results[2] as JSONObject).getInt("code"))
    }

    @Test
    fun compatibleMethodsStillRequireAnIdlModelAndUnsupportedPayloadsOnlyReceiveTheContext() {
        val handler = BusinessCallHandler("business")
        val delegate = CapturingMethod("no.model")
        val method = ModelLessMethod(delegate)
        handler.registerMethod("no.model") { method }
        val sdkContext = mockk<IBridgeContext>()
        handler.setBridgeContext(sdkContext)
        val context = context(BridgePlatformType.LYNX)
        val callback = CapturingCallback()
        handler.handle(context, call(context, "no.model", JavaOnlyMap()), callback)
        handler.handle(context, call(context, "no.model", JSONObject()), callback)
        handler.handle(context, call(context, "no.model", "unsupported"), callback)

        assertSame(sdkContext, delegate.currentContext)
        assertTrue(delegate.inputs.isEmpty())
        assertEquals(2, callback.results.size)
        assertEquals(IDLBridgeMethod.ANNOTATION_ERROR, (callback.results[0] as ReadableMap).getInt("code"))
        assertEquals(IDLBridgeMethod.ANNOTATION_ERROR, (callback.results[1] as JSONObject).getInt("code"))
    }

    @Test
    fun methodFailuresBecomeProcessorErrorsWhileFactoryAndContextFailuresPropagate() {
        val handler = BusinessCallHandler("business")
        val methodFailure = IllegalStateException("method failed")
        val contextFailure = IllegalStateException("context failed")
        val factoryFailure = IllegalStateException("factory failed")
        val method = CapturingMethod("throwing").apply { handleFailure = methodFailure }
        handler.registerMethod("throwing") { method }
        handler.registerMethod("factory.failure") { throw factoryFailure }
        val context = context(BridgePlatformType.WEB)
        val callback = CapturingCallback()
        handler.handle(context, call(context, "throwing", JSONObject()), callback)

        assertEquals(IDLBridgeMethod.UNKNOWN_ERROR, (callback.results.single() as JSONObject).getInt("code"))
        assertEquals(1, method.inputs.size)
        assertSame(factoryFailure, runCatching {
            handler.handle(context, call(context, "factory.failure", JSONObject()), callback)
        }.exceptionOrNull())
        handler.setBridgeContext(mockk())
        method.contextFailure = contextFailure
        assertSame(contextFailure, runCatching {
            handler.handle(context, call(context, "throwing", JSONObject()), callback)
        }.exceptionOrNull())
        assertEquals(1, callback.results.size)
        assertEquals(1, method.inputs.size)
    }

    @Test
    fun releaseDoesNotReleaseMethodsOrRemoveRegistrationsAndDirectCallsRemainAvailable() {
        val handler = BusinessCallHandler("business")
        val created = mutableListOf<CapturingMethod>()
        handler.registerMethod("release") {
            CapturingMethod("instance-${created.size}").also { created.add(it) }
        }
        val context = context(BridgePlatformType.WEB)
        val first = handler.getBridge(context, "release")
        val second = handler.getBridge(context, "release")
        assertNotSame(first, second)
        assertFalse(handler.isReleased())

        handler.onRelease()

        assertTrue(handler.isReleased())
        assertTrue(handler.isMethodExists("release"))
        assertTrue(created.all { it.releaseCalls == 0 })
        val callback = CapturingCallback()
        handler.handle(context, call(context, "release", JSONObject()), callback)
        assertEquals(3, created.size)
        assertEquals(1, created[2].inputs.size)
        assertEquals(IDLBridgeMethod.SUCCESS, (callback.results.single() as JSONObject).getInt("code"))
        assertTrue(handler.isReleased())
        assertTrue(created.all { it.releaseCalls == 0 })
    }

    private fun context(platform: BridgePlatformType): BridgeContext = BridgeContext().apply { this.platform = platform }

    private fun call(context: BridgeContext, name: String, params: Any?): BridgeCall = BridgeCall(context).apply {
        bridgeName = name
        this.params = params
        platform = Platform.Lynx
    }

    private class CapturingCallback : IBridgeMethodCallback {
        val results = mutableListOf<Any>()

        override fun onBridgeResult(parcel: Any) {
            results.add(parcel)
        }
    }

    private class ModelLessMethod(delegate: IDLBridgeMethod) : IDLBridgeMethod by delegate

    private class CapturingMethod(override val name: String) : IDLBridgeMethod {
        override val compatibility = IDLBridgeMethod.Compatibility.Compatible
        val inputs = mutableListOf<Map<String, Any?>>()
        val platforms = mutableListOf<BridgePlatformType>()
        val contextsAtInvocation = mutableListOf<IBridgeContext?>()
        var currentContext: IBridgeContext? = null
        var handleFailure: Throwable? = null
        var contextFailure: Throwable? = null
        var releaseCalls = 0

        @IDLMethodParamModel
        interface Params : IDLMethodBaseParamModel {
            @get:IDLMethodParamField(keyPath = "data")
            val data: Map<String, Any>?

            @get:IDLMethodParamField(keyPath = "value")
            val value: String?

            @get:IDLMethodParamField(keyPath = "outer")
            val outer: String?
        }

        override fun realHandle(params: Map<String, Any?>, callback: IDLBridgeMethod.Callback, type: BridgePlatformType) {
            inputs.add(params)
            platforms.add(type)
            contextsAtInvocation.add(currentContext)
            handleFailure?.let { throw it }
            callback.invoke(mapOf("code" to IDLBridgeMethod.SUCCESS))
        }

        override fun setProviderFactory(contextProviderFactory: ContextProviderFactory?) = Unit

        override fun setBridgeContext(bridgeContext: IBridgeContext) {
            contextFailure?.let { throw it }
            currentContext = bridgeContext
        }

        override fun release() {
            releaseCalls++
        }
    }
}
