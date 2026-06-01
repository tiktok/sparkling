// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.api.util

import com.tiktok.sparkling.method.registry.api.Utils
import com.lynx.react.bridge.JavaOnlyArray
import com.lynx.react.bridge.JavaOnlyMap
import io.mockk.unmockkAll
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DataConvertUtilsTest {
    @Before
    fun setUp() {
        DataConvertUtils.disableLongToDouble = false
        DataConvertUtils.newInputTypeChange = false
    }

    @After
    fun tearDown() {
        DataConvertUtils.disableLongToDouble = false
        DataConvertUtils.newInputTypeChange = false
        unmockkAll()
    }

    @Test
    fun testMapSupportPiperdataToJSONRespectsLongFlag() {
        val source = mapOf("longValue" to 42L)

        DataConvertUtils.disableLongToDouble = false
        val asDouble = Utils.mapSupportPiperdataToJSON(source)
        assertEquals(42.0, asDouble.getDouble("longValue"), 0.001)

        DataConvertUtils.disableLongToDouble = true
        val asLong = Utils.mapSupportPiperdataToJSON(source)
        assertEquals(42L, asLong.getLong("longValue"))
    }

    @Test
    fun testConvertMapToReadableMapSupportsNestedAndNull() {
        val source =
            mapOf<String, Any?>(
                "nested" to mapOf("inner" to 1),
                "list" to listOf("a", 2),
                "nullValue" to null,
                "bool" to true,
            )

        val writableMap = Utils.convertMapToReadableMap(source)

        val nested = writableMap.getMap("nested")
        assertNotNull(nested)
        assertEquals(1, nested?.getInt("inner"))

        val list = writableMap.getArray("list")
        assertNotNull(list)
        assertEquals("a", list?.getString(0))
        assertEquals(2, list?.getInt(1))

        assertTrue(writableMap.hasKey("nullValue"))
        assertTrue(writableMap.isNull("nullValue"))
        assertTrue(writableMap.getBoolean("bool"))
    }

    @Test
    fun testGetValueLongBehaviorDependsOnInputFlag() {
        DataConvertUtils.newInputTypeChange = false
        val oldBehavior = Utils.getValue(7L)
        assertTrue(oldBehavior is Int)
        assertEquals(7, oldBehavior)

        DataConvertUtils.newInputTypeChange = true
        val newBehavior = Utils.getValue(7L)
        assertTrue(newBehavior is Long)
        assertEquals(7L, newBehavior)
    }

    @Test
    fun testMapToJSONWithSimpleTypes() {
        // Given
        val testMap =
            mapOf(
                "string" to "value",
                "int" to 42,
                "double" to 3.14,
                "boolean" to true,
                "null" to null,
            )

        // When
        val result = Utils.mapToJSON(testMap)

        // Then
        assertNotNull("Result should not be null", result)
        assertEquals("String value should match", "value", result.getString("string"))
        assertEquals("Int value should match", 42, result.getInt("int"))
        assertEquals("Double value should match", 3.14, result.getDouble("double"), 0.001)
        assertTrue("Boolean value should match", result.getBoolean("boolean"))
        assertTrue("Null value should be null", result.isNull("null"))
    }

    @Test
    fun testMapToJSONWithNestedMap() {
        // Given
        val nestedMap = mapOf("nested_key" to "nested_value")
        val testMap =
            mapOf(
                "simple" to "value",
                "nested" to nestedMap,
            )

        // When
        val result = Utils.mapToJSON(testMap)

        // Then
        assertNotNull("Result should not be null", result)
        assertEquals("Simple value should match", "value", result.getString("simple"))

        val nestedJson = result.getJSONObject("nested")
        assertNotNull("Nested object should not be null", nestedJson)
        assertEquals("Nested value should match", "nested_value", nestedJson.getString("nested_key"))
    }

    @Test
    fun testMapToJSONWithList() {
        // Given
        val testList = listOf("item1", "item2", 123)
        val testMap =
            mapOf(
                "list" to testList,
                "simple" to "value",
            )

        // When
        val result = Utils.mapToJSON(testMap)

        // Then
        assertNotNull("Result should not be null", result)
        assertEquals("Simple value should match", "value", result.getString("simple"))

        val jsonArray = result.getJSONArray("list")
        assertNotNull("Array should not be null", jsonArray)
        assertEquals("Array should have 3 items", 3, jsonArray.length())
        assertEquals("First item should match", "item1", jsonArray.getString(0))
        assertEquals("Second item should match", "item2", jsonArray.getString(1))
        assertEquals("Third item should match", 123, jsonArray.getInt(2))
    }

    @Test
    fun testMapToJSONWithEmptyMap() {
        // Given
        val emptyMap = emptyMap<String, Any>()

        // When
        val result = Utils.mapToJSON(emptyMap)

        // Then
        assertNotNull("Result should not be null", result)
        assertEquals("Result should be empty JSON object", 0, result.length())
    }

    @Test
    fun testMapToJSONWithComplexNesting() {
        // Given
        val deepNestedMap =
            mapOf(
                "level3" to "deep_value",
            )
        val nestedMap =
            mapOf(
                "level2" to deepNestedMap,
                "array" to listOf(1, 2, 3),
            )
        val testMap =
            mapOf(
                "level1" to nestedMap,
                "simple" to "top_level",
            )

        // When
        val result = Utils.mapToJSON(testMap)

        // Then
        assertNotNull("Result should not be null", result)
        assertEquals("Top level value should match", "top_level", result.getString("simple"))

        val level1 = result.getJSONObject("level1")
        assertNotNull("Level 1 should not be null", level1)

        val level2 = level1.getJSONObject("level2")
        assertNotNull("Level 2 should not be null", level2)
        assertEquals("Deep value should match", "deep_value", level2.getString("level3"))

        val array = level1.getJSONArray("array")
        assertNotNull("Array should not be null", array)
        assertEquals("Array should have 3 items", 3, array.length())
    }

    @Test
    fun testMapToJSONWithSpecialCharacters() {
        // Given
        val testMap =
            mapOf(
                "unicode" to "Hello 🌍",
                "special_chars" to "!@#$%^&*()",
                "json_chars" to "\"quotes\" and {braces}",
                "empty_string" to "",
            )

        // When
        val result = Utils.mapToJSON(testMap)

        // Then
        assertNotNull("Result should not be null", result)
        assertEquals("Unicode should be preserved", "Hello 🌍", result.getString("unicode"))
        assertEquals("Special chars should be preserved", "!@#$%^&*()", result.getString("special_chars"))
        assertEquals("JSON chars should be escaped properly", "\"quotes\" and {braces}", result.getString("json_chars"))
        assertEquals("Empty string should be preserved", "", result.getString("empty_string"))
    }

    @Test
    fun testMapToJSONWithLargeNumbers() {
        // Given
        val testMap =
            mapOf(
                "large_int" to Long.MAX_VALUE,
                "small_int" to Long.MIN_VALUE,
                "large_double" to Double.MAX_VALUE,
                "small_double" to Double.MIN_VALUE,
            )

        // When
        val result = Utils.mapToJSON(testMap)

        // Then
        assertNotNull("Result should not be null", result)
        assertEquals("Large int should match", Long.MAX_VALUE, result.getLong("large_int"))
        assertEquals("Small int should match", Long.MIN_VALUE, result.getLong("small_int"))
        assertEquals("Large double should match", Double.MAX_VALUE, result.getDouble("large_double"), 0.001)
        assertEquals("Small double should match", Double.MIN_VALUE, result.getDouble("small_double"), 0.001)
    }

    @Test
    fun testMapToJSONPreservesOrder() {
        // Given
        val testMap =
            linkedMapOf(
                "first" to 1,
                "second" to 2,
                "third" to 3,
            )

        // When
        val result = Utils.mapToJSON(testMap)

        // Then
        assertNotNull("Result should not be null", result)
        assertEquals("Should have 3 keys", 3, result.length())
        assertTrue("Should contain all keys", result.has("first") && result.has("second") && result.has("third"))
    }

    @Test
    fun testMapToJSONWithMixedTypes() {
        // Given
        val testMap =
            mapOf(
                "string" to "text",
                "number" to 42,
                "boolean" to false,
                "array" to listOf("a", "b", "c"),
                "object" to mapOf("inner" to "value"),
                "null_value" to null,
            )

        // When
        val result = Utils.mapToJSON(testMap)

        // Then
        assertNotNull("Result should not be null", result)
        assertEquals("String should match", "text", result.getString("string"))
        assertEquals("Number should match", 42, result.getInt("number"))
        assertFalse("Boolean should match", result.getBoolean("boolean"))

        val array = result.getJSONArray("array")
        assertEquals("Array should have 3 items", 3, array.length())

        val obj = result.getJSONObject("object")
        assertEquals("Object should have inner value", "value", obj.getString("inner"))

        assertTrue("Null value should be null", result.isNull("null_value"))
    }

    @Test
    fun testToStringOrJsonForNullMapAndList() {
        assertEquals("", Utils.toStringOrJson(null))
        assertEquals("{\"k\":1}", Utils.toStringOrJson(mapOf("k" to 1)))
        assertEquals("[1,2,3]", Utils.toStringOrJson(listOf(1, 2, 3)))
    }

    @Test
    fun testJsonToMapAndJsonToListKeepNestedValues() {
        val obj = JSONObject("{\"name\":\"sparkling\",\"meta\":{\"a\":1},\"arr\":[1,2]}")
        val map = Utils.jsonToMap(obj)
        assertEquals("sparkling", map["name"])
        assertTrue(map["meta"] is Map<*, *>)
        assertTrue(map["arr"] is List<*>)

        val arr = JSONArray("[\"x\", {\"k\":true}, [1,2], null]")
        val list = Utils.jsonToList(arr)
        assertEquals("x", list[0])
        assertTrue(list[1] is Map<*, *>)
        assertTrue(list[2] is List<*>)
        assertNull(list[3])
    }

    @Test
    fun testConvertArrayToWritableArrayRespectsLongFlag() {
        val source =
            listOf(
                1L,
                mapOf("inner" to "v"),
                listOf(2, 3),
            )

        DataConvertUtils.disableLongToDouble = false
        val asDouble = Utils.convertArrayToWritableArray(source)
        assertEquals(1.0, asDouble.getDouble(0), 0.001)

        DataConvertUtils.disableLongToDouble = true
        val asLong = Utils.convertArrayToWritableArray(source)
        assertEquals(1L, asLong.getLong(0))
        assertEquals("v", asLong.getMap(1)?.getString("inner"))
        assertEquals(3, asLong.getArray(2)?.getInt(1))
    }

    @Test
    fun testGetValueForReadableMapAndArray() {
        val map =
            JavaOnlyMap().apply {
                putString("name", "sparkling")
                putInt("count", 2)
            }
        val arr =
            JavaOnlyArray().apply {
                pushMap(map)
                pushDouble(1.5)
            }

        val value = Utils.getValue(arr)
        assertTrue(value is List<*>)
        val list = value as List<*>
        assertTrue(list[0] is Map<*, *>)
        assertEquals(1.5, list[1] as Double, 0.001)

        DataConvertUtils.newInputTypeChange = false
        assertEquals(1, Utils.getValue(1L))
        DataConvertUtils.newInputTypeChange = true
        assertEquals(1L, Utils.getValue(1L))
    }

    @Test
    fun testConvertMapToReadableMapSupportsArrayAndJsonNull() {
        val source =
            mapOf<String, Any?>(
                "arrayValue" to arrayOf(1, "x", true),
                "jsonNull" to JSONObject.NULL,
            )

        val writableMap = Utils.convertMapToReadableMap(source)

        val array = writableMap.getArray("arrayValue")
        assertNotNull(array)
        assertEquals(1, array?.getInt(0))
        assertEquals("x", array?.getString(1))
        assertTrue(array?.getBoolean(2) == true)

        assertTrue(writableMap.hasKey("jsonNull"))
        assertTrue(writableMap.isNull("jsonNull"))
    }

    @Test
    fun testConvertJsonToArrayHandlesNestedAndNull() {
        val jsonArray = JSONArray("[1, {\"k\":\"v\"}, [2,3], null]")

        val writable = Utils.convertJsonToArray(jsonArray)

        assertEquals(1, writable.getInt(0))
        assertEquals("v", writable.getMap(1)?.getString("k"))
        assertEquals(3, writable.getArray(2)?.getInt(1))
        assertTrue(writable.isNull(3))
    }

    @Test
    fun testGetValueNumberPreservesDoubleWhenNeeded() {
        DataConvertUtils.newInputTypeChange = false
        assertEquals(1.25, Utils.getValue(1.25) as Double, 0.001)

        DataConvertUtils.newInputTypeChange = true
        assertEquals(2.5, Utils.getValue(2.5f) as Double, 0.001)
    }
}
