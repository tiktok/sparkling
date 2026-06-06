package com.tiktok.sparkling.hybridkit.lynx

import android.os.Bundle
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LynxInitDataTest {
    @Test
    fun tryTransformUnsupportedDataHandlesBundleJsonAndArray() {
        val bundle =
            Bundle().apply {
                putString("name", "sparkling")
                putInt("count", 2)
            }
        val jsonObject =
            JSONObject().apply {
                put("a", 1)
                put("b", JSONObject().put("c", true))
            }
        val jsonArray =
            JSONArray().apply {
                put("x")
                put(JSONObject().put("y", 2))
                put(JSONObject.NULL)
            }

        val fromBundle = LynxInitData.tryTransformUnsupportedData(bundle) as Map<*, *>
        val fromJson = LynxInitData.tryTransformUnsupportedData(jsonObject) as Map<*, *>
        val fromArray = LynxInitData.tryTransformUnsupportedData(jsonArray) as List<*>

        assertEquals("sparkling", fromBundle["name"])
        assertEquals(2, fromBundle["count"])
        assertEquals(1, fromJson["a"])
        assertTrue(fromJson["b"] is Map<*, *>)
        assertEquals("x", fromArray[0])
        assertNull(fromArray[2])
    }

    @Test
    fun tryTransformUnsupportedDataIgnoresNonStringMapKeysAndSupportsNested() {
        val input =
            mapOf(
                "ok" to listOf(1, mapOf("nested" to "v")),
                7 to "ignored",
            )

        val out = LynxInitData.tryTransformUnsupportedData(input) as Map<*, *>

        assertTrue(out.containsKey("ok"))
        assertEquals(1, (out["ok"] as List<*>)[0])
        assertFalse(out.containsKey(7))
    }

    @Test
    fun fromMapAndPutProduceTemplateDataWithoutCrash() {
        val initData = LynxInitData.fromMap(mapOf("a" to 1, "b" to true))
        initData.put("c", JSONObject().put("x", "y"))

        val template = initData.getTemplateData()
        assertNotNull(template)
        assertTrue(template.toString().isNotEmpty())
    }
}
