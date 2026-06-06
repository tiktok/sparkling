// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.registry.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CancelCallbackConfigTest {
    @Test
    fun defaultsAreDisabled() {
        val config = CancelCallbackConfig()
        assertFalse(config.enable)
        assertNull(config.cancelCallbackUrlList)
    }

    @Test
    fun fieldsCanBeMutated() {
        val config = CancelCallbackConfig()
        config.enable = true
        config.cancelCallbackUrlList = arrayListOf("https://a", "https://b")

        assertTrue(config.enable)
        assertEquals(2, config.cancelCallbackUrlList?.size)
        assertEquals("https://a", config.cancelCallbackUrlList?.get(0))
        assertEquals("https://b", config.cancelCallbackUrlList?.get(1))
    }
}
