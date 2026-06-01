// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.registry.api

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BridgeSettingsTest {
    @After
    fun tearDown() {
        BridgeSettings.bridgeRegistryOptimize = false
        BridgeSettings.bridgeDisableLongToDouble = false
        BridgeSettings.bridgeNewInputNumberTypeChange = false
        BridgeSettings.bridgeCallToStringOptimization = false
    }

    @Test
    fun defaultsAreFalse() {
        assertFalse(BridgeSettings.bridgeRegistryOptimize)
        assertFalse(BridgeSettings.bridgeDisableLongToDouble)
        assertFalse(BridgeSettings.bridgeNewInputNumberTypeChange)
        assertFalse(BridgeSettings.bridgeCallToStringOptimization)
    }

    @Test
    fun mutableFlagsCanBeToggled() {
        BridgeSettings.bridgeRegistryOptimize = true
        BridgeSettings.bridgeDisableLongToDouble = true
        BridgeSettings.bridgeNewInputNumberTypeChange = true
        BridgeSettings.bridgeCallToStringOptimization = true
        assertTrue(BridgeSettings.bridgeRegistryOptimize)
        assertTrue(BridgeSettings.bridgeDisableLongToDouble)
        assertTrue(BridgeSettings.bridgeNewInputNumberTypeChange)
        assertTrue(BridgeSettings.bridgeCallToStringOptimization)

        BridgeSettings.bridgeRegistryOptimize = false
        BridgeSettings.bridgeDisableLongToDouble = false
        BridgeSettings.bridgeNewInputNumberTypeChange = false
        BridgeSettings.bridgeCallToStringOptimization = false
        assertFalse(BridgeSettings.bridgeRegistryOptimize)
        assertFalse(BridgeSettings.bridgeDisableLongToDouble)
        assertFalse(BridgeSettings.bridgeNewInputNumberTypeChange)
        assertFalse(BridgeSettings.bridgeCallToStringOptimization)
    }
}
