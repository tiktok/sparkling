// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool

import com.tiktok.sparkling.method.protocol.entity.Platform
import com.lynx.react.bridge.Callback
import com.lynx.react.bridge.JavaOnlyMap
import com.tiktok.sparkling.debug.SparklingDebugToolRegistry
import com.tiktok.sparkling.debugtool.inspect.MethodInvocation
import com.tiktok.sparkling.debugtool.inspect.MethodInvocationStore
import com.tiktok.sparkling.method.protocol.BridgeContext
import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.protocol.entity.BridgeResult
import com.tiktok.sparkling.method.protocol.handler.BridgeThreadDispatcher
import com.tiktok.sparkling.method.protocol.impl.interceptor.BridgeMockInterceptor
import com.tiktok.sparkling.method.protocol.impl.lynx.RealLynxBridgeDelegate
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeHandler
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeMethodCallback
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeProtocol
import com.tiktok.sparkling.method.protocol.utils.BridgeConverter
import com.tiktok.sparkling.method.registry.api.SparklingBridge
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Real delegate, dispatcher, lifecycle, provider and store; only dispatch timing/native work are controlled. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class SparklingDebugToolProviderImplTest {
    private lateinit var previousDispatcher: BridgeThreadDispatcher
    private var previousProviders: Any? = null
    private val providersField = SparklingDebugToolRegistry::class.java
        .getDeclaredField("testProviders").apply { isAccessible = true }

    @Before
    fun setUp() {
        previousDispatcher = SparklingBridge.bridgeThreadDispatcher
        SparklingBridge.bridgeThreadDispatcher = object : BridgeThreadDispatcher() {
            override fun dispatchLynxBridgeThread(call: BridgeCall, jsbRealCallBlock: (Boolean) -> Unit?) {
                jsbRealCallBlock(true)
            }
        }
        previousProviders = providersField.get(SparklingDebugToolRegistry)
        providersField.set(SparklingDebugToolRegistry, listOf(SparklingDebugToolProviderImpl()))
        MethodInvocationStore.setCapacity(300)
        MethodInvocationStore.clear()
    }

    @After
    fun tearDown() {
        MethodInvocationStore.clear()
        MethodInvocationStore.setCapacity(300)
        providersField.set(SparklingDebugToolRegistry, previousProviders)
        SparklingBridge.bridgeThreadDispatcher = previousDispatcher
    }

    @Test
    fun successfulCallbackUpdatesThePendingRecord() = assertCompletion(code = 1)

    @Test
    fun failedCallbackUpdatesThePendingRecord() = assertCompletion(code = -6)

    @Test
    fun repeatedAttachNotifiesOncePerInvocationPhase() {
        val harness = Harness()
        harness.attachAgain()
        harness.attachAgain()
        var notifications = 0
        val listener = object : MethodInvocationStore.Listener {
            override fun onMethodInvocationsChanged() { notifications++ }
        }
        MethodInvocationStore.addListener(listener)
        try {
            val invocation = harness.call("repeated-attach")
            val pending = records().single().also(::assertPending)
            assertEquals("Repeated attach must not duplicate start notifications", 1, notifications)

            invocation.complete(1, "response")

            assertEquals("Repeated attach must not duplicate completion notifications", 2, notifications)
            assertCompleted(pending, records().single(), 1, "response")
            assertJsResult(invocation, 1, "response")
        } finally {
            MethodInvocationStore.removeListener(listener)
        }
    }

    @Test
    fun nullParamsUseNativeStartTimeAndKeepTheSameRecordId() {
        val harness = Harness()
        val before = System.currentTimeMillis()
        val invocation = harness.call("null-params", includeParams = false)
        val after = System.currentTimeMillis()
        val pending = records().single().also(::assertPending)
        assertTrue("Use the observed native start, never the -1 bridge sentinel", pending.startTimeMs in before..after)
        assertTrue(invocation.jsResults.isEmpty())

        invocation.complete(1, "response")

        assertCompleted(pending, records().single(), 1, "response")
        assertJsResult(invocation, 1, "response")
    }

    @Test
    fun sameNameCallsCompletingInReverseOrderKeepTheirOwnRecords() {
        val harness = Harness()
        val first = harness.call("first")
        val firstPending = records().single().also(::assertPending)
        val second = harness.call("second")
        val secondPending = records().single { it.id != firstPending.id }.also(::assertPending)

        second.complete(1, "second-result")
        assertEquals(2, records().size)
        assertEquals(firstPending, records().single { it.id == firstPending.id })
        assertCompleted(secondPending, records().single { it.id == secondPending.id }, 1, "second-result")
        assertTrue(first.jsResults.isEmpty())

        first.complete(-6, "first-result")
        assertEquals(2, records().size)
        assertCompleted(firstPending, records().single { it.id == firstPending.id }, -6, "first-result")
        assertCompleted(secondPending, records().single { it.id == secondPending.id }, 1, "second-result")
        assertJsResult(first, -6, "first-result")
        assertJsResult(second, 1, "second-result")
    }

    @Test
    fun repeatedNativeCallbacksDoNotCreateAnotherRecord() {
        val invocation = Harness().call("repeated")
        val pending = records().single().also(::assertPending)
        invocation.complete(1, "first")
        invocation.complete(0, "second")

        assertEquals(1, records().size)
        assertEquals(pending.id, records().single().id)
        assertNotNull(records().single().endTimeMs)
        // Debug observation must not introduce callback-once behavior in the bridge.
        assertEquals(listOf(1, 0), invocation.jsResults.map { it.getInt("code") })
    }

    @Test
    fun lateCallbackAfterClearDoesNotRestoreOrReplaceAnInvocation() {
        val harness = Harness()
        val cleared = harness.call("cleared")
        assertPending(records().single())
        MethodInvocationStore.clear()
        val current = harness.call("current")
        val currentPending = records().single().also(::assertPending)

        cleared.complete(1, "cleared-result")
        assertEquals(listOf(currentPending), records())
        assertJsResult(cleared, 1, "cleared-result")

        current.complete(1, "current-result")
        assertCompleted(currentPending, records().single(), 1, "current-result")
        assertJsResult(current, 1, "current-result")
    }

    @Test
    fun lateCallbackAfterCapacityEvictionDoesNotRestoreOrReplaceAnInvocation() {
        val harness = Harness()
        val evicted = harness.call("evicted")
        val evictedId = records().single().id
        val retainedCalls = (1..300).map { harness.call("retained-$it") }
        val retained = records()
        assertEquals(300, retained.size)
        assertTrue(retained.none { it.id == evictedId })
        retained.forEach(::assertPending)

        evicted.complete(1, "evicted-result")
        assertEquals(retained, records())
        assertJsResult(evicted, 1, "evicted-result")

        retainedCalls.first().complete(1, "retained-result")
        assertEquals(300, records().size)
        assertCompleted(retained.first(), records().first(), 1, "retained-result")
        assertEquals(retained.drop(1), records().drop(1))
    }

    @Test
    fun increasedStoreCapacityKeepsCallAssociationsUntilRecordsAreTrimmed() {
        MethodInvocationStore.setCapacity(500)
        val harness = Harness()
        val first = harness.call("first-at-capacity-500")
        val firstPending = records().single().also(::assertPending)
        repeat(300) { harness.call("retained-$it") }
        assertEquals(301, records().size)

        first.complete(1, "first-result")

        assertEquals(301, records().size)
        assertCompleted(firstPending, records().single { it.id == firstPending.id }, 1, "first-result")
        assertJsResult(first, 1, "first-result")

        MethodInvocationStore.setCapacity(300)
        val retained = records()
        assertEquals(300, retained.size)
        assertTrue(retained.none { it.id == firstPending.id })
        first.complete(0, "late-result")

        assertEquals("A trimmed call must not restore or replace a retained record", retained, records())
        assertEquals(listOf(1, 0), first.jsResults.map { it.getInt("code") })
    }

    @Test
    fun debugListenerFailuresAtStartAndCompletionDoNotDropSuccessCallback() = assertListenerIsolation(1)

    @Test
    fun debugListenerFailuresAtStartAndCompletionDoNotDropFailureCallback() = assertListenerIsolation(-6)

    @Test
    fun replacingBridgeCallKeepsOnePendingRecordAndCompletesIt() {
        val harness = Harness()
        val interceptor = harness.replaceCalls()
        val invocation = harness.call("replaced-request")
        assertNotNull(interceptor.replacement)
        assertNotSame(interceptor.original, interceptor.replacement)
        assertSame(interceptor.replacement, harness.lastNativeCall())
        val pending = records().single().also(::assertPending)
        assertTrue(invocation.jsResults.isEmpty())

        invocation.complete(1, "replaced-result")

        assertCompleted(pending, records().single(), 1, "replaced-result")
        assertJsResult(invocation, 1, "replaced-result")
    }

    @Test
    fun replacingBridgeCallLateCallbackAfterClearDoesNotRestoreHistory() {
        val harness = Harness()
        harness.replaceCalls()
        val cleared = harness.call("cleared-replacement")
        assertPending(records().single())
        MethodInvocationStore.clear()
        val current = harness.call("current-replacement")
        val currentPending = records().single().also(::assertPending)

        cleared.complete(1, "cleared-result")

        assertEquals(listOf(currentPending), records())
        assertJsResult(cleared, 1, "cleared-result")
        current.complete(-6, "current-result")
        assertCompleted(currentPending, records().single(), -6, "current-result")
        assertJsResult(current, -6, "current-result")
    }

    @Test
    fun closeEventDispatchCreatesOnePendingRecordAndCompletesIt() {
        val invocation = Harness().releaseWithCloseEvent("close-request")
        val pending = records().single().also(::assertPending)
        assertEquals("close-request", JSONObject(pending.params).getString("marker"))

        invocation.complete(1, "close-result")

        assertCompleted(pending, records().single(), 1, "close-result")
        invocation.complete(0, "repeated-close-result")
        assertEquals(pending.id, records().single().id)
        // A close observer's callback is native logging, not a callback into a destroyed JS page.
        assertTrue(invocation.jsResults.isEmpty())
    }

    @Test
    fun closeEventLateCallbackAfterClearDoesNotRestoreHistory() {
        val closed = Harness().releaseWithCloseEvent("cleared-close")
        assertPending(records().single())
        MethodInvocationStore.clear()
        val current = Harness().call("current-page")
        val currentPending = records().single().also(::assertPending)

        closed.complete(-6, "late-close-result")

        assertEquals(listOf(currentPending), records())
        assertTrue(closed.jsResults.isEmpty())
        current.complete(1, "current-result")
        assertCompleted(currentPending, records().single(), 1, "current-result")
        assertJsResult(current, 1, "current-result")
    }

    @Test
    fun directMockResultRecordsOneCompletedInvocationWithoutCallingNative() {
        val harness = Harness()
        val phases = mutableListOf<List<MethodInvocation>>()
        val listener = object : MethodInvocationStore.Listener {
            override fun onMethodInvocationsChanged() { phases += records() }
        }
        MethodInvocationStore.addListener(listener)
        try {
            val results = harness.callWithMockResult("mock-result")
            assertEquals("Mock completion must emit one start and one update", 2, phases.size)
            val pending = phases.first().single().also(::assertPending)
            assertCompleted(pending, records().single(), 1, "mock-result")
            val result = results.single()
            assertEquals(1, result.getInt("code"))
            assertEquals("mock-result", result.getJSONObject("data").getString("marker"))
        } finally {
            MethodInvocationStore.removeListener(listener)
        }
    }

    @Test
    fun nativeEventIsRecordedOnceAndStillReachesTheProtocol() {
        val payload = JSONObject().put("marker", "native-event")
        val delivered = Harness().sendEventAndCapture(payload)

        assertEquals(1, delivered.size)
        assertEquals("fixture.nativeEvent", delivered.single().first)
        assertSame(payload, delivered.single().second)
        val record = records().single()
        assertEquals("fixture.nativeEvent", record.name)
        assertEquals("native-to-js", record.namespace)
        assertEquals(1, record.code)
        assertEquals(true, record.success)
        assertNotNull(record.endTimeMs)
        assertEquals("native-event", JSONObject(record.params).getString("marker"))
    }

    private fun assertCompletion(code: Int) {
        val invocation = Harness().call("request")
        val pending = records().single().also(::assertPending)
        assertEquals("request", JSONObject(pending.params).getString("marker"))
        assertTrue(invocation.jsResults.isEmpty())

        invocation.complete(code, "response")

        assertEquals(1, records().size)
        assertCompleted(pending, records().single(), code, "response")
        assertJsResult(invocation, code, "response")
    }

    private fun assertListenerIsolation(code: Int) {
        var notifications = 0
        val listener = object : MethodInvocationStore.Listener {
            override fun onMethodInvocationsChanged() {
                notifications++
                throw IllegalStateException("Inspector listener failed")
            }
        }
        MethodInvocationStore.addListener(listener)
        try {
            val invocation = Harness().call("listener")
            val pending = records().single().also(::assertPending)
            assertEquals("Start must exercise the listener synchronously", 1, notifications)
            invocation.complete(code, "response")
            assertEquals("Completion must also exercise the listener", 2, notifications)
            assertCompleted(pending, records().single(), code, "response")
            assertJsResult(invocation, code, "response")
        } finally {
            MethodInvocationStore.removeListener(listener)
        }
    }

    private fun assertPending(record: MethodInvocation) {
        assertEquals("fixture.sameName", record.name)
        assertNull(record.endTimeMs)
        assertNull(record.result)
        assertNull(record.code)
        assertNull(record.success)
        assertNull(record.durationMs)
        assertTrue(record.startTimeMs > 0L)
    }

    private fun assertCompleted(pending: MethodInvocation, completed: MethodInvocation, code: Int, marker: String) {
        assertEquals(pending.id, completed.id)
        assertEquals(pending.params, completed.params)
        assertEquals(pending.startTimeMs, completed.startTimeMs)
        assertEquals(code, completed.code)
        assertEquals(code == 1, completed.success)
        assertNotNull(completed.endTimeMs)
        assertTrue(requireNotNull(completed.durationMs) >= 0L)
        assertEquals(marker, JSONObject(requireNotNull(completed.result)).getJSONObject("data").getString("marker"))
    }

    private fun assertJsResult(invocation: Invocation, code: Int, marker: String) {
        val result = invocation.jsResults.single()
        assertEquals(code, result.getInt("code"))
        assertEquals(marker, result.getJSONObject("data").getString("marker"))
        assertEquals(200, result.getJSONObject("data").getInt("httpStatus"))
    }

    private fun records(): List<MethodInvocation> = MethodInvocationStore.snapshot()

    private class Harness {
        private val bridge = SparklingBridge()
        private val handler = ControlledNativeHandler()

        init {
            bridge.getBridgeContext().apply {
                platform = BridgePlatformType.LYNX
                containerId = "pending-test"
                sparklingBridge = bridge
                requireNotNull(dispatcher).registerHandler(handler)
            }
            SparklingDebugToolRegistry.attachBridge(bridge)
        }

        fun attachAgain() { SparklingDebugToolRegistry.attachBridge(bridge) }

        fun replaceCalls(): ReplacingCallInterceptor = ReplacingCallInterceptor().also {
            bridge.registerJSBMockInterceptor(it)
        }

        fun lastNativeCall(): BridgeCall = handler.calls.last()

        fun releaseWithCloseEvent(marker: String): Invocation {
            val callbackIndex = handler.callbacks.size
            bridge.addClosedEventObserver(
                listOf("fixture.sameName"),
                listOf(JSONObject().put("marker", marker)),
            )
            bridge.release()
            assertEquals("Release must dispatch the close observer", callbackIndex + 1, handler.callbacks.size)
            return Invocation(handler.callbacks[callbackIndex], mutableListOf())
        }

        fun callWithMockResult(marker: String): List<JSONObject> {
            bridge.registerJSBMockInterceptor(object : BridgeMockInterceptor() {
                override fun invokeBridgeResult(originCall: BridgeCall): BridgeResult = BridgeResult(
                    JSONObject().put("code", 1).put("msg", "fixture")
                        .put("data", JSONObject().put("marker", marker).put("httpStatus", 200)),
                )
            })
            val callbackCount = handler.callbacks.size
            return dispatch("mock-request").also {
                assertEquals("A direct mock result must bypass the native handler", callbackCount, handler.callbacks.size)
            }
        }

        fun sendEventAndCapture(payload: JSONObject): List<Pair<String, Any?>> {
            val delivered = mutableListOf<Pair<String, Any?>>()
            bridge.getBridgeContext().registerProtocol(object : IBridgeProtocol(bridge.getBridgeContext()) {
                override fun sendEvent(name: String, params: Any?) { delivered += name to params }
                override fun init() = Unit
                override fun createBridgeCall(msg: String): BridgeCall = BridgeCall(mContext).apply {
                    bridgeName = msg
                    platform = Platform.Lynx
                }
            })
            bridge.sendEvent("fixture.nativeEvent", payload)
            return delivered
        }

        fun call(marker: String, includeParams: Boolean = true): Invocation {
            val callbackIndex = handler.callbacks.size
            val results = dispatch(marker, includeParams)
            assertEquals("The real dispatcher must reach the native handler", callbackIndex + 1, handler.callbacks.size)
            return Invocation(handler.callbacks[callbackIndex], results)
        }

        private fun dispatch(marker: String, includeParams: Boolean = true): MutableList<JSONObject> {
            val results = mutableListOf<JSONObject>()
            val request = if (includeParams) {
                BridgeConverter.convertJSONObject2JavaOnlyMap(JSONObject().put("marker", marker))
            } else null
            RealLynxBridgeDelegate(bridge.getBridgeContext()).call(
                "fixture.sameName",
                request,
                object : Callback {
                    override fun invoke(vararg args: Any?) {
                        results += BridgeConverter.revertJavaOnlyMap2JSONObject(args.single() as JavaOnlyMap)
                    }
                },
                "lynx",
            )
            return results
        }
    }

    private class ControlledNativeHandler : IBridgeHandler {
        val callbacks = mutableListOf<IBridgeMethodCallback>()
        val calls = mutableListOf<BridgeCall>()
        override fun handle(bridgeContext: BridgeContext, call: BridgeCall, callback: IBridgeMethodCallback) {
            calls += call
            callbacks += callback
        }
        override fun onRelease() = Unit
        override fun isReleased(): Boolean = false
    }

    private class ReplacingCallInterceptor : BridgeMockInterceptor() {
        var original: BridgeCall? = null
        var replacement: BridgeCall? = null

        override fun interceptBridgeCall(originCall: BridgeCall): BridgeCall {
            original = originCall
            return BridgeCall(originCall.context).apply {
                bridgeName = originCall.bridgeName
                params = originCall.params
                platform = originCall.platform
                nameSpace = originCall.nameSpace
                callbackId = originCall.callbackId
                url = originCall.url
                timestamp = originCall.timestamp
                bridgeCallThreadType = originCall.bridgeCallThreadType
            }.also { replacement = it }
        }
    }

    private class Invocation(
        private val callback: IBridgeMethodCallback,
        val jsResults: MutableList<JSONObject>,
    ) {
        fun complete(code: Int, marker: String) {
            callback.onBridgeResult(
                JSONObject().put("code", code).put("msg", "fixture")
                    .put("data", JSONObject().put("marker", marker).put("httpStatus", 200)),
            )
        }
    }
}
