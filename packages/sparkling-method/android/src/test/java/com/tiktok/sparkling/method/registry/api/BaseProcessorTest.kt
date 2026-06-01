// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.registry.api

import com.tiktok.sparkling.method.protocol.interfaces.IBridgeMethodCallback
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IBridgeContext
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.exception.IllegalInputParamException
import com.tiktok.sparkling.method.registry.core.exception.IllegalOperationException
import com.tiktok.sparkling.method.registry.core.exception.IllegalOutputParamException
import com.tiktok.sparkling.method.registry.core.interfaces.IPlatformDataProcessor
import com.tiktok.sparkling.method.registry.core.model.context.ContextProviderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BaseProcessorTest {
    /**
     * Stub bridge whose `realHandle` invokes the provided callback synchronously
     * with whatever data the test wishes to surface.
     */
    private open class StubBridge(
        override val name: String = "stub.bridge",
    ) : IDLBridgeMethod {
        var realHandleCalled: Boolean = false
        var nextResult: Map<String, Any?> = mapOf(IDLBridgeMethod.PARAM_CODE to IDLBridgeMethod.SUCCESS)
        var throwOnHandle: Throwable? = null

        override fun realHandle(
            params: Map<String, Any?>,
            callback: IDLBridgeMethod.Callback,
            type: BridgePlatformType,
        ) {
            realHandleCalled = true
            throwOnHandle?.let { throw it }
            callback.invoke(nextResult)
        }

        override fun setProviderFactory(contextProviderFactory: ContextProviderFactory?) = Unit

        override fun setBridgeContext(bridgeContext: IBridgeContext) = Unit
    }

    /**
     * Test-only processor that mimics the real `WebPlatformDataProcessor` interface
     * but lets us drive the success / failure paths deterministically.
     */
    private class FakeProcessor(
        var transformInput: ((Any?) -> Map<String, Any?>?)? = { it as Map<String, Any?>? },
        var transformOutput: ((Map<String, Any?>) -> Map<String, Any?>) = { it },
        var inputThrows: Throwable? = null,
    ) : IPlatformDataProcessor<Map<String, Any?>> {
        var inputCalls = 0
        var outputCalls = 0

        override fun matchPlatformType(platformType: BridgePlatformType): Boolean = true

        override fun transformPlatformDataToMap(
            params: Map<String, Any?>,
            clazz: Class<out IDLBridgeMethod>,
        ): Map<String, Any?>? {
            inputCalls++
            inputThrows?.let { throw it }
            return transformInput?.invoke(params)
        }

        override fun transformMapToPlatformData(
            params: Map<String, Any?>,
            clazz: Class<out IDLBridgeMethod>,
        ): Map<String, Any?> {
            outputCalls++
            return transformOutput.invoke(params)
        }
    }

    private class TestProcessor(
        bridge: IDLBridgeMethod,
        data: Map<String, Any?>,
        override val processor: FakeProcessor,
    ) : BaseProcessor<Map<String, Any?>>(bridge, data) {
        override fun getPlatformType(): BridgePlatformType = BridgePlatformType.WEB

        override fun onError(
            code: Int,
            message: String,
        ): Map<String, Any?> = mapOf(IDLBridgeMethod.PARAM_CODE to code, IDLBridgeMethod.PARAM_MSG to message)
    }

    private class CapturingCallback : IBridgeMethodCallback {
        var lastResult: Any? = null

        override fun onBridgeResult(parcel: Any) {
            lastResult = parcel
        }
    }

    @Test
    fun handleDispatchesSuccessThroughProcessor() {
        val bridge =
            StubBridge().apply {
                nextResult =
                    mapOf(
                        IDLBridgeMethod.PARAM_CODE to IDLBridgeMethod.SUCCESS,
                        IDLBridgeMethod.PARAM_DATA to mapOf("k" to "v"),
                    )
            }
        val processor = FakeProcessor()
        val target = TestProcessor(bridge, mapOf("input" to 1), processor)
        val callback = CapturingCallback()

        target.handle(callback)

        assertTrue(bridge.realHandleCalled)
        assertEquals(1, processor.inputCalls)
        assertEquals(1, processor.outputCalls)
        @Suppress("UNCHECKED_CAST")
        val result = callback.lastResult as Map<String, Any?>
        assertEquals(IDLBridgeMethod.SUCCESS, result[IDLBridgeMethod.PARAM_CODE])
    }

    @Test
    fun handleDispatchesFailureCodeThroughProcessor() {
        val bridge =
            StubBridge().apply {
                nextResult = mapOf(IDLBridgeMethod.PARAM_CODE to IDLBridgeMethod.FAIL)
            }
        val processor = FakeProcessor()
        val target = TestProcessor(bridge, mapOf("input" to 1), processor)
        val callback = CapturingCallback()

        target.handle(callback)

        @Suppress("UNCHECKED_CAST")
        val result = callback.lastResult as Map<String, Any?>
        assertEquals(IDLBridgeMethod.FAIL, result[IDLBridgeMethod.PARAM_CODE])
    }

    @Test
    fun handleErrorsWhenTransformInputReturnsNull() {
        val bridge = StubBridge()
        val processor = FakeProcessor(transformInput = { null })
        val target = TestProcessor(bridge, mapOf("input" to 1), processor)
        val callback = CapturingCallback()

        target.handle(callback)

        @Suppress("UNCHECKED_CAST")
        val result = callback.lastResult as Map<String, Any?>
        assertEquals(IDLBridgeMethod.ANNOTATION_ERROR, result[IDLBridgeMethod.PARAM_CODE])
        assertTrue((result[IDLBridgeMethod.PARAM_MSG] as String).contains("transformPlatformDataToMap"))
    }

    @Test
    fun handleConvertsIllegalInputParamExceptionIntoInvalidParam() {
        val bridge = StubBridge()
        val processor = FakeProcessor(inputThrows = IllegalInputParamException("bad-input"))
        val target = TestProcessor(bridge, mapOf("input" to 1), processor)
        val callback = CapturingCallback()

        target.handle(callback)

        @Suppress("UNCHECKED_CAST")
        val result = callback.lastResult as Map<String, Any?>
        assertEquals(IDLBridgeMethod.INVALID_PARAM, result[IDLBridgeMethod.PARAM_CODE])
    }

    @Test
    fun handleConvertsIllegalOutputParamExceptionIntoInvalidResult() {
        val bridge = StubBridge()
        val processor = FakeProcessor(inputThrows = IllegalOutputParamException("bad-output"))
        val target = TestProcessor(bridge, mapOf("input" to 1), processor)
        val callback = CapturingCallback()

        target.handle(callback)

        @Suppress("UNCHECKED_CAST")
        val result = callback.lastResult as Map<String, Any?>
        assertEquals(IDLBridgeMethod.INVALID_RESULT, result[IDLBridgeMethod.PARAM_CODE])
    }

    @Test
    fun handleConvertsIllegalOperationExceptionIntoIllegalOperationError() {
        val bridge = StubBridge()
        val processor = FakeProcessor(inputThrows = IllegalOperationException("nope"))
        val target = TestProcessor(bridge, mapOf("input" to 1), processor)
        val callback = CapturingCallback()

        target.handle(callback)

        @Suppress("UNCHECKED_CAST")
        val result = callback.lastResult as Map<String, Any?>
        assertEquals(IDLBridgeMethod.ILLEGAL_OPERATION_ERROR, result[IDLBridgeMethod.PARAM_CODE])
    }

    @Test
    fun handleConvertsUnknownThrowableIntoUnknownError() {
        val bridge = StubBridge()
        val processor = FakeProcessor(inputThrows = RuntimeException("oops"))
        val target = TestProcessor(bridge, mapOf("input" to 1), processor)
        val callback = CapturingCallback()

        target.handle(callback)

        @Suppress("UNCHECKED_CAST")
        val result = callback.lastResult as Map<String, Any?>
        assertEquals(IDLBridgeMethod.UNKNOWN_ERROR, result[IDLBridgeMethod.PARAM_CODE])
    }
}
