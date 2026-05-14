// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.api.util

import com.tiktok.sparkling.method.registry.api.Utils
import io.mockk.unmockkAll
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
        // Setup if needed
    }

    @After
    fun tearDown() {
        unmockkAll()
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
}
