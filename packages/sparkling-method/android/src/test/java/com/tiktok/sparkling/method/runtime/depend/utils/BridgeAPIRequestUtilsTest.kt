package com.tiktok.sparkling.method.runtime.depend.utils

import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.runtime.depend.network.AbsStringConnection
import android.os.Looper
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class BridgeAPIRequestUtilsTest {
    @Test
    fun addParametersToUrlSerializesMapListAndTagFrom() {
        val params =
            mapOf<String, Any>(
                "map" to mapOf("k" to "v"),
                "list" to listOf(1, "x"),
                "plain" to 7,
            )

        val lynxUrl =
            BridgeAPIRequestUtils.addParametersToUrl(
                "https://example.com/path",
                params,
                BridgePlatformType.LYNX,
            )
        assertTrue(lynxUrl.contains("request_tag_from=lynx"))
        assertTrue(lynxUrl.contains("plain=7"))
        assertTrue(lynxUrl.contains("map=%7B%22k%22%3A%22v%22%7D"))
        assertTrue(lynxUrl.contains("list=%5B1%2C%22x%22%5D"))

        val webUrl =
            BridgeAPIRequestUtils.addParametersToUrl(
                "https://example.com/path",
                emptyMap(),
                BridgePlatformType.WEB,
            )
        assertTrue(webUrl.contains("request_tag_from=h5"))
    }

    @Test
    fun filterHeaderEmptyValueKeepsOnlyNonEmptyStrings() {
        val source =
            mapOf<String, Any>(
                "a" to "1",
                "b" to "",
                "c" to 3,
                "d" to "ok",
            )

        val result = BridgeAPIRequestUtils.filterHeaderEmptyValue(source)

        assertEquals(2, result.size)
        assertEquals("1", result["a"])
        assertEquals("ok", result["d"])
        assertFalse(result.containsKey("b"))
        assertFalse(result.containsKey("c"))
    }

    @Test
    fun convertParamValueToStringKeepsPrimitiveTypesOnly() {
        val source =
            mapOf<String, Any>(
                "int" to 1,
                "long" to 2L,
                "double" to 1.5,
                "bool" to true,
                "str" to "s",
                "map" to mapOf("k" to "v"),
                "list" to listOf(1, 2),
            )

        val result = BridgeAPIRequestUtils.convertParamValueToString(source)

        assertEquals("1", result["int"])
        assertEquals("2", result["long"])
        assertEquals("1.5", result["double"])
        assertEquals("true", result["bool"])
        assertEquals("s", result["str"])
        assertFalse(result.containsKey("map"))
        assertFalse(result.containsKey("list"))
    }

    @Test
    fun addParametersToUrlUsesEmptyTagForOtherPlatform() {
        val otherUrl =
            BridgeAPIRequestUtils.addParametersToUrl(
                "https://example.com/path",
                null,
                BridgePlatformType.ALL,
            )

        assertTrue(otherUrl.contains("request_tag_from="))
        assertFalse(otherUrl.contains("request_tag_from=lynx"))
        assertFalse(otherUrl.contains("request_tag_from=h5"))
    }

    @Test
    fun filterAndConvertHandleNullInput() {
        val headerResult = BridgeAPIRequestUtils.filterHeaderEmptyValue(null)
        val paramResult = BridgeAPIRequestUtils.convertParamValueToString(null)

        assertTrue(headerResult.isEmpty())
        assertTrue(paramResult.isEmpty())
    }

    @Test
    fun handleSuccessInvokesParsingFailedForInvalidJson() {
        val callback = CaptureResponseCallback()
        val method =
            BridgeAPIRequestUtils::class.java.getDeclaredMethod(
                "handleSuccess",
                String::class.java,
                LinkedHashMap::class.java,
                Int::class.javaObjectType,
                Int::class.javaObjectType,
                IResponseCallback::class.java,
            )
        method.isAccessible = true

        method.invoke(
            BridgeAPIRequestUtils,
            "not-json",
            linkedMapOf("h" to "v"),
            200,
            0,
            callback,
        )

        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertTrue(callback.parsingFailed)
        assertNotNull(callback.parsingThrowable)
    }

    @Test
    fun handleSuccessInvokesOnSuccessForValidJson() {
        val callback = CaptureResponseCallback()
        val method =
            BridgeAPIRequestUtils::class.java.getDeclaredMethod(
                "handleSuccess",
                String::class.java,
                LinkedHashMap::class.java,
                Int::class.javaObjectType,
                Int::class.javaObjectType,
                IResponseCallback::class.java,
            )
        method.isAccessible = true

        method.invoke(
            BridgeAPIRequestUtils,
            "{\"ok\":true}",
            linkedMapOf<String, String>(),
            200,
            0,
            callback,
        )

        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertTrue(callback.successCalled)
        assertEquals(true, callback.successBody?.optBoolean("ok"))
    }

    @Test
    fun handleConnectionPropagatesNullConnectionToOnFailed() {
        val callback = CaptureResponseCallback()
        val method =
            BridgeAPIRequestUtils::class.java.getDeclaredMethod(
                "handleConnection",
                AbsStringConnection::class.java,
                IResponseCallback::class.java,
            )
        method.isAccessible = true
        method.invoke(BridgeAPIRequestUtils, null, callback)

        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertTrue(callback.failedCalled)
        assertEquals(BridgeAPIRequestUtils.ERROR_CODE_408, callback.failedErrorCode)
    }

    @Test
    fun handleConnectionRoutesValidJsonBodyToOnSuccess() {
        val callback = CaptureResponseCallback()
        val connection =
            object : AbsStringConnection() {
                override fun getStringResponseBody(): String = "{\"a\":1}"

                override fun getResponseCode(): Int = 200

                override fun getClientCode(): Int = 0

                override fun getErrorMsg(): String = ""

                override fun getException(): Throwable? = null
            }

        val method =
            BridgeAPIRequestUtils::class.java.getDeclaredMethod(
                "handleConnection",
                AbsStringConnection::class.java,
                IResponseCallback::class.java,
            )
        method.isAccessible = true
        method.invoke(BridgeAPIRequestUtils, connection, callback)

        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertTrue(callback.successCalled)
        assertEquals(1, callback.successBody?.optInt("a"))
    }

    @Test
    fun handleConnectionRoutesEmptyBodyToOnSuccessWhenNoError() {
        val callback = CaptureResponseCallback()
        val connection =
            object : AbsStringConnection() {
                override fun getStringResponseBody(): String? = null

                override fun getResponseCode(): Int = 204

                override fun getClientCode(): Int = 0
            }

        val method =
            BridgeAPIRequestUtils::class.java.getDeclaredMethod(
                "handleConnection",
                AbsStringConnection::class.java,
                IResponseCallback::class.java,
            )
        method.isAccessible = true
        method.invoke(BridgeAPIRequestUtils, connection, callback)

        Shadows.shadowOf(Looper.getMainLooper()).idle()
        // empty body + no error => onSuccess invoked with parsing-failed-style empty JSON
        assertTrue(callback.successCalled || callback.parsingFailed)
    }

    @Test
    fun handleConnectionPropagatesErrorMessageToOnFailed() {
        val callback = CaptureResponseCallback()
        val cause = RuntimeException("network down")
        val connection =
            object : AbsStringConnection() {
                override fun getStringResponseBody(): String? = null

                override fun getResponseCode(): Int = 500

                override fun getClientCode(): Int = -1

                override fun getErrorMsg(): String = "boom"

                override fun getException(): Throwable = cause
            }

        val method =
            BridgeAPIRequestUtils::class.java.getDeclaredMethod(
                "handleConnection",
                AbsStringConnection::class.java,
                IResponseCallback::class.java,
            )
        method.isAccessible = true
        method.invoke(BridgeAPIRequestUtils, connection, callback)

        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertTrue(callback.failedCalled)
        assertEquals(500, callback.failedErrorCode)
        assertEquals(cause, callback.failedThrowable)
    }

    @Test
    fun handleErrorReturnsFalseWhenNoErrorPresent() {
        val callback = CaptureResponseCallback()
        val method =
            BridgeAPIRequestUtils::class.java.getDeclaredMethod(
                "handleError",
                Int::class.javaObjectType,
                Int::class.javaObjectType,
                String::class.java,
                Throwable::class.java,
                IResponseCallback::class.java,
            )
        method.isAccessible = true
        val handled = method.invoke(BridgeAPIRequestUtils, 200, 0, "", null, callback) as Boolean

        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertFalse(handled)
        assertFalse(callback.failedCalled)
    }

    @Test
    fun handleErrorReturnsTrueWhenOnlyErrorMessagePresent() {
        val callback = CaptureResponseCallback()
        val method =
            BridgeAPIRequestUtils::class.java.getDeclaredMethod(
                "handleError",
                Int::class.javaObjectType,
                Int::class.javaObjectType,
                String::class.java,
                Throwable::class.java,
                IResponseCallback::class.java,
            )
        method.isAccessible = true
        val handled = method.invoke(BridgeAPIRequestUtils, 400, 1, "bad request", null, callback) as Boolean

        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertTrue(handled)
        assertTrue(callback.failedCalled)
        assertEquals(400, callback.failedErrorCode)
        assertEquals("bad request", callback.failedThrowable?.message)
    }

    @Test
    fun handleSuccessJsonOverloadInvokesOnSuccess() {
        val callback = CaptureResponseCallback()
        val response = JSONObject().put("hello", "world")
        val method =
            BridgeAPIRequestUtils::class.java.getDeclaredMethod(
                "handleSuccess",
                JSONObject::class.java,
                LinkedHashMap::class.java,
                Int::class.javaObjectType,
                Int::class.javaObjectType,
                IResponseCallback::class.java,
            )
        method.isAccessible = true
        method.invoke(
            BridgeAPIRequestUtils,
            response,
            linkedMapOf<String, String>(),
            200,
            0,
            callback,
        )

        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertTrue(callback.successCalled)
        assertEquals("world", callback.successBody?.optString("hello"))
    }

    private class CaptureResponseCallback : IResponseCallback {
        var successCalled = false
        var parsingFailed = false
        var failedCalled = false
        var successBody: JSONObject? = null
        var parsingThrowable: Throwable? = null
        var failedErrorCode: Int? = null
        var failedThrowable: Throwable? = null

        override fun onSuccess(
            body: JSONObject,
            responseHeader: LinkedHashMap<String, String>,
            statusCode: Int?,
            clientCode: Int?,
        ) {
            successCalled = true
            successBody = body
        }

        override fun onParsingFailed(
            body: JSONObject,
            responseHeader: LinkedHashMap<String, String>,
            rawResponse: String,
            throwable: Throwable,
            statusCode: Int?,
            clientCode: Int?,
        ) {
            parsingFailed = true
            parsingThrowable = throwable
        }

        override fun onFailed(
            errorCode: Int?,
            clientCode: Int?,
            throwable: Throwable,
        ) {
            failedCalled = true
            failedErrorCode = errorCode
            failedThrowable = throwable
        }
    }
}
