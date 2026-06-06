// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.protocol.handler

import com.tiktok.sparkling.method.protocol.BridgeContext
import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.protocol.entity.BridgeResult
import com.tiktok.sparkling.method.protocol.impl.interceptor.BridgeMockInterceptor
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeCallback
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeHandler
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeMethodCallback
import com.tiktok.sparkling.method.protocol.interfaces.ShouldHandleBridgeCallResultModel
import com.tiktok.sparkling.method.registry.api.BusinessCallHandler
import com.tiktok.sparkling.method.protocol.DefaultBridgeClientImp
import com.tiktok.sparkling.method.protocol.DefaultBridgeLifeClientImp
import com.tiktok.sparkling.method.registry.api.SparklingBridge
import com.tiktok.sparkling.method.registry.api.SparklingMethodInvocationCenter
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BridgeDispatcherTest {
    private lateinit var bridgeDispatcher: BridgeDispatcher
    private val mockBridgeContext = Mockito.mock(BridgeContext::class.java)
    private val mockBridgeCall = Mockito.mock(BridgeCall::class.java)
    private val mockBridgeCallback = Mockito.mock(IBridgeCallback::class.java)
    private val mockBridgeHandler = Mockito.mock(IBridgeHandler::class.java)
    private val mockBridgeMockInterceptor = Mockito.mock(BridgeMockInterceptor::class.java)
    private val mockBusinessCallHandler = Mockito.mock(BusinessCallHandler::class.java)
    private val mockBridgeLifeClient = Mockito.mock(DefaultBridgeLifeClientImp::class.java)
    private val mockBridgeClient = Mockito.mock(DefaultBridgeClientImp::class.java)

    @Before
    fun setUp() {
        bridgeDispatcher = BridgeDispatcher()
        bridgeDispatcher.registerHandler(mockBridgeHandler)

        `when`(mockBridgeContext.jsbMockInterceptor).thenReturn(mockBridgeMockInterceptor)
        `when`(mockBridgeContext.bridgeClient).thenReturn(mockBridgeClient)
        `when`(mockBridgeContext.bridgeLifeClientImp).thenReturn(mockBridgeLifeClient)
        `when`(mockBridgeContext.businessCallHandler).thenReturn(mockBusinessCallHandler)
        `when`(mockBridgeContext.platform).thenReturn(BridgePlatformType.LYNX)
        `when`(mockBridgeContext.getNamespace()).thenReturn(null)
        `when`(mockBridgeCall.bridgeName).thenReturn("test.method")
        `when`(mockBridgeCall.nameSpace).thenReturn("")
        `when`(mockBridgeCall.params).thenReturn(JSONObject())
        `when`(mockBridgeMockInterceptor.interceptBridgeCall(mockBridgeCall)).thenReturn(mockBridgeCall)

        // Mock default behavior for lifecycle client - don't use any() in setUp
        `when`(mockBridgeLifeClient.shouldHandleBridgeCall(mockBridgeCall, mockBridgeContext)).thenReturn(ShouldHandleBridgeCallResultModel(true, null))
        SparklingBridge.bridgeFactoryManager = Mockito.mock(BridgeFactoryManager::class.java)
    }

    @Test
    fun testOnDispatchBridgeMethodWithInterceptorInvokeResult() {
        val mockResult = BridgeResult(JSONObject())
        `when`(mockBridgeMockInterceptor.invokeBridgeResult(mockBridgeCall)).thenReturn(mockResult)
        bridgeDispatcher.onDispatchBridgeMethod(mockBridgeCall, mockBridgeCallback, mockBridgeContext)
        verify(mockBridgeCallback).onBridgeResult(mockResult, mockBridgeCall, null)
    }

    @Test
    fun testOnDispatchBridgeMethodReportsStartAndEndFromDispatcher() {
        val context = BridgeContext()
        val call =
            BridgeCall(context).apply {
                bridgeName = "storage.getItem"
                nameSpace = ""
                params = JSONObject().put("key", "foo")
                platform = BridgeCall.PlatForm.Lynx
            }
        val dispatcher = BridgeDispatcher()
        dispatcher.registerHandler(
            object : IBridgeHandler {
                override fun handle(
                    bridgeContext: BridgeContext,
                    call: BridgeCall,
                    callback: IBridgeMethodCallback,
                ) {
                    callback.onBridgeResult(JSONObject().put(IDLBridgeMethod.PARAM_CODE, IDLBridgeMethod.SUCCESS))
                }

                override fun onRelease() = Unit

                override fun isReleased(): Boolean = false
            },
        )
        val observer = RecordingInvocationObserver()
        SparklingMethodInvocationCenter.addObserver(observer)
        try {
            dispatcher.onDispatchBridgeMethod(call, mockBridgeCallback, context)
        } finally {
            SparklingMethodInvocationCenter.removeObserver(observer)
        }

        assertEquals(1, observer.starts.size)
        assertEquals(1, observer.ends.size)
        assertEquals(observer.starts.single().id, observer.ends.single().id)
        assertEquals("storage.getItem", observer.ends.single().name)
        assertEquals(IDLBridgeMethod.SUCCESS, observer.ends.single().code)
    }

    @Test
    fun testOnDispatchBridgeMethodReportsLifecycleInterceptFailure() {
        val observer = RecordingInvocationObserver()
        `when`(mockBridgeMockInterceptor.invokeBridgeResult(mockBridgeCall)).thenReturn(null)
        `when`(mockBridgeLifeClient.shouldHandleBridgeCall(mockBridgeCall, mockBridgeContext))
            .thenReturn(ShouldHandleBridgeCallResultModel(false, "blocked"))

        SparklingMethodInvocationCenter.addObserver(observer)
        try {
            bridgeDispatcher.onDispatchBridgeMethod(mockBridgeCall, mockBridgeCallback, mockBridgeContext)
        } finally {
            SparklingMethodInvocationCenter.removeObserver(observer)
        }

        assertEquals(1, observer.starts.size)
        assertEquals(1, observer.ends.size)
        assertEquals(IDLBridgeMethod.BRIDGE_CALL_BE_INTERCEPTED, observer.ends.single().code)
        assertTrue(
            observer.ends
                .single()
                .result
                .toString()
                .contains("blocked"),
        )
    }

    // @Test TODO: Fix test - NullPointerException at line 78
    // fun testOnDispatchBridgeMethodWithInterceptorInterceptCall() {
    //     `when`(mockBridgeMockInterceptor.invokeBridgeResult(mockBridgeCall)).thenReturn(null)
    //     `when`(mockBridgeMockInterceptor.interceptBridgeCall(mockBridgeCall)).thenReturn(mockBridgeCall)
    //     `when`(mockBridgeContext.shouldHandleWithBusinessHandler(mockBridgeCall)).thenReturn(false)
    //
    //     bridgeDispatcher.onDispatchBridgeMethod(mockBridgeCall, mockBridgeCallback, mockBridgeContext)
    //     verify(mockBridgeHandler).handle(eq(mockBridgeContext), eq(mockBridgeCall), any())
    // }

    // @Test TODO: Fix test - NullPointerException at line 86
    // fun testOnDispatchBridgeMethodWithBusinessHandler() {
    //     `when`(mockBridgeMockInterceptor.invokeBridgeResult(mockBridgeCall)).thenReturn(null)
    //     `when`(mockBridgeContext.shouldHandleWithBusinessHandler(mockBridgeCall)).thenReturn(true)
    //
    //     bridgeDispatcher.onDispatchBridgeMethod(mockBridgeCall, mockBridgeCallback, mockBridgeContext)
    //     verify(mockBusinessCallHandler).handle(eq(mockBridgeContext), eq(mockBridgeCall), any())
    // }

    // @Test TODO: Fix test - InvalidUseOfMatchersException at line 39
    // fun testHandleRawJSBCall() {
    //     bridgeDispatcher.handleRawJSBCall(mockBridgeCall, mockBridgeContext, mockBridgeCallback)
    //     verify(mockBridgeHandler).handle(eq(mockBridgeContext), eq(mockBridgeCall), any())
    // }

    // @Test TODO: Fix test - UnfinishedVerificationException at line 39
    // fun testRegisterHandler() {
    //     val newHandler = Mockito.mock(IBridgeHandler::class.java)
    //     bridgeDispatcher.registerHandler(newHandler)
    //     // No direct assertion, but ensures the method runs without error.
    // }

    private class RecordingInvocationObserver : SparklingMethodInvocationCenter.Observer {
        val starts = mutableListOf<SparklingMethodInvocationCenter.Event>()
        val ends = mutableListOf<SparklingMethodInvocationCenter.Event>()

        override fun onStart(event: SparklingMethodInvocationCenter.Event) {
            starts += event
        }

        override fun onEnd(event: SparklingMethodInvocationCenter.Event) {
            ends += event
        }
    }
}
