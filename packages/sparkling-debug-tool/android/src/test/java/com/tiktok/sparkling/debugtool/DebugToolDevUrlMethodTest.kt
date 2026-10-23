// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool

import android.app.Application
import android.content.pm.ApplicationInfo
import com.tiktok.sparkling.debugtool.getDevUrl.AbsGetDevUrlMethodIDL
import com.tiktok.sparkling.debugtool.getDevUrl.GetDevUrlMethod
import com.tiktok.sparkling.debugtool.setDevUrl.AbsSetDevUrlMethodIDL
import com.tiktok.sparkling.debugtool.setDevUrl.SetDevUrlMethod
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.SparklingBridgeManager
import com.tiktok.sparkling.method.registry.core.model.idl.CompletionBlock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.lang.reflect.Proxy

@RunWith(RobolectricTestRunner::class)
class DebugToolDevUrlMethodTest {
    private lateinit var application: Application

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        application
            .getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        application
            .getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun getDevUrlReturnsPersistedUrlInDataResult() {
        SparklingDebugTool.setDevUrl(application, "  http://127.0.0.1:5969/  ")
        val callback = GetCallbackRecorder()

        GetDevUrlMethod(application).handle(emptyGetParams(), callback, BridgePlatformType.LYNX)

        assertEquals("http://127.0.0.1:5969/", callback.successResult?.url)
        assertNull(callback.failureCode)
    }

    @Test
    fun getDevUrlUsesNestedDataResponseContract() {
        SparklingDebugTool.setDevUrl(application, "https://example.com:5969/")
        var response: Map<String, Any?>? = null

        GetDevUrlMethod(application).realHandle(
            emptyMap(),
            object : IDLBridgeMethod.Callback {
                override fun invoke(data: Map<String, Any?>) {
                    response = data
                }
            },
            BridgePlatformType.LYNX,
        )

        assertEquals(IDLBridgeMethod.SUCCESS, response?.get(IDLBridgeMethod.PARAM_CODE))
        assertEquals(
            "https://example.com:5969/",
            (response?.get(IDLBridgeMethod.PARAM_DATA) as? Map<*, *>)?.get("url"),
        )
    }

    @Test
    fun getDevUrlReturnsEmptyStringWhenNoUrlIsPersisted() {
        val callback = GetCallbackRecorder()

        GetDevUrlMethod(application).handle(emptyGetParams(), callback, BridgePlatformType.LYNX)

        assertEquals("", callback.successResult?.url)
        assertNull(callback.failureCode)
    }

    @Test
    fun setDevUrlPersistsTrimmedHttpUrl() {
        val callback = SetCallbackRecorder()

        SetDevUrlMethod(application).handle(
            setParams("  https://example.com:5969/main.lynx.bundle  "),
            callback,
            BridgePlatformType.LYNX,
        )

        assertNotNull(callback.successResult)
        assertNull(callback.failureCode)
        assertEquals(
            "https://example.com:5969/main.lynx.bundle",
            SparklingDebugTool.getDevUrl(application, ""),
        )
    }

    @Test
    fun setDevUrlRejectsMissingEmptyAndMalformedUrls() {
        listOf(
            null,
            "",
            "   ",
            "ftp://example.com/main.lynx.bundle",
            "http://",
            "http:///main.lynx.bundle",
            "http://exa mple.com",
            "not a url",
        ).forEach { url ->
            val callback = SetCallbackRecorder()

            SetDevUrlMethod(application).handle(
                setParams(url),
                callback,
                BridgePlatformType.LYNX,
            )

            assertEquals(IDLBridgeMethod.INVALID_PARAM, callback.failureCode)
            assertEquals(DEV_URL_ERROR_MESSAGE, callback.failureMsg)
            assertEquals("", SparklingDebugTool.getDevUrl(application, ""))
        }
    }

    @Test
    fun setDevUrlRealHandleReturnsContractCodesAndPersistsTrimmedUrl() {
        val method = SetDevUrlMethod(application)

        listOf(
            emptyMap(),
            mapOf("url" to "not a url"),
        ).forEach { params ->
            var response: Map<String, Any?>? = null

            method.realHandle(
                params,
                object : IDLBridgeMethod.Callback {
                    override fun invoke(data: Map<String, Any?>) {
                        response = data
                    }
                },
                BridgePlatformType.LYNX,
            )

            assertEquals(IDLBridgeMethod.INVALID_PARAM, response?.get(IDLBridgeMethod.PARAM_CODE))
            assertEquals(DEV_URL_ERROR_MESSAGE, response?.get(IDLBridgeMethod.PARAM_MSG))
            assertEquals("", SparklingDebugTool.getDevUrl(application, ""))
        }

        var response: Map<String, Any?>? = null
        method.realHandle(
            mapOf("url" to "  https://example.com:5969/main.lynx.bundle  "),
            object : IDLBridgeMethod.Callback {
                override fun invoke(data: Map<String, Any?>) {
                    response = data
                }
            },
            BridgePlatformType.LYNX,
        )

        assertEquals(IDLBridgeMethod.SUCCESS, response?.get(IDLBridgeMethod.PARAM_CODE))
        assertEquals(
            "https://example.com:5969/main.lynx.bundle",
            SparklingDebugTool.getDevUrl(application, ""),
        )
    }

    @Test
    fun initRegistersBothMethodsIdempotently() {
        application.applicationInfo.flags =
            application.applicationInfo.flags or ApplicationInfo.FLAG_DEBUGGABLE

        SparklingDebugTool.init(application)
        SparklingDebugTool.init(application)

        assertEquals(
            GetDevUrlMethod::class.java,
            SparklingBridgeManager.findIDLMethodClass(
                BridgePlatformType.LYNX,
                "debugtool.getDevUrl",
            ),
        )
        assertEquals(
            SetDevUrlMethod::class.java,
            SparklingBridgeManager.findIDLMethodClass(
                BridgePlatformType.LYNX,
                "debugtool.setDevUrl",
            ),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun emptyGetParams(): AbsGetDevUrlMethodIDL.IDLMethodGetDevUrlParamModel =
        Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(AbsGetDevUrlMethodIDL.IDLMethodGetDevUrlParamModel::class.java),
        ) { _, _, _ -> null } as AbsGetDevUrlMethodIDL.IDLMethodGetDevUrlParamModel

    @Suppress("UNCHECKED_CAST")
    private fun setParams(url: String?): AbsSetDevUrlMethodIDL.IDLMethodSetDevUrlParamModel =
        Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(AbsSetDevUrlMethodIDL.IDLMethodSetDevUrlParamModel::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getUrl" -> url
                else -> null
            }
        } as AbsSetDevUrlMethodIDL.IDLMethodSetDevUrlParamModel

    private class GetCallbackRecorder : CompletionBlock<AbsGetDevUrlMethodIDL.IDLMethodGetDevUrlResultModel> {
        var successResult: AbsGetDevUrlMethodIDL.IDLMethodGetDevUrlResultModel? = null
        var failureCode: Int? = null

        override fun onSuccess(
            result: AbsGetDevUrlMethodIDL.IDLMethodGetDevUrlResultModel,
            msg: String,
        ) {
            successResult = result
        }

        override fun onFailure(
            code: Int,
            msg: String,
            data: AbsGetDevUrlMethodIDL.IDLMethodGetDevUrlResultModel?,
        ) {
            failureCode = code
        }

        override fun onRawSuccess(data: AbsGetDevUrlMethodIDL.IDLMethodGetDevUrlResultModel?) = Unit
    }

    private class SetCallbackRecorder : CompletionBlock<AbsSetDevUrlMethodIDL.IDLMethodSetDevUrlResultModel> {
        var successResult: AbsSetDevUrlMethodIDL.IDLMethodSetDevUrlResultModel? = null
        var failureCode: Int? = null
        var failureMsg: String? = null

        override fun onSuccess(
            result: AbsSetDevUrlMethodIDL.IDLMethodSetDevUrlResultModel,
            msg: String,
        ) {
            successResult = result
        }

        override fun onFailure(
            code: Int,
            msg: String,
            data: AbsSetDevUrlMethodIDL.IDLMethodSetDevUrlResultModel?,
        ) {
            failureCode = code
            failureMsg = msg
        }

        override fun onRawSuccess(data: AbsSetDevUrlMethodIDL.IDLMethodSetDevUrlResultModel?) = Unit
    }

    private companion object {
        const val PREFS_NAME = "sparkling_debug_tool"
    }
}
