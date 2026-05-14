// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.protocol.impl.errors

import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class JSBErrorReportModelTest {
    private lateinit var errorReportModel: JSBErrorReportModel

    @Before
    fun setUp() {
        errorReportModel = JSBErrorReportModel()
    }

    @Test
    fun testPutGlobalExtension() {
        JSBErrorReportModel.putGlobalExtension("globalKey", "globalValue")
        // Cannot assert directly, but we can ensure no exception is thrown
    }

    @Test
    fun testSetters() {
        errorReportModel.setJsbMethodName("testMethod")
        errorReportModel.setJsbUrl("http://test.com")
        errorReportModel.setJsbErrorCode(IDLBridgeMethod.UNKNOWN_ERROR)
        errorReportModel.setJsbBridgeSdk("testSdk")
        // Cannot assert directly, but we can ensure no exception is thrown
    }

    @Test
    fun testPutJsbExtension() {
        errorReportModel.putJsbExtension("localKey", "localValue")
        // Cannot assert directly, but we can ensure no exception is thrown
    }

    @Test
    fun testReportJSBErrorModelWithValidCode() {
        errorReportModel.setJsbMethodName("testMethod")
        errorReportModel.setJsbErrorCode(IDLBridgeMethod.UNREGISTERED)
        errorReportModel.reportJSBErrorModel(null)
        // Cannot assert directly, but we can ensure no exception is thrown
    }

    @Test
    fun testReportJSBErrorModelWithInvalidCode() {
        errorReportModel.setJsbMethodName("testMethod")
        errorReportModel.setJsbErrorCode(-999)
        errorReportModel.reportJSBErrorModel(null)
        // Cannot assert directly, but we can ensure no exception is thrown
    }
}
