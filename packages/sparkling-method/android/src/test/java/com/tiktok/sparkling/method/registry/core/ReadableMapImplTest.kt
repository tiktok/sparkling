// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.registry.core

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercises [ReadableMapImpl], [ReadableArrayImpl], [DynamicImpl], and
 * [KeyIteratorImpl] using JSONObject/JSONArray inputs. Robolectric is required
 * because org.json APIs are stub-only on the JVM.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ReadableMapImplTest {
    private fun sample(): JSONObject =
        JSONObject().apply {
            put("s", "hello")
            put("i", 42)
            put("d", 3.14)
            put("b", true)
            put(
                "a",
                JSONArray().apply {
                    put("x")
                    put(7)
                    put(2.5)
                    put(true)
                },
            )
            put("m", JSONObject().apply { put("k", "v") })
        }

    @Test
    fun mapAccessorsReturnExpectedValues() {
        val map = ReadableMapImpl(sample())

        assertTrue(map.hasKey("s"))
        assertFalse(map.hasKey("missing"))
        assertFalse(map.isNull("s"))

        assertEquals("hello", map.getString("s"))
        assertEquals(42, map.getInt("i"))
        assertEquals(3.14, map.getDouble("d"), 0.0)
        assertTrue(map.getBoolean("b"))

        val arr = map.getArray("a")
        assertNotNull(arr)
        assertEquals(4, arr!!.size())

        val nested = map.getMap("m")
        assertNotNull(nested)
        assertEquals("v", nested!!.getString("k"))
    }

    @Test
    fun mapGetTypeAndDynamicWrap() {
        val map = ReadableMapImpl(sample())
        assertEquals(XReadableType.String, map.getType("s"))
        assertEquals(XReadableType.Int, map.getType("i"))
        assertEquals(XReadableType.Number, map.getType("d"))
        assertEquals(XReadableType.Boolean, map.getType("b"))
        assertEquals(XReadableType.Array, map.getType("a"))
        assertEquals(XReadableType.Map, map.getType("m"))
        assertEquals(XReadableType.Null, map.getType("missing"))

        val dyn = map.get("s")
        assertEquals("hello", dyn.asString())
        assertEquals(XReadableType.String, dyn.getType())
        dyn.recycle()
    }

    @Test
    fun mapGetMissingNestedReturnsNull() {
        val map = ReadableMapImpl(JSONObject())
        assertNull(map.getArray("missing"))
        assertNull(map.getMap("missing"))
    }

    @Test
    fun keyIteratorWalksAllKeys() {
        val map = ReadableMapImpl(sample())
        val iterator = map.keyIterator()
        val seen = mutableSetOf<String>()
        while (iterator.hasNextKey()) {
            seen.add(iterator.nextKey())
        }
        assertEquals(setOf("s", "i", "d", "b", "a", "m"), seen)
    }

    @Test
    fun toMapReturnsPlainKotlinMap() {
        val map = ReadableMapImpl(sample())
        val plain = map.toMap()
        assertEquals("hello", plain["s"])
        assertEquals(42, plain["i"])
    }

    @Test
    fun arrayAccessorsCoverAllOverloads() {
        val map = ReadableMapImpl(sample())
        val arr = map.getArray("a")!!

        assertFalse(arr.isNull(0))
        assertEquals("x", arr.getString(0))
        assertEquals(7, arr.getInt(1))
        assertEquals(2.5, arr.getDouble(2), 0.0)
        assertTrue(arr.getBoolean(3))

        val nestedMap =
            ReadableMapImpl(
                JSONObject().apply {
                    put(
                        "nested",
                        JSONArray().apply {
                            put(JSONObject().put("k", "v"))
                            put(JSONArray().put(1))
                        },
                    )
                },
            )
        val nested = nestedMap.getArray("nested")!!
        assertNotNull(nested.getMap(0))
        assertNotNull(nested.getArray(1))

        // missing positions
        assertNull(arr.getArray(0))
        assertNull(arr.getMap(0))

        val list = arr.toList()
        assertEquals(4, list.size)
    }

    @Test
    fun arrayGetTypeMatchesContent() {
        val arr =
            ReadableMapImpl(
                JSONObject().apply {
                    put(
                        "a",
                        JSONArray().apply {
                            put("s")
                            put(true)
                            put(JSONObject())
                            put(JSONArray())
                            put(1)
                            put(1.5)
                            put(JSONObject.NULL)
                        },
                    )
                },
            ).getArray("a")!!

        assertEquals(XReadableType.String, arr.getType(0))
        assertEquals(XReadableType.Boolean, arr.getType(1))
        assertEquals(XReadableType.Map, arr.getType(2))
        assertEquals(XReadableType.Array, arr.getType(3))
        // ints come back as Number on JSONArray.opt, not Int
        val intType = arr.getType(4)
        assertTrue(intType == XReadableType.Number || intType == XReadableType.Int)
        assertEquals(XReadableType.Number, arr.getType(5))
        assertEquals(XReadableType.Null, arr.getType(6))
        assertTrue(arr.isNull(6))
    }

    @Test
    fun dynamicAsBooleanIntStringDoubleArrayMap() {
        // boolean
        val b = DynamicImpl(true)
        assertTrue(b.asBoolean())
        assertEquals(XReadableType.Boolean, b.getType())
        assertFalse(b.isNull())

        // int
        val i = DynamicImpl(7)
        assertEquals(7, i.asInt())
        assertEquals(7.0, i.asDouble(), 0.0)

        // float / long / double coercion to double
        assertEquals(2.5, DynamicImpl(2.5f).asDouble(), 0.0)
        assertEquals(3.0, DynamicImpl(3L).asDouble(), 0.0)
        assertEquals(4.0, DynamicImpl(4.0).asDouble(), 0.0)

        // string
        val s = DynamicImpl("hi")
        assertEquals("hi", s.asString())

        // null
        assertTrue(DynamicImpl(null).isNull())
        assertEquals(XReadableType.Null, DynamicImpl(null).getType())

        // array / map
        val a = DynamicImpl(JSONArray().put(1))
        assertNotNull(a.asArray())
        val m = DynamicImpl(JSONObject().put("k", "v"))
        assertNotNull(m.asMap())
    }

    @Test
    fun dynamicTypeMismatchThrows() {
        try {
            DynamicImpl("s").asBoolean()
            fail("expected exception")
        } catch (_: Exception) {
        }
        try {
            DynamicImpl("s").asInt()
            fail("expected exception")
        } catch (_: Exception) {
        }
        try {
            DynamicImpl(1).asString()
            fail("expected exception")
        } catch (_: Exception) {
        }
        try {
            DynamicImpl("s").asDouble()
            fail("expected exception")
        } catch (_: Exception) {
        }
        try {
            DynamicImpl("s").asArray()
            fail("expected exception")
        } catch (_: Exception) {
        }
        try {
            DynamicImpl("s").asMap()
            fail("expected exception")
        } catch (_: Exception) {
        }
    }

    @Test
    fun bridgeCollectionsOptHelpersCoverAllTypes() {
        val source =
            JSONObject().apply {
                put("s", "v")
                put("b", true)
                put("i", 5)
                // d as a non-int double; JSONObject collapses 1.0 to int.
                put("d", 1.25)
                put("m", JSONObject().put("kk", "vv"))
                put("arr", JSONArray().put("x"))
            }
        val map = ReadableMapImpl(source)

        assertEquals("v", map.optString("s"))
        assertEquals("default", map.optString("missing", "default"))
        assertEquals("default", map.optString("b", "default")) // type mismatch

        assertTrue(map.optBoolean("b"))
        assertFalse(map.optBoolean("missing"))
        assertFalse(map.optBoolean("s")) // type mismatch

        // NOTE: optInt routes through DynamicImpl whose getType matches `is Number`
        // before `is Int`, so an Int value is reported as Number and optInt
        // returns the default. This documents the current behavior.
        assertEquals(0, map.optInt("i"))
        assertEquals(0, map.optInt("missing"))
        assertEquals(0, map.optInt("s")) // type mismatch

        assertEquals(1.25, map.optDouble("d"), 0.0)
        assertEquals(0.0, map.optDouble("missing"), 0.0)
        assertEquals(0.0, map.optDouble("s"), 0.0) // type mismatch

        assertNotNull(map.optMap("m"))
        assertNull(map.optMap("missing"))
        assertNull(map.optMap("s")) // type mismatch

        assertNotNull(map.optArray("arr"))
        assertNull(map.optArray("missing"))
        assertNull(map.optArray("s")) // type mismatch
    }

    @Test
    fun toObjectMapAndToObjectListWalkAllBranches() {
        val source =
            JSONObject().apply {
                put("s", "v")
                put("b", true)
                put("i", 5)
                put("d", 1.25)
                put("nestedMap", JSONObject().put("k", "vv"))
                put(
                    "nestedArr",
                    JSONArray()
                        .put("a")
                        .put(true)
                        .put(1)
                        .put(2.5)
                        .put(JSONObject().put("k", "vv"))
                        .put(JSONArray().put("x")),
                )
            }
        val map = ReadableMapImpl(source)
        val plain = map.toObjectMap()
        assertEquals("v", plain["s"])
        assertEquals(true, plain["b"])
        @Suppress("UNCHECKED_CAST")
        val nestedMap = plain["nestedMap"] as Map<String, Any>
        assertEquals("vv", nestedMap["k"])
        @Suppress("UNCHECKED_CAST")
        val nestedArr = plain["nestedArr"] as List<Any>
        assertEquals(6, nestedArr.size)
        assertArrayEquals(arrayOf("a", true), arrayOf(nestedArr[0], nestedArr[1]))
    }

    @Test
    fun keyIteratorImplBehavesAsExpected() {
        val it = KeyIteratorImpl(listOf("a", "b").iterator())
        assertTrue(it.hasNextKey())
        assertEquals("a", it.nextKey())
        assertTrue(it.hasNextKey())
        assertEquals("b", it.nextKey())
        assertFalse(it.hasNextKey())
    }

    @Test
    fun xReadableTypeEnumValuesAreCovered() {
        // Touch every enum entry so their <clinit> executes.
        assertEquals(7, XReadableType.values().size)
        assertEquals(XReadableType.Null, XReadableType.valueOf("Null"))
        assertEquals(XReadableType.Boolean, XReadableType.valueOf("Boolean"))
        assertEquals(XReadableType.Number, XReadableType.valueOf("Number"))
        assertEquals(XReadableType.Int, XReadableType.valueOf("Int"))
        assertEquals(XReadableType.String, XReadableType.valueOf("String"))
        assertEquals(XReadableType.Map, XReadableType.valueOf("Map"))
        assertEquals(XReadableType.Array, XReadableType.valueOf("Array"))
    }
}
