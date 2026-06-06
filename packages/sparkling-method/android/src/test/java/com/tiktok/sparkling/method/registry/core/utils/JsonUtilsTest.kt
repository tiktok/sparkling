// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.registry.core.utils

import com.tiktok.sparkling.method.registry.core.model.idl.DynamicType
import com.tiktok.sparkling.method.registry.core.model.idl.IDLDynamic
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [JsonUtils]. Covers all type branches in `mapToJSON`,
 * `listToJSON`, `jsonToMap`, `jsonToList`, `toJSONArray`, and `toJSONObject`,
 * including IDLDynamic + IDLMethodBaseModel adapters.
 */
@RunWith(RobolectricTestRunner::class)
class JsonUtilsTest {
    @Test
    fun toJsonAndFromJsonRoundTripPlainData() {
        val data = mapOf("a" to 1, "b" to "two")
        val text = JsonUtils.toJson(data)

        @Suppress("UNCHECKED_CAST")
        val back = JsonUtils.fromJson(text, Map::class.java) as Map<String, Any>
        assertEquals("two", back["b"])
        // gson decodes 1 as Double here, allow either numeric type
        assertEquals(1.0, (back["a"] as Number).toDouble(), 0.0)
    }

    @Test
    fun mapToJsonHandlesAllPrimitiveAndStructuralTypes() {
        val source =
            mapOf<String, Any>(
                "intVal" to 1,
                "longVal" to 2L,
                "floatVal" to 3.5f,
                "doubleVal" to 4.25,
                "stringVal" to "hello",
                "boolVal" to true,
                "mapVal" to mapOf("nested" to "value"),
                "listVal" to listOf(1, "two"),
                "dynVal" to fixedDynamic("dyn"),
            )

        val json = JsonUtils.mapToJSON(source)

        assertEquals(1, json.getInt("intVal"))
        assertEquals(2.0, json.getDouble("longVal"), 0.0)
        assertEquals(3.5, json.getDouble("floatVal"), 1e-6)
        assertEquals(4.25, json.getDouble("doubleVal"), 0.0)
        assertEquals("hello", json.getString("stringVal"))
        assertTrue(json.getBoolean("boolVal"))
        assertEquals("value", json.getJSONObject("mapVal").getString("nested"))
        val listJson = json.getJSONArray("listVal")
        assertEquals(1, listJson.getInt(0))
        assertEquals("two", listJson.getString(1))
        assertEquals("dyn", json.getString("dynVal"))
    }

    @Test
    fun mapToJsonGracefullyIgnoresStructuralCastFailures() {
        // List<Any> contract is violated by inserting a non-Any (null) element.
        // The cast must be caught silently rather than crash the conversion.
        val badList: MutableList<Any?> = mutableListOf()
        badList.add(null)
        val safe: List<Any?> = badList
        val source = mutableMapOf<String, Any>()
        @Suppress("UNCHECKED_CAST")
        source["list"] = (safe as List<Any>)

        // Should not throw — exception inside catch is swallowed.
        JsonUtils.mapToJSON(source)
    }

    @Test
    fun listToJsonHandlesAllPrimitiveAndStructuralTypes() {
        val source =
            listOf<Any>(
                1,
                2L,
                3.5f,
                4.25,
                "five",
                false,
                mapOf("k" to "v"),
                listOf<Any>("a", 1),
                fixedDynamic(true),
            )

        val arr = JsonUtils.listToJSON(source)

        assertEquals(1, arr.getInt(0))
        assertEquals(2.0, arr.getDouble(1), 0.0)
        assertEquals(3.5, arr.getDouble(2), 1e-6)
        assertEquals(4.25, arr.getDouble(3), 0.0)
        assertEquals("five", arr.getString(4))
        assertFalse(arr.getBoolean(5))
        assertEquals("v", arr.getJSONObject(6).getString("k"))
        val nested = arr.getJSONArray(7)
        assertEquals("a", nested.getString(0))
        assertEquals(1, nested.getInt(1))
        assertTrue(arr.getBoolean(8))
    }

    @Test
    fun jsonToMapAndJsonToListReturnHydratedKotlinValues() {
        val payload =
            JSONObject(
                """
                {
                  "i": 1,
                  "d": 2.5,
                  "s": "x",
                  "b": true,
                  "obj": { "nested": 7 },
                  "arr": [1, "two", { "k": "v" }, [9, 10]]
                }
                """.trimIndent(),
            )

        val map = JsonUtils.jsonToMap(payload)
        assertEquals(1, map["i"])
        assertEquals(2.5, map["d"])
        assertEquals("x", map["s"])
        assertEquals(true, map["b"])
        @Suppress("UNCHECKED_CAST")
        val nested = map["obj"] as Map<String, Any?>
        assertEquals(7, nested["nested"])
        @Suppress("UNCHECKED_CAST")
        val arr = map["arr"] as List<Any?>
        assertEquals(1, arr[0])
        assertEquals("two", arr[1])
        @Suppress("UNCHECKED_CAST")
        val nestedMap = arr[2] as Map<String, Any?>
        assertEquals("v", nestedMap["k"])
        @Suppress("UNCHECKED_CAST")
        val nestedList = arr[3] as List<Any?>
        assertEquals(9, nestedList[0])
        assertEquals(10, nestedList[1])
    }

    @Test
    fun jsonToMapDefaultsUnknownTypesToNull() {
        val payload = JSONObject().apply { put("nullable", JSONObject.NULL) }
        val map = JsonUtils.jsonToMap(payload)
        assertNull(map["nullable"])
    }

    @Test
    fun toJSONArrayHandlesPrimitivesNullsCollectionsBaseModelAndIdlDynamic() {
        val baseModel =
            object : IDLMethodBaseModel {
                override fun toJSON(): JSONObject = JSONObject().apply { put("type", "base") }

                override fun convert(): Map<String, Any>? = mapOf()
            }
        val opaque = object {} // unknown -> filtered out

        val source =
            listOf<Any?>(
                1,
                2L,
                "s",
                true,
                3.14,
                listOf<Any>(7, "x"),
                mapOf("a" to 1),
                fixedDynamic("d"),
                baseModel,
                null, // dropped via filterNotNull
                opaque, // unknown -> mapped to null and dropped
            )

        val arr = JsonUtils.toJSONArray(source)

        // null + opaque are dropped, so size = 9
        assertEquals(9, arr.length())
        assertEquals(1, arr.getInt(0))
        assertEquals(2L, arr.getLong(1))
        assertEquals("s", arr.getString(2))
        assertTrue(arr.getBoolean(3))
        assertEquals(3.14, arr.getDouble(4), 0.0)
        assertTrue(arr.get(5) is JSONArray)
        assertTrue(arr.get(6) is JSONObject)
        assertEquals("d", arr.getString(7))
        assertEquals("base", arr.getJSONObject(8).getString("type"))
    }

    @Test
    fun toJSONObjectHandlesAllValueBranchesAndDropsNonStringKeys() {
        val baseModel =
            object : IDLMethodBaseModel {
                override fun toJSON(): JSONObject = JSONObject().apply { put("kind", "base") }

                override fun convert(): Map<String, Any>? = null
            }

        val source =
            mapOf<Any?, Any?>(
                "i" to 1,
                "l" to 2L,
                "s" to "v",
                "b" to true,
                "d" to 1.5,
                "list" to listOf<Any>(1, 2),
                "map" to mapOf("k" to "v"),
                "dyn" to fixedDynamic(42L),
                "model" to baseModel,
                "unknown" to object {},
                42 to "ignored", // non-string key dropped
            )

        val json = JsonUtils.toJSONObject(source)

        assertEquals(1, json.getInt("i"))
        assertEquals(2L, json.getLong("l"))
        assertEquals("v", json.getString("s"))
        assertTrue(json.getBoolean("b"))
        assertEquals(1.5, json.getDouble("d"), 0.0)
        assertTrue(json.getJSONArray("list").length() == 2)
        assertEquals("v", json.getJSONObject("map").getString("k"))
        assertEquals(42L, json.getLong("dyn"))
        assertEquals("base", json.getJSONObject("model").getString("kind"))
        assertFalse(json.has("42"))
        // `unknown` resolves to null and JSONObject treats null .put as remove,
        // so the key is absent.
        assertFalse(json.has("unknown"))
    }

    private fun fixedDynamic(value: Any?): IDLDynamic =
        object : IDLDynamic {
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

            override fun getType(): DynamicType =
                when (value) {
                    null -> DynamicType.Null
                    is Boolean -> DynamicType.Boolean
                    is Int -> DynamicType.Int
                    is Long -> DynamicType.Long
                    is Double, is Float -> DynamicType.Number
                    is String -> DynamicType.String
                    is Map<*, *> -> DynamicType.Map
                    is List<*> -> DynamicType.Array
                    is ByteArray -> DynamicType.ByteArray
                    else -> DynamicType.Null
                }

            override fun recycle() {}
        }
}
