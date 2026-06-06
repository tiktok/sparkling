// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.registry.api

import com.lynx.react.bridge.PiperData
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the internal extension functions in PiperDataExt.kt:
 * - [PiperData.toJSONObject]
 * - [PiperData.stringify]
 * - [Map.containsPiperData]
 *
 * `PiperData.fromString` requires the Lynx native library to be loaded, which
 * is unavailable in the JVM unit-test environment. We therefore construct
 * PiperData via the no-arg constructor and stamp `mType` / `mRawData` through
 * reflection — this is the same shape `fromObject` ends up producing on a real
 * device, and it lets us exercise every branch of the extension functions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PiperDataExtTest {
    private fun newPiper(
        type: PiperData.DataType,
        rawData: Any?,
    ): PiperData {
        val ctor = PiperData::class.java.getDeclaredConstructor()
        ctor.isAccessible = true
        val piper = ctor.newInstance()
        PiperData::class.java.getDeclaredField("mType").apply {
            isAccessible = true
            set(piper, type)
        }
        PiperData::class.java.getDeclaredField("mRawData").apply {
            isAccessible = true
            set(piper, rawData)
        }
        return piper
    }

    private fun stringPiper(raw: String): PiperData = newPiper(PiperData.DataType.String, raw)

    private fun mapPiper(raw: Map<*, *>): PiperData = newPiper(PiperData.DataType.Map, raw)

    private fun emptyPiper(): PiperData = newPiper(PiperData.DataType.Empty, null)

    @Test
    fun toJSONObjectFromStringPayload() {
        val piper = stringPiper("""{"a":1,"b":"x"}""")
        val obj = piper.toJSONObject()
        assertEquals(1, obj.getInt("a"))
        assertEquals("x", obj.getString("b"))
    }

    @Test
    fun toJSONObjectFromStringWithInvalidJsonReturnsEmpty() {
        val piper = stringPiper("not-json")
        val obj = piper.toJSONObject()
        // JSON parse failure -> caught -> empty JSONObject
        assertEquals(0, obj.length())
    }

    @Test
    fun toJSONObjectFromStringWithNonStringRawDataReturnsEmpty() {
        // Type is String but rawData is not a String — should fall through to
        // the non-String branch and yield an empty JSONObject.
        val piper = newPiper(PiperData.DataType.String, 42)
        assertEquals(0, piper.toJSONObject().length())
    }

    @Test
    fun toJSONObjectFromMapPayload() {
        val piper = mapPiper(mapOf<String, Any>("a" to 1, "b" to "x"))
        val obj = piper.toJSONObject()
        assertEquals(1, obj.getInt("a"))
        assertEquals("x", obj.getString("b"))
    }

    @Test
    fun toJSONObjectFromMapTypeWithNonMapRawDataReturnsEmpty() {
        val piper = newPiper(PiperData.DataType.Map, "not-a-map")
        assertEquals(0, piper.toJSONObject().length())
    }

    @Test
    fun toJSONObjectFromEmptyTypeReturnsEmpty() {
        val piper = emptyPiper()
        assertEquals(0, piper.toJSONObject().length())
    }

    @Test
    fun stringifyFromStringPayloadReturnsRawString() {
        val piper = stringPiper("""{"a":1}""")
        assertEquals("""{"a":1}""", piper.stringify())
    }

    @Test
    fun stringifyFromStringWithNonStringRawDataReturnsEmpty() {
        val piper = newPiper(PiperData.DataType.String, 42)
        // rawData is not a String -> `as? String` -> null -> `?: ""`
        assertEquals("", piper.stringify())
    }

    @Test
    fun stringifyFromMapPayloadReturnsJSONStringified() {
        val piper = mapPiper(mapOf<String, Any>("a" to 1))
        val s = piper.stringify()
        assertTrue(s.contains("\"a\":1"))
    }

    @Test
    fun stringifyFromMapTypeWithNonMapRawDataReturnsEmpty() {
        val piper = newPiper(PiperData.DataType.Map, 12345)
        assertEquals("", piper.stringify())
    }

    @Test
    fun stringifyFromEmptyTypeReturnsEmpty() {
        val piper = emptyPiper()
        assertEquals("", piper.stringify())
    }

    @Test
    fun containsPiperDataDetectsTopLevel() {
        val map = mapOf("k" to emptyPiper(), "x" to 1)
        assertTrue(map.containsPiperData())
    }

    @Test
    fun containsPiperDataWalksNestedMaps() {
        val map =
            mapOf<String, Any?>(
                "outer" to
                    mapOf<String, Any?>(
                        "inner" to emptyPiper(),
                    ),
            )
        assertTrue(map.containsPiperData())
    }

    @Test
    fun containsPiperDataReturnsFalseWhenAbsent() {
        val map = mapOf<String, Any?>("a" to 1, "b" to mapOf("c" to "d"))
        assertFalse(map.containsPiperData())
    }
}
