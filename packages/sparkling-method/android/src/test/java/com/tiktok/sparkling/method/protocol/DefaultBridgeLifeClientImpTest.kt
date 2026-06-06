// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.protocol

import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.protocol.entity.BridgeResult
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeLifeClient
import com.tiktok.sparkling.method.protocol.impl.errors.JSBErrorReportModel
import com.tiktok.sparkling.method.protocol.impl.lifecycle.fe.FeCallMonitorModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class DefaultBridgeLifeClientImpTest {
    private lateinit var lifeClient: DefaultBridgeLifeClientImp
    private lateinit var mockBridgeContext: BridgeContext
    private lateinit var mockBridgeCall: BridgeCall
    private lateinit var mockErrorReportModel: JSBErrorReportModel
    private lateinit var mockFeCallMonitorModel: FeCallMonitorModel

    @Before
    fun setUp() {
        mockBridgeContext = Mockito.mock(BridgeContext::class.java)
        mockBridgeCall = Mockito.mock(BridgeCall::class.java)
        mockErrorReportModel = Mockito.mock(JSBErrorReportModel::class.java)
        mockFeCallMonitorModel = Mockito.mock(FeCallMonitorModel::class.java)

        // Set up the BridgeCall mock
        Mockito.`when`(mockBridgeCall.context).thenReturn(mockBridgeContext)
        Mockito.`when`(mockBridgeCall.jsbSDKErrorReportModel).thenReturn(mockErrorReportModel)
        Mockito.`when`(mockBridgeCall.feCallMonitorModel).thenReturn(mockFeCallMonitorModel)

        lifeClient = DefaultBridgeLifeClientImp(mockBridgeContext)
    }

    @Test
    fun testRegisterIBridgeLifeClient() {
        val mockLifeClient = Mockito.mock(IBridgeLifeClient::class.java)
        lifeClient.registerIBridgeLifeClient(mockLifeClient)
        // Cannot directly access bridgeLifeClients, so we can't assert here.
        // We can only ensure no exception is thrown.
    }

    @Test
    fun testShouldHandleBridgeCall() {
        val result = lifeClient.shouldHandleBridgeCall(mockBridgeCall, mockBridgeContext)
        assertTrue(result.shouldHandleBridgeCall)
        assertNull(result.reason)
    }

    @Test
    fun testOnBridgeCalledStart() {
        Mockito.`when`(mockBridgeCall.platform).thenReturn(BridgeCall.PlatForm.Other)
        Mockito.`when`(mockBridgeCall.url).thenReturn("http://test.com")
        Mockito.`when`(mockBridgeCall.bridgeName).thenReturn("testBridge")
        Mockito.`when`(mockBridgeCall.jsbEngine).thenReturn("testEngine")

        lifeClient.onBridgeCalledStart(mockBridgeCall, mockBridgeContext)

        // Verify the error report model was set up
        Mockito.verify(mockErrorReportModel).setJsbBridgeSdk("SparklingBridge")
        Mockito.verify(mockErrorReportModel).setJsbUrl("http://test.com")
        Mockito.verify(mockErrorReportModel).setJsbMethodName("testBridge")
        Mockito.verify(mockErrorReportModel).setJsbEngine("testEngine")
    }

    @Test
    fun testOnBridgeImplHandleStart() {
        lifeClient.onBridgeImplHandleStart(mockBridgeCall, mockBridgeContext)
        // No assertion, just ensuring no exception is thrown
    }

    @Test
    fun testOnBridgeImplHandleEnd() {
        lifeClient.onBridgeImplHandleEnd(mockBridgeCall, mockBridgeContext)
        // No assertion, just ensuring no exception is thrown
    }

    @Test
    fun testOnBridgeCallbackCallStart() {
        val mockResult = Mockito.mock(BridgeResult::class.java)
        Mockito.`when`(mockBridgeCall.platform).thenReturn(BridgeCall.PlatForm.Other)
        lifeClient.onBridgeCallbackCallStart(mockResult, mockBridgeCall, mockBridgeContext)
        // No assertion, just ensuring no exception is thrown
    }

    @Test
    fun testOnBridgeCallbackInvokeStart() {
        val mockResult = Mockito.mock(BridgeResult::class.java)
        Mockito.`when`(mockBridgeCall.platform).thenReturn(BridgeCall.PlatForm.Other)
        lifeClient.onBridgeCallbackInvokeStart(mockResult, mockBridgeCall)
        // No assertion, just ensuring no exception is thrown
    }

    @Test
    fun testDelegatesEventCallbacksToRegisteredLifeClient() {
        val child = Mockito.mock(IBridgeLifeClient::class.java)
        lifeClient.registerIBridgeLifeClient(child)

        lifeClient.onBridgeEventStart("evt", mapOf("k" to "v"))
        lifeClient.onBridgeEventEnd("evt", mapOf("k" to "v"))

        Mockito.verify(child).onBridgeEventStart("evt", mapOf("k" to "v"))
        Mockito.verify(child).onBridgeEventEnd("evt", mapOf("k" to "v"))
    }

    @Test
    fun testOnBridgeCalledEndUpdatesMonitorAndDelegates() {
        val child = Mockito.mock(IBridgeLifeClient::class.java)
        lifeClient.registerIBridgeLifeClient(child)
        Mockito.`when`(mockBridgeCall.platform).thenReturn(BridgeCall.PlatForm.Other)

        lifeClient.onBridgeCalledEnd(mockBridgeCall, mockBridgeContext)

        Mockito.verify(child).onBridgeCalledEnd(mockBridgeCall, mockBridgeContext)
        Mockito.verify(mockFeCallMonitorModel).reportFeCallInfo()
    }
}
