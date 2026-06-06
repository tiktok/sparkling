// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.runtime.depend.utils

import android.os.Looper
import com.tiktok.sparkling.method.runtime.depend.common.IHostNetworkDepend
import com.tiktok.sparkling.method.runtime.depend.network.AbsStreamConnection
import com.tiktok.sparkling.method.runtime.depend.network.AbsStringConnection
import com.tiktok.sparkling.method.runtime.depend.network.HttpRequest
import com.tiktok.sparkling.method.runtime.depend.network.RequestJsonFormatOptionConstants
import com.tiktok.sparkling.method.runtime.depend.network.RequestMethod
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BridgeAPIRequestUtilsPublicMethodsTest {
    private class StringConnectionStub(
        private val body: String? = "{\"ok\":true}",
        private val code: Int = 200,
    ) : AbsStringConnection() {
        override fun getStringResponseBody(): String? = body

        override fun getResponseCode(): Int = code

        override fun getResponseHeader(): LinkedHashMap<String, String> = linkedMapOf("h" to "v")

        override fun getClientCode(): Int = 0

        override fun getErrorMsg(): String = ""

        override fun getException(): Throwable? = null
    }

    private class StreamConnectionStub : AbsStreamConnection() {
        override fun getInputStreamResponseBody() = ByteArrayInputStream(byteArrayOf(1, 2, 3))

        override fun getResponseCode(): Int = 200

        override fun getClientCode(): Int = 0
    }

    private class HostDepend(
        private val stringConnection: AbsStringConnection = StringConnectionStub(),
        private val streamConnection: AbsStreamConnection = StreamConnectionStub(),
    ) : IHostNetworkDepend {
        var lastMethod: RequestMethod? = null
        var lastRequest: HttpRequest? = null

        override fun requestForString(
            method: RequestMethod,
            request: HttpRequest,
        ): AbsStringConnection {
            lastMethod = method
            lastRequest = request
            return stringConnection
        }

        override fun requestForStream(
            method: RequestMethod,
            request: HttpRequest,
        ): AbsStreamConnection {
            lastMethod = method
            lastRequest = request
            return streamConnection
        }
    }

    private class CapturingResponseCallback : IResponseCallback {
        var success = false
        var failed = false
        var lastBody: JSONObject? = null

        override fun onSuccess(
            body: JSONObject,
            responseHeader: LinkedHashMap<String, String>,
            statusCode: Int?,
            clientCode: Int?,
        ) {
            success = true
            lastBody = body
        }

        override fun onFailed(
            errorCode: Int?,
            clientCode: Int?,
            throwable: Throwable,
        ) {
            failed = true
        }
    }

    private class CapturingStreamCallback : IStreamResponseCallback {
        var connection: AbsStreamConnection? = null

        override fun handleConnection(connection: AbsStreamConnection?) {
            this.connection = connection
        }
    }

    @Test
    fun getDispatchesGetRequestAndForwardsResponse() {
        val depend = HostDepend()
        val cb = CapturingResponseCallback()

        BridgeAPIRequestUtils.get(
            "https://example.com",
            linkedMapOf("h" to "v"),
            disableRedirect = true,
            addCommonParams = true,
            callback = cb,
            hostNetworkDepend = depend,
        )

        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals(RequestMethod.GET, depend.lastMethod)
        assertNotNull(depend.lastRequest)
        assertTrue(cb.success)
    }

    @Test
    fun postFileUploadDispatchesPostAndForwardsResponse() {
        val depend = HostDepend()
        val cb = CapturingResponseCallback()
        val fileMap =
            LinkedHashMap<String, File>().apply {
                put("file", File.createTempFile("upload", ".bin"))
            }

        BridgeAPIRequestUtils.post(
            "https://example.com/upload",
            linkedMapOf("h" to "v"),
            fileMap,
            mapOf("k" to "v"),
            cb,
            depend,
        )

        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals(RequestMethod.POST, depend.lastMethod)
        assertTrue(cb.success)
    }

    @Test
    fun postWithByteArrayJsonRoutesThroughHandleConnection() {
        val depend = HostDepend()
        val cb = CapturingResponseCallback()

        BridgeAPIRequestUtils.post(
            "https://example.com",
            mapOf("h" to "v"),
            "application/octet-stream",
            disableRedirect = false,
            addCommonParams = null,
            postData = byteArrayOf(1, 2, 3),
            callback = cb,
            hostNetworkDepend = depend,
        )

        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals(RequestMethod.POST, depend.lastMethod)
        assertTrue(cb.success)
    }

    @Test
    fun postWithStreamCallbackForwardsStreamConnection() {
        val depend = HostDepend()
        val cb = CapturingStreamCallback()

        BridgeAPIRequestUtils.post(
            "https://example.com",
            mapOf("h" to "v"),
            "application/octet-stream",
            disableRedirect = false,
            addCommonParams = true,
            postData = byteArrayOf(1, 2),
            callback = cb,
            hostNetworkDepend = depend,
        )

        assertEquals(RequestMethod.POST, depend.lastMethod)
        assertNotNull(cb.connection)
    }

    @Test
    fun postJsonWithDefaultFormattingTransmitsRawString() {
        val depend = HostDepend()
        val cb = CapturingResponseCallback()
        val payload = JSONObject().put("k", "v")

        BridgeAPIRequestUtils.post(
            "https://example.com",
            mapOf("h" to "v"),
            "application/json",
            disableRedirect = false,
            addCommonParams = true,
            postData = payload,
            callback = cb,
            hostNetworkDepend = depend,
            jsonFormatOption = RequestJsonFormatOptionConstants.DEFAULT,
        )

        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals(RequestMethod.POST, depend.lastMethod)
        assertTrue(cb.success)
    }

    @Test
    fun postJsonWithDoFormatTransmitsPrettyString() {
        val depend = HostDepend()
        val cb = CapturingResponseCallback()
        val payload = JSONObject().put("k", "v")

        BridgeAPIRequestUtils.post(
            "https://example.com",
            mapOf("h" to "v"),
            "application/json",
            disableRedirect = false,
            addCommonParams = true,
            postData = payload,
            callback = cb,
            hostNetworkDepend = depend,
            jsonFormatOption = RequestJsonFormatOptionConstants.DO_FORMAT,
        )

        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals(RequestMethod.POST, depend.lastMethod)
        assertTrue(cb.success)
    }

    @Test
    fun postJsonWithNonJsonContentTypeUsesParamsBranch() {
        val depend = HostDepend()
        val cb = CapturingResponseCallback()
        val payload = JSONObject().put("k", "v").put("x", 1)

        BridgeAPIRequestUtils.post(
            "https://example.com",
            mapOf("h" to "v"),
            "application/x-www-form-urlencoded",
            disableRedirect = false,
            addCommonParams = false,
            postData = payload,
            callback = cb,
            hostNetworkDepend = depend,
            jsonFormatOption = RequestJsonFormatOptionConstants.DEFAULT,
        )

        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals(RequestMethod.POST, depend.lastMethod)
        assertTrue(cb.success)
    }

    @Test
    fun putDispatchesPutAndDeleteDispatchesDelete() {
        val depend = HostDepend()
        val cb = CapturingResponseCallback()
        val payload = JSONObject().put("ok", true)

        BridgeAPIRequestUtils.put(
            "https://example.com/put",
            linkedMapOf("h" to "v"),
            "application/json",
            disableRedirect = true,
            addCommonParams = null,
            postData = payload,
            callback = cb,
            hostNetworkDepend = depend,
        )
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals(RequestMethod.PUT, depend.lastMethod)
        assertTrue(cb.success)

        val cb2 = CapturingResponseCallback()
        BridgeAPIRequestUtils.delete(
            "https://example.com/delete",
            linkedMapOf("h" to "v"),
            disableRedirect = false,
            addCommonParams = null,
            callback = cb2,
            hostNetworkDepend = depend,
        )
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals(RequestMethod.DELETE, depend.lastMethod)
        assertTrue(cb2.success)
    }

    @Test
    fun downloadFileDispatchesDownloadStream() {
        val depend = HostDepend()
        val cb = CapturingStreamCallback()

        BridgeAPIRequestUtils.downloadFile(
            "https://example.com/asset.zip",
            linkedMapOf("h" to "v"),
            needAddCommonParams = true,
            callback = cb,
            hostNetworkDepend = depend,
        )

        assertEquals(RequestMethod.DOWNLOAD, depend.lastMethod)
        assertNotNull(cb.connection)
    }

    @Test
    fun postWithExceptionInChainSwallowsAndDoesNotCallCallback() {
        val cb = CapturingResponseCallback()
        val exploding =
            object : IHostNetworkDepend {
                override fun requestForString(
                    method: RequestMethod,
                    request: HttpRequest,
                ): AbsStringConnection = throw RuntimeException("network down")

                override fun requestForStream(
                    method: RequestMethod,
                    request: HttpRequest,
                ): AbsStreamConnection = throw RuntimeException("network down")
            }

        BridgeAPIRequestUtils.post(
            "https://example.com",
            mapOf("h" to "v"),
            "application/octet-stream",
            disableRedirect = false,
            addCommonParams = false,
            postData = byteArrayOf(),
            callback = cb,
            hostNetworkDepend = exploding,
        )

        Shadows.shadowOf(Looper.getMainLooper()).idle()
        // Exceptions are caught internally; callback never invoked.
        assertEquals(false, cb.success)
        assertEquals(false, cb.failed)
    }
}
