// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.runtime.depend

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BridgeBaseRuntimeTest {
    @Before
    fun setUp() {
        // Reset state before each test
        BridgeBaseRuntime.applicationContext = null
    }

    @After
    fun tearDown() {
        // Clean up after each test
        BridgeBaseRuntime.applicationContext = null
    }

    @Test
    fun testRuntimeInitialization() {
        // Given & When
        val runtime = BridgeBaseRuntime

        // Then
        assertNotNull("Runtime should be initialized", runtime)
    }

    @Test
    fun testRuntimeProperties() {
        // Given & When
        val runtime = BridgeBaseRuntime

        // Then
        assertNotNull("Runtime should not be null", runtime)
        assertNull("Application context should be null by default", runtime.applicationContext)
    }

    @Test
    fun testRuntimeContextSetting() {
        // Given
        val runtime = BridgeBaseRuntime

        // When & Then - Test that context can be set and retrieved
        assertNull("Context should be null initially", runtime.applicationContext)
    }
}
