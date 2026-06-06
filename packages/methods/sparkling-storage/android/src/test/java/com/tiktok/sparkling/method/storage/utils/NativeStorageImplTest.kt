package com.tiktok.sparkling.method.storage.utils

import android.app.Application
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class NativeStorageImplTest {
    private lateinit var app: Application
    private lateinit var storage: NativeStorageImpl

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        app
            .getSharedPreferences(PREFERENCE_NAME, Application.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        app
            .getSharedPreferences("biz-a-sparkling-storage", Application.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        app
            .getSharedPreferences("biz-b-sparkling-storage", Application.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        storage = NativeStorageImpl.getInstance(app)
    }

    @After
    fun tearDown() {
        app
            .getSharedPreferences(PREFERENCE_NAME, Application.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        app
            .getSharedPreferences("biz-a-sparkling-storage", Application.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        app
            .getSharedPreferences("biz-b-sparkling-storage", Application.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun setAndGetPrimitiveValues() {
        assertTrue(storage.setStorageItem("bool", true))
        assertTrue(storage.setStorageItem("int", 42))
        assertTrue(storage.setStorageItem("double", 3.14))
        assertTrue(storage.setStorageItem("string", "hello"))

        assertEquals(true, storage.getStorageItem("bool"))
        assertEquals(42, storage.getStorageItem("int"))
        assertEquals(3.14, storage.getStorageItem("double"))
        assertEquals("hello", storage.getStorageItem("string"))
    }

    @Test
    fun setAndGetCollectionValues() {
        val dataList = listOf("a", "b", 1)
        val dataMap = mapOf("name" to "sparkling", "count" to 2)

        assertTrue(storage.setStorageItem("list", dataList))
        assertTrue(storage.setStorageItem("map", dataMap))

        val listValue = storage.getStorageItem("list") as? List<*>
        val mapValue = storage.getStorageItem("map") as? Map<*, *>

        assertEquals(3, listValue?.size)
        assertEquals("a", listValue?.get(0))
        assertEquals("sparkling", mapValue?.get("name"))
    }

    @Test
    fun bizStorageIsIsolated() {
        assertTrue(storage.setBizStorageItem("biz-a", "token", "a-token"))
        assertTrue(storage.setBizStorageItem("biz-b", "token", "b-token"))

        assertEquals("a-token", storage.getBizStorageItem("biz-a", "token"))
        assertEquals("b-token", storage.getBizStorageItem("biz-b", "token"))
        assertNull(storage.getStorageItem("token"))
    }

    @Test
    fun removeStorageRemovesValues() {
        assertTrue(storage.setStorageItem("to-remove", "value"))
        assertEquals("value", storage.getStorageItem("to-remove"))

        assertTrue(storage.removeStorageItem("to-remove"))
        assertNull(storage.getStorageItem("to-remove"))
    }

    @Test
    fun invalidInputsReturnFalseOrNull() {
        assertFalse(storage.setStorageItem(null, "value"))
        assertFalse(storage.setStorageItem("key", null))
        assertNull(storage.getStorageItem(null))
        assertFalse(storage.removeStorageItem(null))
    }

    @Test
    fun invalidWrappedJsonReturnsNull() {
        app
            .getSharedPreferences(PREFERENCE_NAME, Application.MODE_PRIVATE)
            .edit()
            .putString("broken", "{not-json")
            .commit()

        assertNull(storage.getStorageItem("broken"))
    }
}
