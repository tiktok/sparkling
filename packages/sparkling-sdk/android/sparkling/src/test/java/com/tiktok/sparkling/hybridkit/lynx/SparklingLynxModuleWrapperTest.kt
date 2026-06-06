// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.lynx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SparklingLynxModuleWrapperTest {
    @Test
    fun defaultConstructorLeavesValuesNull() {
        val wrapper = SparklingLynxModuleWrapper()
        assertNull(wrapper.clz)
        assertNull(wrapper.moduleParams)
    }

    @Test
    fun mutableModuleParamsCanBeReassigned() {
        val wrapper = SparklingLynxModuleWrapper()
        val params = mapOf("k" to 1)
        wrapper.moduleParams = params
        assertEquals(params, wrapper.moduleParams)
        wrapper.moduleParams = null
        assertNull(wrapper.moduleParams)
    }
}
