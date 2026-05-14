// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core.base

import android.webkit.WebView
import com.lynx.tasm.LynxView
import com.tiktok.sparkling.method.protocol.BridgeContext
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IBridgeContext
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamField
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamModel
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodResultModel
import com.tiktok.sparkling.method.registry.core.model.context.ContextProviderFactory
import com.tiktok.sparkling.method.registry.core.model.idl.CompletionBlock
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseParamModel
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseResultModel
import io.mockk.*
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AbsSparklingIDLMethodTest {
    private lateinit var testMethod: TestSparklingIDLMethod
    private lateinit var mockBridgeContext: IBridgeContext
    private lateinit var mockContextFactory: ContextProviderFactory
    private lateinit var mockCallback: IDLBridgeMethod.Callback
    private lateinit var mockCompletionBlock: CompletionBlock<TestSparklingIDLMethod.TestResultModel>
    private lateinit var mockWebView: WebView
    private lateinit var mockLynxView: LynxView
    private lateinit var mockProtocolBridgeContext: BridgeContext

    @Before
    fun setUp() {
        mockBridgeContext = mockk(relaxed = true)
        mockContextFactory = mockk(relaxed = true)
        mockCallback = mockk(relaxed = true)
        mockCompletionBlock = mockk(relaxed = true)
        mockWebView = mockk(relaxed = true)
        mockLynxView = mockk(relaxed = true)
        mockProtocolBridgeContext = mockk(relaxed = true)

        // Initialize test method
        testMethod = TestSparklingIDLMethod()
        testMethod.setBridgeContext(mockBridgeContext)
        testMethod.setProviderFactory(mockContextFactory)

        every { mockBridgeContext.getObject(BridgeContext::class.java) } returns mockProtocolBridgeContext
        every { mockProtocolBridgeContext.bridgeLifeClientImp } returns mockk(relaxed = true)
        every { mockWebView.url } returns "https://test.com"
        every { mockLynxView.templateUrl } returns "https://lynx.test.com"
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testSetProviderFactory() {
        // Given
        val newFactory = mockk<ContextProviderFactory>()

        // When
        testMethod.setProviderFactory(newFactory)

        // Then
        assertEquals("Context provider factory should be set", newFactory, testMethod.contextProviderFactory)
    }

    @Test
    fun testSetBridgeContext() {
        // Given
        val newContext = mockk<IBridgeContext>()

        // When
        testMethod.setBridgeContext(newContext)

        // Then
        assertEquals("Bridge context should be set", newContext, testMethod.getSDKContext())
    }

    @Test
    fun testGetSDKContext() {
        // When
        val context = testMethod.getSDKContext()

        // Then
        assertEquals("Should return the set bridge context", mockBridgeContext, context)
    }

    @Test
    fun testProvideContextDeprecated() {
        // Given
        val testClass = String::class.java
        val expectedResult = "test_result"
        every { mockContextFactory.provideInstance(testClass) } returns expectedResult

        // When
        val result = testMethod.provideContext(testClass)

        // Then
        assertEquals("Should provide context instance", expectedResult, result)
        verify { mockContextFactory.provideInstance(testClass) }
    }

    @Test
    fun testOnSuccessWithOriginalResult() {
        // Given
        val testData =
            mutableMapOf<String, Any>(
                "key1" to "value1",
                IDLBridgeMethod.ORIGINAL_RESULT to mapOf("original" to "data"),
            )
        val msg = "Success message"
        testMethod.useOriginalResultOverride = true

        every { mockCallback.invoke(any()) } returns Unit

        // When
        testMethod.onSuccess(mockCallback, testData, msg)

        // Then
        verify { mockCallback.invoke(mapOf("original" to "data") as Map<String, Any?>) }
    }

    @Test
    fun testOnSuccessWithNormalResult() {
        // Given
        val testData = mutableMapOf<String, Any>("key1" to "value1")
        val msg = "Success message"
        testMethod.useOriginalResultOverride = false

        every { mockCallback.invoke(any()) } returns Unit

        // When
        testMethod.onSuccess(mockCallback, testData, msg)

        // Then
        verify {
            mockCallback.invoke(any())
        }
    }

    @Test
    fun testOnFailureWithOriginalResult() {
        // Given
        val testData =
            mutableMapOf<String, Any>(
                IDLBridgeMethod.ORIGINAL_RESULT to mapOf("error" to "details"),
            )
        val code = 500
        val msg = "Error message"
        testMethod.useOriginalResultOverride = true

        every { mockCallback.invoke(any()) } returns Unit

        // When
        testMethod.onFailure(mockCallback, code, msg, testData)

        // Then
        verify { mockCallback.invoke(mapOf("error" to "details") as Map<String, Any?>) }
    }

    @Test
    fun testOnFailureWithNormalResult() {
        // Given
        val testData = mutableMapOf<String, Any>("error_key" to "error_value")
        val code = 404
        val msg = "Not found"
        testMethod.useOriginalResultOverride = false

        every { mockCallback.invoke(any()) } returns Unit

        // When
        testMethod.onFailure(mockCallback, code, msg, testData)

        // Then
        verify {
            mockCallback.invoke(any())
        }
    }

    @Test
    fun testGetHybridUrlForLynx() {
        // Given
        every { mockBridgeContext.view } returns mockLynxView

        // When - Use reflection to access protected method
        val method = AbsSparklingIDLMethod::class.java.getDeclaredMethod("getHybridUrl", BridgePlatformType::class.java)
        method.isAccessible = true
        val url = method.invoke(testMethod, BridgePlatformType.LYNX) as? String

        // Then
        assertEquals("Should return Lynx template URL", "https://lynx.test.com", url)
    }

    @Test
    fun testGetHybridUrlForWeb() {
        // Given
        every { mockBridgeContext.view } returns mockWebView

        // When - Use reflection to access protected method
        val method = AbsSparklingIDLMethod::class.java.getDeclaredMethod("getHybridUrl", BridgePlatformType::class.java)
        method.isAccessible = true
        val url = method.invoke(testMethod, BridgePlatformType.WEB) as? String

        // Then
        assertEquals("Should return WebView URL", "https://test.com", url)
    }

    @Test
    fun testRealHandleWithValidParams() {
        // Given
        val params = mapOf<String, Any?>("param1" to "value1")
        testMethod.handleInvoked = false

        every { mockCallback.invoke(any()) } returns Unit

        // When
        testMethod.realHandle(params, mockCallback, BridgePlatformType.WEB)

        // Then
        assertTrue("Handle method should be invoked", testMethod.handleInvoked)
        verify { mockProtocolBridgeContext.bridgeLifeClientImp }
    }

    @Test
    fun testToJSONFromMap() {
        // Given
        val testMap = mapOf("key1" to "value1", "key2" to 123)

        // When
        val result = testMethod.toJSON(testMap)

        // Then
        assertNotNull("JSON result should not be null", result)
        assertEquals("Should contain key1", "value1", result.getString("key1"))
        assertEquals("Should contain key2", 123, result.getInt("key2"))
    }

    @Test
    fun testToJSONFromList() {
        // Given
        val testList = listOf("item1", "item2", 123)

        // When
        val result = testMethod.toJSON(testList)

        // Then
        assertNotNull("JSON array result should not be null", result)
        assertEquals("Should have 3 items", 3, result.length())
        assertEquals("First item should be 'item1'", "item1", result.getString(0))
    }

    // Test implementation classes
    private class TestSparklingIDLMethod : AbsSparklingIDLMethod<AbsSparklingIDLMethodTest.TestSparklingIDLMethod.TestParamModel, AbsSparklingIDLMethodTest.TestSparklingIDLMethod.TestResultModel>() {
        var handleInvoked = false
        var useOriginalResultOverride = false

        override val name: String = "testMethod"
        override val useOriginalResult: Boolean get() = useOriginalResultOverride

        override fun handle(
            params: TestParamModel,
            callback: CompletionBlock<TestResultModel>,
            type: BridgePlatformType,
        ) {
            handleInvoked = true
            callback.onSuccess(TestResultModel(), "Success")
        }

        @IDLMethodParamModel
        interface TestParamModel : IDLMethodBaseParamModel {
            @get:IDLMethodParamField(keyPath = "param1")
            val param1: String?

            override fun toJSON(): JSONObject =
                JSONObject().apply {
                    put("param1", param1)
                }

            override fun convert(): Map<String, Any>? = mapOf("param1" to (param1 ?: ""))
        }

        @IDLMethodResultModel
        class TestResultModel : IDLMethodBaseResultModel {
            override fun convert(): MutableMap<String, Any>? = mutableMapOf("result" to "success")

            override fun toJSON(): JSONObject =
                JSONObject().apply {
                    put("result", "success")
                }
        }
    }
}
