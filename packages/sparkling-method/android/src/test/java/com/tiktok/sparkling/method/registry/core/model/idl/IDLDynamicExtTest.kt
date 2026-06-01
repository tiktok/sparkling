// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.registry.core.model.idl

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests every [DynamicType] branch of the [IDLDynamic] extension functions
 * `getValue()` and `toPrimitiveOrJSON()`.
 */
@RunWith(RobolectricTestRunner::class)
class IDLDynamicExtTest {
    @Test
    fun getValueRoutesPerType() {
        assertEquals("hi", FakeDynamic("hi", DynamicType.String).getValue())
        assertEquals(2.5, FakeDynamic(2.5, DynamicType.Number).getValue())
        assertEquals(true, FakeDynamic(true, DynamicType.Boolean).getValue())
        assertEquals(7L, FakeDynamic(7L, DynamicType.Long).getValue())
        assertEquals(9, FakeDynamic(9, DynamicType.Int).getValue())

        @Suppress("UNCHECKED_CAST")
        val map = FakeDynamic(mapOf("k" to 1), DynamicType.Map).getValue() as Map<String, Any>
        assertEquals(1, map["k"])

        @Suppress("UNCHECKED_CAST")
        val list = FakeDynamic(listOf<Any>(1, 2), DynamicType.Array).getValue() as List<Any>
        assertEquals(2, list.size)

        val bytes = FakeDynamic(byteArrayOf(1, 2, 3), DynamicType.ByteArray).getValue() as ByteArray
        assertEquals(3, bytes.size)

        assertNull(FakeDynamic(null, DynamicType.Null).getValue())
    }

    @Test
    fun toPrimitiveOrJSONRoutesPerType() {
        assertEquals("hi", FakeDynamic("hi", DynamicType.String).toPrimitiveOrJSON())
        assertEquals(2.5, FakeDynamic(2.5, DynamicType.Number).toPrimitiveOrJSON())
        assertEquals(false, FakeDynamic(false, DynamicType.Boolean).toPrimitiveOrJSON())
        assertEquals(7L, FakeDynamic(7L, DynamicType.Long).toPrimitiveOrJSON())
        assertEquals(9, FakeDynamic(9, DynamicType.Int).toPrimitiveOrJSON())

        val mapOut = FakeDynamic(mapOf("k" to 1), DynamicType.Map).toPrimitiveOrJSON()
        assertTrue(mapOut is JSONObject)
        assertEquals(1, (mapOut as JSONObject).getInt("k"))

        val listOut = FakeDynamic(listOf<Any>(1, "two"), DynamicType.Array).toPrimitiveOrJSON()
        assertTrue(listOut is JSONArray)
        assertEquals("two", (listOut as JSONArray).getString(1))

        val bytes = FakeDynamic(byteArrayOf(9), DynamicType.ByteArray).toPrimitiveOrJSON() as ByteArray
        assertEquals(9.toByte(), bytes[0])

        assertSame(JSONObject.NULL, FakeDynamic(null, DynamicType.Null).toPrimitiveOrJSON())
    }

    @Test
    fun dynamicTypeEnumExposesAllValues() {
        // Touch values()/valueOf so the enum class itself shows up in coverage.
        assertNotNull(DynamicType.valueOf("Null"))
        assertNotNull(DynamicType.valueOf("Boolean"))
        assertNotNull(DynamicType.valueOf("Int"))
        assertNotNull(DynamicType.valueOf("Number"))
        assertNotNull(DynamicType.valueOf("String"))
        assertNotNull(DynamicType.valueOf("Map"))
        assertNotNull(DynamicType.valueOf("Array"))
        assertNotNull(DynamicType.valueOf("Long"))
        assertNotNull(DynamicType.valueOf("ByteArray"))
        assertEquals(9, DynamicType.values().size)
    }

    private class FakeDynamic(
        private val value: Any?,
        private val type: DynamicType,
    ) : IDLDynamic {
        override fun isNull(): Boolean = value == null

        override fun asBoolean(): Boolean = value as Boolean

        override fun asDouble(): Double = (value as Number).toDouble()

        override fun asInt(): Int = (value as Number).toInt()

        override fun asLong(): Long = (value as Number).toLong()

        override fun asString(): String = value as String

        @Suppress("UNCHECKED_CAST")
        override fun asArray(): List<Any> = value as List<Any>

        @Suppress("UNCHECKED_CAST")
        override fun asMap(): Map<String, Any> = value as Map<String, Any>

        override fun asByteArray(): ByteArray = value as ByteArray

        override fun getType(): DynamicType = type

        override fun recycle() {}
    }
}
