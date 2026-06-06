// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.protocol.utils

import com.lynx.react.bridge.JavaOnlyArray
import com.lynx.react.bridge.JavaOnlyMap
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [BridgeConverter] covering both directions of map / array
 * conversion between Lynx [JavaOnlyMap]/[JavaOnlyArray] and [JSONObject]/
 * [JSONArray], including the numeric coercion logic and null/empty edge cases.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BridgeConverterTest {
    @Test
    fun revertJavaOnlyMap2JSONObjectWithNullReturnsEmpty() {
        val obj = BridgeConverter.revertJavaOnlyMap2JSONObject(null)
        assertEquals(0, obj.length())
    }

    @Test
    fun revertJavaOnlyMap2JSONObjectWithEmptyReturnsEmpty() {
        val obj = BridgeConverter.revertJavaOnlyMap2JSONObject(JavaOnlyMap())
        assertEquals(0, obj.length())
    }

    @Test
    fun revertJavaOnlyMap2JSONObjectScalarTypes() {
        val map = JavaOnlyMap()
        map.putString("s", "hello")
        map.putBoolean("b", true)
        map.putInt("i", 42)
        map.putDouble("d", 1.5)
        map.putLong("l", 1234567890L)
        map.putNull("nullKey")

        val obj = BridgeConverter.revertJavaOnlyMap2JSONObject(map)
        assertEquals("hello", obj.getString("s"))
        assertTrue(obj.getBoolean("b"))
        assertEquals(42, obj.getInt("i"))
        assertEquals(1.5, obj.getDouble("d"), 0.0001)
        // Number coercion will store integral doubles as longs
        assertEquals(1234567890L, obj.getLong("l"))
        // null values are not put via putOpt
        assertFalse(obj.has("nullKey"))
    }

    @Test
    fun revertJavaOnlyMap2JSONObjectNumberCoercion() {
        val map = JavaOnlyMap()
        // value that fits in Int range
        map.putDouble("intLike", 5.0)
        // value that overflows Int but fits in Long
        map.putDouble("longLike", 5_000_000_000.0)
        // truly fractional value
        map.putDouble("fractional", 1.25)

        val obj = BridgeConverter.revertJavaOnlyMap2JSONObject(map)
        assertEquals(5, obj.getInt("intLike"))
        assertEquals(5_000_000_000L, obj.getLong("longLike"))
        assertEquals(1.25, obj.getDouble("fractional"), 0.0001)
    }

    @Test
    fun revertJavaOnlyMap2JSONObjectNestedArrayAndMap() {
        val nested = JavaOnlyMap()
        nested.putString("k", "v")

        val arr = JavaOnlyArray()
        arr.pushString("first")
        arr.pushInt(2)
        arr.pushBoolean(false)

        val outer = JavaOnlyMap()
        outer.putMap("nested", nested)
        outer.putArray("arr", arr)

        val obj = BridgeConverter.revertJavaOnlyMap2JSONObject(outer)
        assertEquals("v", obj.getJSONObject("nested").getString("k"))
        val jsonArr = obj.getJSONArray("arr")
        assertEquals(3, jsonArr.length())
        assertEquals("first", jsonArr.getString(0))
        assertEquals(2, jsonArr.getInt(1))
        assertFalse(jsonArr.getBoolean(2))
    }

    @Test
    fun revertJavaOnlyMap2JSONObjectArrayWithNestedTypes() {
        val nestedArr = JavaOnlyArray()
        nestedArr.pushDouble(3.0)

        val nestedMap = JavaOnlyMap()
        nestedMap.putString("k", "v")

        val arr = JavaOnlyArray()
        arr.pushArray(nestedArr)
        arr.pushMap(nestedMap)

        val outer = JavaOnlyMap()
        outer.putArray("arr", arr)

        val obj = BridgeConverter.revertJavaOnlyMap2JSONObject(outer)
        val jsonArr = obj.getJSONArray("arr")
        assertEquals(3, jsonArr.getJSONArray(0).getInt(0))
        assertEquals("v", jsonArr.getJSONObject(1).getString("k"))
    }

    @Test
    fun convertJSONObject2JavaOnlyMapScalarTypes() {
        val obj =
            JSONObject().apply {
                put("s", "hello")
                put("b", true)
                put("i", 42)
                put("d", 1.5)
                put("nullKey", JSONObject.NULL)
            }
        val map = BridgeConverter.convertJSONObject2JavaOnlyMap(obj)
        assertEquals("hello", map.getString("s"))
        assertTrue(map.getBoolean("b"))
        assertEquals(42, map.getInt("i"))
        assertEquals(1.5, map.getDouble("d"), 0.0001)
        assertNull(map["nullKey"])
    }

    @Test
    fun convertJSONObject2JavaOnlyMapNestedArrayAndMap() {
        val obj =
            JSONObject().apply {
                put("nested", JSONObject().put("k", "v"))
                put(
                    "arr",
                    JSONArray()
                        .put("a")
                        .put(1)
                        .put(JSONObject().put("kk", "vv"))
                        .put(JSONArray().put(true)),
                )
            }
        val map = BridgeConverter.convertJSONObject2JavaOnlyMap(obj)
        val nested = map.getMap("nested") as JavaOnlyMap
        assertEquals("v", nested.getString("k"))

        val arr = map.getArray("arr") as JavaOnlyArray
        assertEquals(4, arr.size)
        assertEquals("a", arr.getString(0))
        assertEquals(1, arr.getInt(1))
        assertEquals("vv", (arr.getMap(2) as JavaOnlyMap).getString("kk"))
        assertTrue((arr.getArray(3) as JavaOnlyArray).getBoolean(0))
    }

    @Test
    fun roundTripJSONObjectThroughJavaOnlyMap() {
        val source =
            JSONObject().apply {
                put("name", "alice")
                put("age", 30)
                put("nested", JSONObject().put("x", "y"))
            }
        val map = BridgeConverter.convertJSONObject2JavaOnlyMap(source)
        val back = BridgeConverter.revertJavaOnlyMap2JSONObject(map)
        assertEquals("alice", back.getString("name"))
        assertEquals(30, back.getInt("age"))
        assertEquals("y", back.getJSONObject("nested").getString("x"))
    }
}
