// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.registry.api.processor

import com.tiktok.sparkling.method.registry.core.IDLAnnotationData
import com.tiktok.sparkling.method.registry.core.IDLAnnotationModel
import com.tiktok.sparkling.method.registry.core.IDLDefaultValue
import com.tiktok.sparkling.method.registry.core.IDLParamField
import com.tiktok.sparkling.method.registry.core.annotation.DefaultType
import com.tiktok.sparkling.method.registry.core.exception.IllegalInputParamException
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class WebProcessorForMapExtraTest {
    interface NestedModel : IDLMethodBaseModel {
        fun getName(): String
    }

    @Test
    fun checkValueRejectsWrongStringType() {
        val data =
            annotationData(
                "v" to IDLParamField(keyPath = "v", returnType = String::class.java),
            )
        val params = JSONObject().put("v", 123)
        val ex = runCatching { WebProcessorForMap.getJavaOnlyMapParams(params, data) }.exceptionOrNull()
        assertTrue(ex is IllegalInputParamException)
    }

    @Test
    fun checkValueRejectsWrongNumberType() {
        val data =
            annotationData(
                "n" to IDLParamField(keyPath = "n", returnType = Number::class.java),
            )
        val params = JSONObject().put("n", "not-a-number")
        val ex = runCatching { WebProcessorForMap.getJavaOnlyMapParams(params, data) }.exceptionOrNull()
        assertTrue(ex is IllegalInputParamException)
    }

    @Test
    fun checkValueRejectsWrongBooleanType() {
        val data =
            annotationData(
                "b" to IDLParamField(keyPath = "b", returnType = Boolean::class.java),
            )
        val params = JSONObject().put("b", "true")
        val ex = runCatching { WebProcessorForMap.getJavaOnlyMapParams(params, data) }.exceptionOrNull()
        assertTrue(ex is IllegalInputParamException)
    }

    @Test
    fun checkValueRejectsWrongListType() {
        val data =
            annotationData(
                "l" to IDLParamField(keyPath = "l", returnType = List::class.java),
            )
        val params = JSONObject().put("l", "not-a-list")
        val ex = runCatching { WebProcessorForMap.getJavaOnlyMapParams(params, data) }.exceptionOrNull()
        assertTrue(ex is IllegalInputParamException)
    }

    @Test
    fun checkValueRejectsWrongMapType() {
        val data =
            annotationData(
                "m" to IDLParamField(keyPath = "m", returnType = Map::class.java),
            )
        val params = JSONObject().put("m", "not-a-map")
        val ex = runCatching { WebProcessorForMap.getJavaOnlyMapParams(params, data) }.exceptionOrNull()
        assertTrue(ex is IllegalInputParamException)
    }

    @Test
    fun checkValueRejectsRequiredMissingInput() {
        val data =
            annotationData(
                "n" to IDLParamField(required = true, keyPath = "n", returnType = String::class.java),
            )
        val params = JSONObject()
        val ex = runCatching { WebProcessorForMap.getJavaOnlyMapParams(params, data) }.exceptionOrNull()
        assertTrue(ex is IllegalInputParamException)
    }

    @Test
    fun checkValueAcceptsExplicitNullForNonRequired() {
        val data =
            annotationData(
                "n" to IDLParamField(keyPath = "n", returnType = String::class.java),
            )
        val params = JSONObject().put("n", JSONObject.NULL)
        val res = WebProcessorForMap.getJavaOnlyMapParams(params, data)
        assertNotNull(res)
        assertNull(res!!["n"])
    }

    @Test
    fun checkValueRejectsRequiredJsonNull() {
        val data =
            annotationData(
                "n" to IDLParamField(required = true, keyPath = "n", returnType = String::class.java),
            )
        val params = JSONObject().put("n", JSONObject.NULL)
        val ex = runCatching { WebProcessorForMap.getJavaOnlyMapParams(params, data) }.exceptionOrNull()
        assertTrue(ex is IllegalInputParamException)
    }

    @Test
    fun preCheckSeedsBooleanDefault() {
        val data =
            annotationData(
                "b" to
                    IDLParamField(
                        keyPath = "b",
                        returnType = Boolean::class.java,
                        defaultValue = IDLDefaultValue(type = DefaultType.BOOL, boolValue = true),
                    ),
            )
        val params = JSONObject()
        val res = WebProcessorForMap.getJavaOnlyMapParams(params, data)
        assertEquals(true, res?.get("b"))
    }

    @Test
    fun preCheckSeedsNumberDefaultsForLongAndDouble() {
        val longData =
            annotationData(
                "v" to
                    IDLParamField(
                        keyPath = "v",
                        returnType = Number::class.java,
                        defaultValue = IDLDefaultValue(type = DefaultType.LONG, longValue = 123L),
                    ),
            )
        val resLong = WebProcessorForMap.getJavaOnlyMapParams(JSONObject(), longData)
        assertEquals(123L, (resLong?.get("v") as Number).toLong())

        val dblData =
            annotationData(
                "v" to
                    IDLParamField(
                        keyPath = "v",
                        returnType = Number::class.java,
                        defaultValue = IDLDefaultValue(type = DefaultType.DOUBLE, doubleValue = 1.5),
                    ),
            )
        val resDbl = WebProcessorForMap.getJavaOnlyMapParams(JSONObject(), dblData)
        assertEquals(1.5, (resDbl?.get("v") as Number).toDouble(), 0.0001)
    }

    @Test
    fun preCheckSeedsStringDefault() {
        val data =
            annotationData(
                "s" to
                    IDLParamField(
                        keyPath = "s",
                        returnType = String::class.java,
                        defaultValue = IDLDefaultValue(type = DefaultType.STRING, stringValue = "fallback"),
                    ),
            )
        val res = WebProcessorForMap.getJavaOnlyMapParams(JSONObject(), data)
        assertEquals("fallback", res?.get("s"))
    }

    @Test
    fun checkValueRejectsInvalidIntEnum() {
        val data =
            annotationData(
                "n" to
                    IDLParamField(
                        keyPath = "n",
                        returnType = Number::class.java,
                        isEnum = true,
                        intEnum = listOf(1, 2),
                    ),
            )
        val ex =
            runCatching {
                WebProcessorForMap.getJavaOnlyMapParams(JSONObject().put("n", 5), data)
            }.exceptionOrNull()
        assertTrue(ex is IllegalInputParamException)
    }

    @Test
    fun checkValueAcceptsValidIntEnum() {
        val data =
            annotationData(
                "n" to
                    IDLParamField(
                        keyPath = "n",
                        returnType = Number::class.java,
                        isEnum = true,
                        intEnum = listOf(1, 2, 3),
                    ),
            )
        val res = WebProcessorForMap.getJavaOnlyMapParams(JSONObject().put("n", 2), data)
        assertEquals(2, (res?.get("n") as Number).toInt())
    }

    @Test
    fun checkValueRejectsMapEnumWithUnknownString() {
        val data =
            annotationData(
                "m" to
                    IDLParamField(
                        keyPath = "m",
                        returnType = Map::class.java,
                        isEnum = true,
                        stringEnum = listOf("on", "off"),
                    ),
            )
        val params = JSONObject().put("m", JSONObject().put("flag", "unknown"))
        val ex = runCatching { WebProcessorForMap.getJavaOnlyMapParams(params, data) }.exceptionOrNull()
        assertTrue(ex is IllegalInputParamException)
    }

    @Test
    fun checkValueAcceptsMapEnumWithKnownIntValues() {
        val data =
            annotationData(
                "m" to
                    IDLParamField(
                        keyPath = "m",
                        returnType = Map::class.java,
                        isEnum = true,
                        intEnum = listOf(1, 2),
                    ),
            )
        val params = JSONObject().put("m", JSONObject().put("a", 1))
        val res = WebProcessorForMap.getJavaOnlyMapParams(params, data)
        assertNotNull(res)
    }

    @Test
    fun convertsJsonArrayToList() {
        val data =
            annotationData(
                "items" to IDLParamField(keyPath = "items", returnType = List::class.java),
            )
        val params = JSONObject().put("items", JSONArray("[1, \"two\", true]"))
        val res = WebProcessorForMap.getJavaOnlyMapParams(params, data)
        val list = res?.get("items") as List<*>
        assertEquals(3, list.size)
        assertEquals("two", list[1])
    }

    @Test
    fun convertsJsonObjectToMap() {
        val data =
            annotationData(
                "obj" to IDLParamField(keyPath = "obj", returnType = Map::class.java),
            )
        val params = JSONObject().put("obj", JSONObject().put("k", 1))
        val res = WebProcessorForMap.getJavaOnlyMapParams(params, data)
        val map = res?.get("obj") as Map<*, *>
        assertEquals(1, (map["k"] as Number).toInt())
    }

    private fun annotationData(vararg fields: Pair<String, IDLParamField>): IDLAnnotationData {
        val model =
            IDLAnnotationModel(
                stringModel = HashMap(fields.toMap()),
            )
        return IDLAnnotationData(
            paramClass = Any::class.java,
            resultClass = Any::class.java,
            methodParamModel = model,
            methodResultModel = IDLAnnotationModel(),
            models = mapOf(IDLMethodBaseModel.Default::class.java to IDLAnnotationModel()),
        )
    }
}
