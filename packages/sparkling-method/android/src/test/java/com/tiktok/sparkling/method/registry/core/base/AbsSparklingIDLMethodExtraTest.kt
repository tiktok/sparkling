// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.registry.core.base

import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IBridgeContext
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamField
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamModel
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodResultModel
import com.tiktok.sparkling.method.registry.core.model.context.ContextProviderFactory
import com.tiktok.sparkling.method.registry.core.model.idl.CompletionBlock
import com.tiktok.sparkling.method.registry.core.model.idl.IDLDynamic
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseParamModel
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseResultModel
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the corners of [AbsSparklingIDLMethod] that the existing test misses:
 * the dynamic param proxy `toJSON`/`toString`, the failure/raw-success branches
 * of [createCompletionBlockProxy], `getXValue` helpers, and the case where
 * params class can't be located in the registry cache.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AbsSparklingIDLMethodExtraTest {
    private lateinit var method: TestMethod
    private lateinit var collected: MutableList<Map<String, Any?>>

    @Before
    fun setUp() {
        method = TestMethod()
        collected = mutableListOf()
    }

    private fun callbackOf(): IDLBridgeMethod.Callback =
        object : IDLBridgeMethod.Callback {
            override fun invoke(data: Map<String, Any?>) {
                collected += data
            }
        }

    private fun fakeDynamic(
        type: com.tiktok.sparkling.method.registry.core.model.idl.DynamicType,
        boolean: Boolean = false,
        int: Int = 0,
        long: Long = 0L,
        string: String = "",
    ) = object : IDLDynamic {
        override fun isNull() = type == com.tiktok.sparkling.method.registry.core.model.idl.DynamicType.Null

        override fun asBoolean() = boolean

        override fun asDouble() = 0.0

        override fun asInt() = int

        override fun asLong() = long

        override fun asString() = string

        override fun asArray() = emptyList<Any>()

        override fun asMap() = emptyMap<String, Any>()

        override fun asByteArray() = ByteArray(0)

        override fun getType() = type

        override fun recycle() = Unit
    }

    @Test
    fun getXValueExtractsIDLDynamicValue() {
        val dyn =
            fakeDynamic(
                com.tiktok.sparkling.method.registry.core.model.idl.DynamicType.Int,
                int = 42,
            )
        assertEquals(42, method.getXValue(dyn))
        assertEquals("plain", method.getXValue("plain"))
        assertNull(method.getXValue(null))
    }

    @Test
    fun getXValueLookupOnMapHandlesIDLDynamicAndPlain() {
        val dyn =
            fakeDynamic(
                com.tiktok.sparkling.method.registry.core.model.idl.DynamicType.Boolean,
                boolean = true,
            )
        val data = mapOf<String, Any>("dyn" to dyn, "raw" to 7)
        assertEquals(true, method.getXValue(data, "dyn"))
        assertEquals(7, method.getXValue(data, "raw"))
        assertNull(method.getXValue(data, "missing"))
    }

    @Test
    fun toJSONFromIDLMethodBaseModelFallsBackToEmptyForNull() {
        val empty = method.toJSON(null as com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel?)
        assertEquals(0, empty.length())
    }

    @Test
    fun shouldUseOriginalDataDefaultIsFalse() {
        // Default impl is `return false`; calling it directly should not throw.
        val flag = method.shouldUseOriginalData(null)
        assertEquals(false, flag)
    }

    @Test
    fun completionBlockProxyForwardsSuccessFailureAndRawSuccess() {
        val proxy =
            method.createCompletionBlockProxy<TestMethod.TestResultModel>(
                this::class.java.classLoader!!,
                callbackOf(),
            )
        proxy.onSuccess(TestMethod.TestResultModel("ok"), "done")
        proxy.onFailure(7, "boom", TestMethod.TestResultModel("nope"))
        proxy.onRawSuccess(TestMethod.TestResultModel("raw"))

        assertEquals(3, collected.size)
        assertEquals(IDLBridgeMethod.SUCCESS, collected[0][IDLBridgeMethod.PARAM_CODE])
        assertEquals(7, collected[1][IDLBridgeMethod.PARAM_CODE])
        assertEquals("raw", collected[2]["result"])
    }

    @Test
    fun completionBlockProxyHandlesNullDataInFailureAndRawSuccess() {
        val proxy =
            method.createCompletionBlockProxy<TestMethod.TestResultModel>(
                this::class.java.classLoader!!,
                callbackOf(),
            )
        proxy.onFailure(99, "explode", null)
        proxy.onRawSuccess(null)
        assertEquals(2, collected.size)
        assertEquals(99, collected[0][IDLBridgeMethod.PARAM_CODE])
        // raw success with null data is delivered as an empty map
        assertTrue((collected[1] as Map<*, *>).isEmpty())
    }

    @Test
    fun realHandleSerializesAllPrimitiveTypesViaProxyToJSON() {
        method.captureToJsonResult = true
        val params =
            mapOf<String, Any?>(
                "p1" to "string",
                "p2" to 1,
                "p3" to 2L,
                "p4" to 3.14,
                "p5" to true,
                "p6" to listOf("x", 1),
                "p7" to mapOf("k" to "v"),
                "p8" to null, // null is filtered out
            )
        method.realHandle(params, callbackOf(), BridgePlatformType.WEB)
        assertNotNull(method.lastToJsonResult)
        val json = method.lastToJsonResult!!
        assertEquals("string", json.optString("p1"))
        assertEquals(1, json.optInt("p2"))
        assertEquals(2L, json.optLong("p3"))
        assertEquals(3.14, json.optDouble("p4"), 0.0001)
        assertEquals(true, json.optBoolean("p5"))
        assertTrue(json.opt("p6") is JSONArray)
        assertTrue(json.opt("p7") is JSONObject)
        assertTrue(!json.has("p8"))
    }

    @Test
    fun realHandleProxiesToStringContainsDataSource() {
        method.captureToStringResult = true
        method.realHandle(mapOf("k" to "v"), callbackOf(), BridgePlatformType.WEB)
        assertNotNull(method.lastToStringResult)
        assertTrue(method.lastToStringResult!!.contains("dataSource"))
    }

    @Test
    fun realHandleFailsWhenParamModelIsMissing() {
        // A class without an inner @IDLMethodParamModel will throw IllegalStateException
        val bad = NoModelMethod()
        val captured = mutableListOf<Map<String, Any?>>()
        val cb =
            object : IDLBridgeMethod.Callback {
                override fun invoke(data: Map<String, Any?>) {
                    captured += data
                }
            }
        try {
            bad.realHandle(emptyMap(), cb, BridgePlatformType.WEB)
        } catch (_: IllegalStateException) {
            // expected
        }
        // No callback delivered (the proxy throws before callback fires).
        assertTrue(captured.isEmpty())
    }

    // ----------------------------------------------------------------------

    private open class TestMethod : AbsSparklingIDLMethod<TestMethod.TestParamModel, TestMethod.TestResultModel>() {
        override val name: String = "test.method"
        var lastToJsonResult: JSONObject? = null
        var lastToStringResult: String? = null
        var captureToJsonResult = false
        var captureToStringResult = false

        override fun handle(
            params: TestParamModel,
            callback: CompletionBlock<TestResultModel>,
            type: BridgePlatformType,
        ) {
            if (captureToJsonResult) {
                lastToJsonResult = params.toJSON()
            }
            if (captureToStringResult) {
                lastToStringResult = params.toString()
            }
            callback.onSuccess(TestResultModel("ok"), "done")
        }

        @IDLMethodParamModel
        interface TestParamModel : IDLMethodBaseParamModel {
            @get:IDLMethodParamField(keyPath = "p1")
            val p1: String?

            @get:IDLMethodParamField(keyPath = "p2")
            val p2: Number?

            @get:IDLMethodParamField(keyPath = "p3")
            val p3: Number?

            @get:IDLMethodParamField(keyPath = "p4")
            val p4: Number?

            @get:IDLMethodParamField(keyPath = "p5")
            val p5: Boolean?

            @get:IDLMethodParamField(keyPath = "p6")
            val p6: List<Any?>?

            @get:IDLMethodParamField(keyPath = "p7")
            val p7: Map<String, Any?>?

            @get:IDLMethodParamField(keyPath = "p8")
            val p8: String?
        }

        @IDLMethodResultModel
        class TestResultModel(
            private val value: String,
        ) : IDLMethodBaseResultModel {
            override fun convert(): MutableMap<String, Any> = mutableMapOf("result" to value)

            override fun toJSON(): JSONObject = JSONObject().put("result", value)
        }
    }

    private class NoModelMethod : AbsSparklingIDLMethod<NoModelMethod.NoModel, NoModelMethod.NoResult>() {
        override val name: String = "no.model"

        override fun handle(
            params: NoModel,
            callback: CompletionBlock<NoResult>,
            type: BridgePlatformType,
        ) = Unit

        // Note: deliberately does NOT annotate with @IDLMethodParamModel so getParamsClazz throws.
        interface NoModel : IDLMethodBaseParamModel

        class NoResult : IDLMethodBaseResultModel {
            override fun convert(): MutableMap<String, Any> = mutableMapOf()

            override fun toJSON(): JSONObject = JSONObject()
        }
    }
}
