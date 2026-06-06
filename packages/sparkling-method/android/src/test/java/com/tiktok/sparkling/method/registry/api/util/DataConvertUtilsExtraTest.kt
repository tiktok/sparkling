// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.registry.api.util

import com.lynx.react.bridge.JavaOnlyArray
import com.lynx.react.bridge.JavaOnlyMap
import com.tiktok.sparkling.method.registry.api.Utils
import com.tiktok.sparkling.method.registry.core.ReadableMapImpl
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Extra coverage for {@link DataConvertUtils}, focusing on branches the
 * existing test suite does not exercise (Float/Long handling, X* readable
 * collections, JSON null payloads, etc.).
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DataConvertUtilsExtraTest {
    @Before
    fun reset() {
        DataConvertUtils.disableLongToDouble = false
        DataConvertUtils.newInputTypeChange = false
    }

    @After
    fun tearDown() {
        DataConvertUtils.disableLongToDouble = false
        DataConvertUtils.newInputTypeChange = false
    }

    @Test
    fun mapSupportPiperdataCoversAllPrimitiveBranches() {
        val source =
            mapOf<String, Any?>(
                "long" to 5L,
                "float" to 1.5f,
                "int" to 7,
                "double" to 2.0,
                "string" to "x",
                "bool" to true,
                "json" to JSONObject().put("k", "v"),
                "arr" to JSONArray().put(1).put(2),
                "nested" to mapOf("k" to 1),
                "list" to listOf(1, 2),
                "ignored" to Any(),
            )
        DataConvertUtils.disableLongToDouble = false
        val asDouble = DataConvertUtils.mapSupportPiperdataToJSON(source)
        assertEquals(5.0, asDouble.getDouble("long"), 0.001)
        assertEquals(1.5, asDouble.getDouble("float"), 0.001)
        assertEquals(7, asDouble.getInt("int"))
        assertEquals(2.0, asDouble.getDouble("double"), 0.001)
        assertEquals("x", asDouble.getString("string"))
        assertTrue(asDouble.getBoolean("bool"))
        assertEquals("v", asDouble.getJSONObject("json").getString("k"))
        assertEquals(2, asDouble.getJSONArray("arr").length())
        assertEquals(1, asDouble.getJSONObject("nested").getInt("k"))
        assertEquals(2, asDouble.getJSONArray("list").length())
        assertFalse(asDouble.has("ignored"))

        DataConvertUtils.disableLongToDouble = true
        val asLong = DataConvertUtils.mapSupportPiperdataToJSON(source)
        assertEquals(5L, asLong.getLong("long"))
    }

    @Test
    fun listSupportPiperdataCoversAllPrimitiveBranches() {
        val nested: List<Any> = listOf(1, "x")
        val source: List<Any> =
            listOf(
                1.5f,
                42L,
                3,
                4.0,
                "hi",
                true,
                mapOf("k" to 1),
                nested,
                Any(),
            )

        DataConvertUtils.disableLongToDouble = false
        val asDouble = DataConvertUtils.listSupportPiperdataToJSON(source)
        // Float -> Double
        assertEquals(1.5, asDouble.getDouble(0), 0.001)
        // Long -> Double when flag is false
        assertEquals(42.0, asDouble.getDouble(1), 0.001)
        assertEquals(3, asDouble.getInt(2))
        assertEquals(4.0, asDouble.getDouble(3), 0.001)
        assertEquals("hi", asDouble.getString(4))
        assertTrue(asDouble.getBoolean(5))
        assertEquals(1, asDouble.getJSONObject(6).getInt("k"))
        assertEquals(2, asDouble.getJSONArray(7).length())

        DataConvertUtils.disableLongToDouble = true
        val asLong = DataConvertUtils.listSupportPiperdataToJSON(source)
        assertEquals(42L, asLong.getLong(1))
    }

    @Test
    fun listToJsonHandlesFloatAndLongFlag() {
        val source: List<Any> = listOf(1.5f, 42L, "x", true, mapOf("k" to 1), listOf(1, 2))

        DataConvertUtils.disableLongToDouble = false
        val asDouble = DataConvertUtils.listToJSON(source)
        assertEquals(1.5, asDouble.getDouble(0), 0.001)
        assertEquals(42.0, asDouble.getDouble(1), 0.001)
        assertEquals("x", asDouble.getString(2))
        assertTrue(asDouble.getBoolean(3))
        assertNotNull(asDouble.getJSONObject(4))
        assertNotNull(asDouble.getJSONArray(5))

        DataConvertUtils.disableLongToDouble = true
        val asLong = DataConvertUtils.listToJSON(source)
        assertEquals(42L, asLong.getLong(1))
    }

    @Test
    fun convertJsonToMapAndArrayHandleAllJsonTypes() {
        val obj =
            JSONObject().apply {
                put("nested", JSONObject().put("k", "v"))
                put("array", JSONArray().put(1).put("x"))
                put("bool", true)
                put("int", 1)
                put("long", Int.MAX_VALUE.toLong() + 1L)
                put("double", 3.14)
                put("string", "hello")
                put("null", JSONObject.NULL)
                put("other", 1.5f.toDouble()) // Floats stored as Doubles
            }
        DataConvertUtils.disableLongToDouble = true
        val map = DataConvertUtils.convertJsonToMap(obj)
        assertNotNull(map.getMap("nested"))
        assertNotNull(map.getArray("array"))
        assertTrue(map.getBoolean("bool"))
        assertEquals(1, map.getInt("int"))
        assertEquals(Int.MAX_VALUE.toLong() + 1L, map.getLong("long"))
        assertEquals(3.14, map.getDouble("double"), 0.001)
        assertEquals("hello", map.getString("string"))
        assertTrue(map.isNull("null"))

        DataConvertUtils.disableLongToDouble = false
        val map2 = DataConvertUtils.convertJsonToMap(obj)
        assertEquals(
            (Int.MAX_VALUE.toLong() + 1L).toDouble(),
            map2.getDouble("long"),
            0.001,
        )
    }

    @Test
    fun convertJsonToArrayCoversNullAndPrimitiveBranches() {
        val arr =
            JSONArray().apply {
                put(JSONObject().put("k", "v"))
                put(JSONArray().put(1))
                put(true)
                put(42)
                put(Int.MAX_VALUE.toLong() + 1L)
                put(2.5)
                put("hi")
                put(JSONObject.NULL)
            }
        DataConvertUtils.disableLongToDouble = true
        val out = DataConvertUtils.convertJsonToArray(arr)
        assertNotNull(out.getMap(0))
        assertNotNull(out.getArray(1))
        assertTrue(out.getBoolean(2))
        assertEquals(42, out.getInt(3))
        assertEquals(Int.MAX_VALUE.toLong() + 1L, out.getLong(4))
        assertEquals(2.5, out.getDouble(5), 0.001)
        assertEquals("hi", out.getString(6))
        assertTrue(out.isNull(7))
    }

    @Test
    fun convertMapToReadableMapCoversFloatJsonAndElseBranches() {
        val source =
            mapOf<String, Any?>(
                "float" to 1.5f,
                "json" to JSONObject().put("k", "v"),
                "arr" to JSONArray().put(1),
                "other" to TestRefValue("x"),
            )
        val map = DataConvertUtils.convertMapToReadableMap(source)
        assertEquals(1.5, map.getDouble("float"), 0.001)
        assertNotNull(map.getMap("json"))
        assertNotNull(map.getArray("arr"))
        assertEquals("toString:x", map.getString("other"))
    }

    @Test
    fun convertArrayToWritableArrayCoversFloatBranch() {
        DataConvertUtils.disableLongToDouble = false
        val arr = DataConvertUtils.convertArrayToWritableArray(listOf(1.5f, "x"))
        assertEquals(1.5, arr.getDouble(0), 0.001)
        assertEquals("x", arr.getString(1))
    }

    @Test
    fun convertXReadableMapAndArrayHandleAllTypes() {
        val nestedMap = JSONObject().put("inner", "v")
        val nestedArray = JSONArray().put("a").put("b")
        val source =
            JSONObject().apply {
                put("string", "hi")
                put("int", 1)
                put("number", 1.5)
                put("bool", true)
                put("array", nestedArray)
                put("map", nestedMap)
            }

        val xMap = ReadableMapImpl(source)
        val converted = DataConvertUtils.convertXReadableMapToReadableMap(xMap)
        assertNotNull(converted)

        // Build an XReadableArray via ReadableArrayImpl through the parent map.
        val arr =
            JSONArray().apply {
                put("hi")
                put(1)
                put(2.5)
                put(true)
                put(nestedArray)
                put(nestedMap)
            }
        val xArr =
            com.tiktok.sparkling.method.registry.core
                .ReadableArrayImpl(arr)
        val convertedArr = DataConvertUtils.convertXReadableArrayToReadableArray(xArr)
        assertNotNull(convertedArr)
    }

    @Test
    fun getValueRecursivelyReducesReadableMaps() {
        val inner =
            JavaOnlyMap().apply {
                putString("name", "sparkling")
            }
        val outer =
            JavaOnlyMap().apply {
                putMap("inner", inner)
                putString("note", "ok")
            }

        @Suppress("UNCHECKED_CAST")
        val flattened = Utils.getValue(outer) as Map<String, Any?>
        assertEquals("ok", flattened["note"])
        @Suppress("UNCHECKED_CAST")
        val innerMap = flattened["inner"] as Map<String, Any?>
        assertEquals("sparkling", innerMap["name"])
    }

    @Test
    fun getValueReturnsValueAsIsForNonNumberAndCollections() {
        val s = "hi"
        assertEquals(s, Utils.getValue(s))
        assertNull(Utils.getValue(null))
        // Boolean is not Number; should be passed through.
        assertEquals(true, Utils.getValue(true))
    }

    @Test
    fun convertMapAndArrayHandleNullValues() {
        val map = DataConvertUtils.convertMapToReadableMap(mapOf("a" to null))
        assertTrue(map.isNull("a"))

        val arr = JavaOnlyArray()
        arr.pushNull()
        val converted = Utils.getValue(arr)
        assertEquals(listOf<Any?>(null), converted)
    }

    private class TestRefValue(
        val tag: String,
    ) {
        override fun toString(): String = "toString:$tag"
    }
}
