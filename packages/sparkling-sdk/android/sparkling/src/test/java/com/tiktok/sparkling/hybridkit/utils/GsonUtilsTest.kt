// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.utils

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.tiktok.sparkling.hybridkit.utils.GsonUtils
import org.junit.Assert.*
import org.junit.Test

class GsonUtilsTest {
    data class TestData(
        val name: String,
        val value: Int,
    )

    @Test
    fun testToJsonWithValidObject() {
        val testData = TestData("test", 42)
        val result = GsonUtils.toJson(testData)

        assertNotNull(result)
        assertTrue(result!!.contains("test"))
        assertTrue(result.contains("42"))
    }

    @Test
    fun testToJsonWithNull() {
        val result = GsonUtils.toJson(null)
        assertEquals("null", result)
    }

    @Test
    fun testFromJsonWithValidJsonString() {
        val jsonString = """{"name":"test","value":42}"""
        val result = GsonUtils.fromJson(jsonString, TestData::class.java)

        assertEquals("test", result.name)
        assertEquals(42, result.value)
    }

    @Test
    fun testFromJsonWithJsonElement() {
        val jsonString = """{"name":"test","value":42}"""
        val jsonElement: JsonElement = JsonParser.parseString(jsonString)
        val result = GsonUtils.fromJson(jsonElement, TestData::class.java)

        assertEquals("test", result.name)
        assertEquals(42, result.value)
    }

    @Test
    fun testFromJsonWithNullJsonString() {
        val result = GsonUtils.fromJson(null as String?, TestData::class.java)
        assertNull(result)
    }

    @Test
    fun testFromJsonWithNullJsonElement() {
        val result = GsonUtils.fromJson(null as JsonElement?, TestData::class.java)
        assertNull(result)
    }

    @Test
    fun testFromJsonWithPrimitiveTypes() {
        val jsonString = "42"
        val result = GsonUtils.fromJson(jsonString, Int::class.java)
        assertEquals(42, result)
    }

    @Test
    fun testFromJsonWithString() {
        val jsonString = "\"hello\""
        val result = GsonUtils.fromJson(jsonString, String::class.java)
        assertEquals("hello", result)
    }
}
