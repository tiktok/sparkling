// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.protocol.impl.lifecycle.fe

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FeCallMonitorModelTest {
    private lateinit var feCallMonitorModel: FeCallMonitorModel

    @Before
    fun setUp() {
        feCallMonitorModel = FeCallMonitorModel()
    }

    @Test
    fun testAddCategory() {
        feCallMonitorModel.addCategory("test_key", "test_value")
        // No public method to get the category, so we can't directly assert.
        // This test is for coverage and to ensure no exceptions are thrown.
    }

    @Test
    fun testReportFeCallInfo() {
        // Set up the test data
        feCallMonitorModel.bridgeName = "testBridge"
        feCallMonitorModel.jsbFuncCallStart = 1000L
        feCallMonitorModel.jsbFuncCallEnd = 1100L
        feCallMonitorModel.jsbCallbackStart = 1200L
        feCallMonitorModel.jsbCallbackEnd = 1300L
        feCallMonitorModel.jsbNativeCallStart = 1150L

        // Execute the method - this should not throw an exception
        try {
            feCallMonitorModel.reportFeCallInfo()
            // If we reach here, the method executed successfully
            assertTrue("reportFeCallInfo executed successfully", true)
        } catch (e: Exception) {
            fail("reportFeCallInfo should not throw an exception: ${e.message}")
        }
    }

    @Test
    fun testReportFeCallInfo_withZeroStartTime() {
        // Set up test data with zero start time
        feCallMonitorModel.bridgeName = "testBridge"
        feCallMonitorModel.jsbFuncCallStart = 0L

        // Execute the method - this should not throw an exception
        try {
            feCallMonitorModel.reportFeCallInfo()
            // If we reach here, the method executed successfully
            assertTrue("reportFeCallInfo with zero start time executed successfully", true)
        } catch (e: Exception) {
            fail("reportFeCallInfo with zero start time should not throw an exception: ${e.message}")
        }
    }
}
