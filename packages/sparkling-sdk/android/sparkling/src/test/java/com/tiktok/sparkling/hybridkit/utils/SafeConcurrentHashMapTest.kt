// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.utils

import com.tiktok.sparkling.hybridkit.utils.SafeConcurrentHashMap
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SafeConcurrentHashMapTest {
    private lateinit var safeConcurrentHashMap: SafeConcurrentHashMap<String, String>

    @Before
    fun setUp() {
        safeConcurrentHashMap = SafeConcurrentHashMap()
    }

    @Test
    fun testPutWithValidKeyValue() {
        val result = safeConcurrentHashMap.put("key1", "value1")

        assertNull(result)
        assertEquals("value1", safeConcurrentHashMap["key1"])
        assertEquals(1, safeConcurrentHashMap.size)
    }

    @Test
    fun testPutWithExistingKey() {
        safeConcurrentHashMap.put("key1", "value1")
        val result = safeConcurrentHashMap.put("key1", "value2")

        assertEquals("value1", result)
        assertEquals("value2", safeConcurrentHashMap["key1"])
        assertEquals(1, safeConcurrentHashMap.size)
    }

    @Test
    fun testPutIfAbsentWithValidKeyValue() {
        val result = safeConcurrentHashMap.putIfAbsent("key1", "value1")

        assertNull(result)
        assertEquals("value1", safeConcurrentHashMap["key1"])
        assertEquals(1, safeConcurrentHashMap.size)
    }

    @Test
    fun testPutIfAbsentWithExistingKey() {
        safeConcurrentHashMap.put("key1", "value1")
        val result = safeConcurrentHashMap.putIfAbsent("key1", "value2")

        assertEquals("value1", result)
        assertEquals("value1", safeConcurrentHashMap["key1"])
        assertEquals(1, safeConcurrentHashMap.size)
    }

    @Test
    fun testPutAllWithValidMap() {
        val mapToPut =
            mapOf(
                "key1" to "value1",
                "key2" to "value2",
                "key3" to "value3",
            )

        safeConcurrentHashMap.putAll(mapToPut)

        assertEquals(3, safeConcurrentHashMap.size)
        assertEquals("value1", safeConcurrentHashMap["key1"])
        assertEquals("value2", safeConcurrentHashMap["key2"])
        assertEquals("value3", safeConcurrentHashMap["key3"])
    }

    @Test
    fun testPutAllWithNullValues() {
        val mapToPut: Map<String, String> =
            mapOf(
                "key1" to "value1",
                "key3" to "value3",
            )

        safeConcurrentHashMap.putAll(mapToPut)

        assertEquals(2, safeConcurrentHashMap.size)
        assertEquals("value1", safeConcurrentHashMap["key1"])
        assertFalse(safeConcurrentHashMap.containsKey("key2"))
        assertEquals("value3", safeConcurrentHashMap["key3"])
    }

    @Test
    fun testPutAllWithEmptyMap() {
        val emptyMap = emptyMap<String, String>()

        safeConcurrentHashMap.putAll(emptyMap)

        assertEquals(0, safeConcurrentHashMap.size)
    }

    @Test
    fun testPutAllWithMixedValidAndNullEntries() {
        val mapToPut: Map<String, String> =
            mapOf(
                "key1" to "value1",
                "key3" to "value3",
            )

        safeConcurrentHashMap.putAll(mapToPut)

        assertEquals(2, safeConcurrentHashMap.size)
        assertTrue(safeConcurrentHashMap.containsKey("key1"))
        assertTrue(safeConcurrentHashMap.containsKey("key3"))
        assertFalse(safeConcurrentHashMap.containsKey("key2"))
        assertFalse(safeConcurrentHashMap.containsKey("key4"))
    }

    @Test
    fun testConcurrentOperations() {
        safeConcurrentHashMap.put("key1", "value1")
        safeConcurrentHashMap.putIfAbsent("key2", "value2")
        safeConcurrentHashMap.putAll(mapOf("key3" to "value3", "key4" to "value4"))

        assertEquals(4, safeConcurrentHashMap.size)
        assertEquals("value1", safeConcurrentHashMap["key1"])
        assertEquals("value2", safeConcurrentHashMap["key2"])
        assertEquals("value3", safeConcurrentHashMap["key3"])
        assertEquals("value4", safeConcurrentHashMap["key4"])
    }
}
