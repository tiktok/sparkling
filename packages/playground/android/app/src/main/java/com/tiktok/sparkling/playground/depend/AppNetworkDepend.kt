// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.playground.depend

import com.tiktok.sparkling.method.runtime.depend.network.AbsStringConnection
import com.tiktok.sparkling.method.runtime.depend.common.IHostNetworkDepend
import com.tiktok.sparkling.method.runtime.depend.network.AbsStreamConnection
import com.tiktok.sparkling.method.runtime.depend.network.HttpRequest
import com.tiktok.sparkling.method.runtime.depend.network.RequestMethod
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.QueryMap
import retrofit2.http.Streaming
import java.io.InputStream
import java.net.URI
import java.util.LinkedHashMap
import java.util.concurrent.TimeUnit

/**
 * Host-side network depend demo using Retrofit/OkHttp.
 */
class AppNetworkDepend : IHostNetworkDepend {
    private interface HostNetworkApi {
        @GET("{path}")
        fun getString(
            @Path(value = "path", encoded = true) path: String,
            @QueryMap(encoded = true) query: Map<String, String>,
            @HeaderMap headers: Map<String, String>,
        ): Call<ResponseBody>

        @Streaming
        @GET("{path}")
        fun getStream(
            @Path(value = "path", encoded = true) path: String,
            @QueryMap(encoded = true) query: Map<String, String>,
            @HeaderMap headers: Map<String, String>,
        ): Call<ResponseBody>

        @FormUrlEncoded
        @POST("{path}")
        fun postForm(
            @Path(value = "path", encoded = true) path: String,
            @QueryMap(encoded = true) query: Map<String, String>,
            @HeaderMap headers: Map<String, String>,
            @FieldMap(encoded = true) fields: Map<String, String>,
        ): Call<ResponseBody>

        @POST("{path}")
        fun postBody(
            @Path(value = "path", encoded = true) path: String,
            @QueryMap(encoded = true) query: Map<String, String>,
            @HeaderMap headers: Map<String, String>,
            @Body body: RequestBody,
        ): Call<ResponseBody>

        @Multipart
        @POST("{path}")
        fun postMultipart(
            @Path(value = "path", encoded = true) path: String,
            @QueryMap(encoded = true) query: Map<String, String>,
            @HeaderMap headers: Map<String, String>,
            @Part parts: List<MultipartBody.Part>,
        ): Call<ResponseBody>

        @PUT("{path}")
        fun putBody(
            @Path(value = "path", encoded = true) path: String,
            @QueryMap(encoded = true) query: Map<String, String>,
            @HeaderMap headers: Map<String, String>,
            @Body body: RequestBody,
        ): Call<ResponseBody>

        @DELETE("{path}")
        fun deleteCall(
            @Path(value = "path", encoded = true) path: String,
            @QueryMap(encoded = true) query: Map<String, String>,
            @HeaderMap headers: Map<String, String>,
        ): Call<ResponseBody>

        @Streaming
        @GET("{path}")
        fun download(
            @Path(value = "path", encoded = true) path: String,
            @QueryMap(encoded = true) query: Map<String, String>,
            @HeaderMap headers: Map<String, String>,
        ): Call<ResponseBody>
    }

    override fun getAPIParams(): Map<String, Any>? = emptyMap()

    override fun requestForString(
        method: RequestMethod,
        request: HttpRequest,
    ): AbsStringConnection {
        val parsed = parseUrl(request.getUrl()) ?: return errorString("Invalid url: ${request.getUrl()}")
        val service = createService(parsed.baseUrl) ?: return errorString("Retrofit unavailable for ${parsed.baseUrl}")
        val headers = buildHeaders(request)
        val query = LinkedHashMap(parsed.query)

        val call: Call<ResponseBody>? =
            when (method) {
                RequestMethod.GET -> {
                    service.getString(parsed.relativePath, query, headers)
                }

                RequestMethod.POST -> {
                    val body = buildBody(request)
                    when {
                        body is MultipartBody -> service.postMultipart(parsed.relativePath, query, headers, body.parts)
                        body != null -> service.postBody(parsed.relativePath, query, headers, body)
                        else -> service.postForm(parsed.relativePath, query, headers, request.params ?: emptyMap())
                    }
                }

                RequestMethod.PUT -> {
                    val body = buildBody(request) ?: RequestBody.create(null, ByteArray(0))
                    service.putBody(parsed.relativePath, query, headers, body)
                }

                RequestMethod.DELETE -> {
                    service.deleteCall(parsed.relativePath, query, headers)
                }

                RequestMethod.DOWNLOAD -> {
                    service.download(parsed.relativePath, query, headers)
                }
            }

        return executeString(call)
    }

    override fun requestForStream(
        method: RequestMethod,
        request: HttpRequest,
    ): AbsStreamConnection {
        val parsed = parseUrl(request.getUrl()) ?: return errorStream("Invalid url: ${request.getUrl()}", null)
        val service = createService(parsed.baseUrl) ?: return errorStream("Retrofit unavailable for ${parsed.baseUrl}", null)
        val headers = buildHeaders(request)
        val query = LinkedHashMap(parsed.query)

        val call: Call<ResponseBody>? =
            when (method) {
                RequestMethod.GET -> {
                    service.getStream(parsed.relativePath, query, headers)
                }

                RequestMethod.POST -> {
                    val body = buildBody(request)
                    when {
                        body is MultipartBody -> service.postMultipart(parsed.relativePath, query, headers, body.parts)
                        body != null -> service.postBody(parsed.relativePath, query, headers, body)
                        else -> service.postForm(parsed.relativePath, query, headers, request.params ?: emptyMap())
                    }
                }

                RequestMethod.DOWNLOAD -> {
                    service.download(parsed.relativePath, query, headers)
                }

                else -> {
                    null
                }
            }

        return executeStream(call)
    }

    private fun createService(baseUrl: String): HostNetworkApi? =
        try {
            Retrofit
                .Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(defaultClient)
                .build()
                .create(HostNetworkApi::class.java)
        } catch (_: Throwable) {
            null
        }

    private val defaultClient: OkHttpClient by lazy {
        val logging =
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        OkHttpClient
            .Builder()
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun parseUrl(raw: String): ParsedUrl? =
        runCatching {
            val uri = URI(raw)
            val base = "${uri.scheme}://${uri.authority}/"
            val rel = uri.rawPath.trimStart('/').ifEmpty { "" }
            val query = LinkedHashMap<String, String>()
            uri.rawQuery?.split("&")?.forEach { pair ->
                if (pair.isNotEmpty()) {
                    val parts = pair.split("=", limit = 2)
                    val key = parts[0]
                    val value = if (parts.size > 1) parts[1] else ""
                    query[key] = value
                }
            }
            ParsedUrl(base, rel, query)
        }.getOrNull()

    private fun buildHeaders(request: HttpRequest): LinkedHashMap<String, String> {
        val headers = LinkedHashMap<String, String>()
        request.headers?.let { headers.putAll(it) }
        request.contentEncoding?.let { headers["Content-Encoding"] = it }
        request.contentType?.let { headers["Content-Type"] = it }
        return headers
    }

    private fun buildBody(request: HttpRequest): RequestBody? {
        request.postFilePart?.takeIf { it.isNotEmpty() }?.let { files ->
            val multi = MultipartBody.Builder().setType(MultipartBody.FORM)
            request.params?.forEach { (k, v) -> multi.addFormDataPart(k, v) }
            files.forEach { (k, file) ->
                val media = request.contentType?.toMediaTypeOrNull()
                multi.addFormDataPart(k, file.name, RequestBody.create(media, file))
            }
            return multi.build()
        }
        request.sendData?.let { data ->
            return RequestBody.create(request.contentType?.toMediaTypeOrNull(), data)
        }
        return null
    }

    private fun executeString(call: Call<ResponseBody>?): AbsStringConnection {
        var response: Response<ResponseBody>? = null
        return try {
            if (call == null) return errorString("call is null")
            response = call.execute()
            val headers =
                LinkedHashMap<String, String>().apply {
                    response?.headers()?.forEach { pair -> put(pair.first, pair.second) }
                }
            val code = response?.code() ?: -1
            val body = response?.body()?.string()
            object : AbsStringConnection() {
                override fun getResponseHeader(): LinkedHashMap<String, String> = headers

                override fun getResponseCode(): Int = code

                override fun getStringResponseBody(): String? = body

                override fun getErrorMsg(): String = ""
            }
        } catch (e: Throwable) {
            errorString(e.message ?: "request error", e)
        } finally {
            try {
                response?.body()?.close()
            } catch (_: Throwable) {
            }
        }
    }

    private fun executeStream(call: Call<ResponseBody>?): AbsStreamConnection {
        return try {
            if (call == null) return errorStream("call is null", null)
            val response = call.execute()
            val headers =
                LinkedHashMap<String, String>().apply {
                    response.headers().forEach { pair -> put(pair.first, pair.second) }
                }
            val code = response.code()
            val stream = response.body()?.byteStream()
            object : AbsStreamConnection() {
                override fun getResponseHeader(): LinkedHashMap<String, String> = headers

                override fun getResponseCode(): Int = code

                override fun getInputStreamResponseBody(): InputStream? = stream

                override fun getErrorMsg(): String = ""

                override fun cancel() {
                    try {
                        stream?.close()
                    } catch (_: Throwable) {
                    }
                    try {
                        response.body()?.close()
                    } catch (_: Throwable) {
                    }
                    try {
                        call.cancel()
                    } catch (_: Throwable) {
                    }
                }
            }
            // NOTE: Do NOT close the response body here - the caller needs
            // to read from the stream first. The cancel() method handles cleanup.
        } catch (e: Throwable) {
            errorStream(e.message ?: "request error", e)
        }
    }

    private fun errorString(
        msg: String,
        throwable: Throwable? = null,
    ): AbsStringConnection =
        object : AbsStringConnection() {
            override fun getErrorMsg(): String = msg

            override fun getException(): Throwable? = throwable
        }

    private fun errorStream(
        msg: String,
        throwable: Throwable?,
    ): AbsStreamConnection =
        object : AbsStreamConnection() {
            override fun getErrorMsg(): String = msg

            override fun getException(): Throwable? = throwable
        }

    private data class ParsedUrl(
        val baseUrl: String,
        val relativePath: String,
        val query: LinkedHashMap<String, String>,
    )
}
