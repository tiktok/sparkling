// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.runtime.depend.utils

import android.text.TextUtils
import android.util.Log
import com.tiktok.sparkling.method.runtime.depend.network.AbsStreamConnection
import com.tiktok.sparkling.method.runtime.depend.network.AbsStringConnection
import com.tiktok.sparkling.method.registry.api.util.ThreadPool
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.utils.JsonUtils
import com.tiktok.sparkling.method.runtime.depend.common.IHostNetworkDepend
import com.tiktok.sparkling.method.runtime.depend.network.HttpRequest
import com.tiktok.sparkling.method.runtime.depend.network.RequestJsonFormatOptionConstants
import org.json.JSONObject
import java.io.File

interface IResponseCallback {
    fun onSuccess(
        body: JSONObject,
        responseHeader: LinkedHashMap<String, String>,
        statusCode: Int?,
        clientCode: Int?,
    )

    fun onParsingFailed(
        body: JSONObject,
        responseHeader: LinkedHashMap<String, String>,
        rawResponse: String,
        throwable: Throwable,
        statusCode: Int?,
        clientCode: Int?,
    ): Unit? = null

    fun onFailed(
        errorCode: Int?,
        clientCode: Int?,
        throwable: Throwable,
    )
}

interface IStreamResponseCallback {
    fun handleConnection(connection: AbsStreamConnection?)
}

object BridgeAPIRequestUtils {
    private var TAG = BridgeAPIRequestUtils::class.java.simpleName

    const val REQUEST_TAG_FROM = "request_tag_from"
    private const val CONTENT_TYPE_JSON = "application/json"
    const val ERROR_CODE_408 = -408

    fun addParametersToUrl(
        url: String,
        params: Map<String, Any>?,
        type: BridgePlatformType,
    ): String {
        val urlBuilder = HttpUrlBuilder(url)
        params?.forEach {
            if (it.value is Map<*, *>) {
                urlBuilder.addParam(
                    it.key,
                    JsonUtils.toJSONObject(it.value as Map<String, Any>).toString(),
                )
            } else if (it.value is List<*>) {
                urlBuilder.addParam(
                    it.key,
                    JsonUtils.toJSONArray(it.value as List<*>).toString(),
                )
            } else {
                urlBuilder.addParam(it.key, it.value.toString())
            }
        }

        var tagFrom = ""
        if (type == BridgePlatformType.WEB) {
            tagFrom = "h5"
        } else if (type == BridgePlatformType.LYNX) {
            tagFrom = "lynx"
        }
        urlBuilder.addParam(REQUEST_TAG_FROM, tagFrom)
        Log.d(TAG, "build url is ${urlBuilder.build()}")
        return urlBuilder.build()
    }

    fun get(
        targetUrl: String,
        headers: Map<String, String>,
        disableRedirect: Boolean,
        addCommonParams: Boolean?,
        callback: IResponseCallback,
        hostNetworkDepend: IHostNetworkDepend,
    ) {
        val connection =
            HttpRequest(targetUrl)
                .headers(headers as LinkedHashMap<String, String>)
                .needAddCommonParams(addCommonParams ?: true)
                .disableRedirect(disableRedirect)
                .doGetForString(hostNetworkDepend)
        handleConnection(connection, callback)
    }

    // upload image
    fun post(
        targetUrl: String,
        headers: LinkedHashMap<String, String>,
        postFilePart: LinkedHashMap<String, File>,
        params: Map<String, String>,
        callback: IResponseCallback,
        hostNetworkDepend: IHostNetworkDepend,
    ) {
        val connection =
            HttpRequest(targetUrl)
                .headers(headers)
                .postFilePart(postFilePart)
                .params(params)
                .needAddCommonParams(true)
                .doPostForString(hostNetworkDepend)
        handleConnection(connection, callback)
    }

    fun post(
        targetUrl: String,
        headers: Map<String, String>,
        contentType: String,
        disableRedirect: Boolean,
        addCommonParams: Boolean?,
        postData: ByteArray,
        callback: IStreamResponseCallback,
        hostNetworkDepend: IHostNetworkDepend,
    ) {
        try {
            val linkedHashMap =
                LinkedHashMap<String, String>().apply {
                    putAll(headers)
                }
            val connection =
                HttpRequest(targetUrl)
                    .headers(linkedHashMap)
                    .contentType(contentType)
                    .needAddCommonParams(addCommonParams ?: true)
                    .sendData(postData)
                    .disableRedirect(disableRedirect)
                    .doPostForStream(hostNetworkDepend)

            callback.handleConnection(connection)
        } catch (throwable: Throwable) {
            Log.e(TAG, "get failed", throwable)
        }
    }

    fun post(
        targetUrl: String,
        headers: Map<String, String>,
        contentType: String,
        disableRedirect: Boolean,
        addCommonParams: Boolean?,
        postData: ByteArray,
        callback: IResponseCallback,
        hostNetworkDepend: IHostNetworkDepend,
    ) {
        try {
            val linkedHashMap =
                LinkedHashMap<String, String>().apply {
                    putAll(headers)
                }
            val connection =
                HttpRequest(targetUrl)
                    .headers(linkedHashMap)
                    .contentType(contentType)
                    .needAddCommonParams(addCommonParams ?: true)
                    .sendData(postData)
                    .disableRedirect(disableRedirect)
                    .doPostForString(hostNetworkDepend)

            handleConnection(connection, callback)
        } catch (throwable: Throwable) {
            Log.e(TAG, "get failed", throwable)
        }
    }

    fun post(
        targetUrl: String,
        headers: Map<String, String>,
        contentType: String,
        disableRedirect: Boolean,
        addCommonParams: Boolean?,
        postData: JSONObject,
        callback: IResponseCallback,
        hostNetworkDepend: IHostNetworkDepend,
        jsonFormatOption: Int?,
    ) {
        try {
            val linkedHashMap =
                LinkedHashMap<String, String>().apply {
                    putAll(headers)
                }
            val connection: AbsStringConnection?
            if (contentType == CONTENT_TYPE_JSON) {
                var sendData: ByteArray? = null
                if (jsonFormatOption == RequestJsonFormatOptionConstants.DEFAULT ||
                    jsonFormatOption == RequestJsonFormatOptionConstants.DONT_FORMAT
                ) {
                    sendData = postData.toString().toByteArray(charset("UTF-8"))
                } else if (jsonFormatOption == RequestJsonFormatOptionConstants.DO_FORMAT) {
                    sendData = postData.toString(2).toByteArray(charset("UTF-8"))
                }
                connection =
                    HttpRequest(targetUrl)
                        .headers(linkedHashMap)
                        .contentType(contentType)
                        .needAddCommonParams(addCommonParams ?: true)
                        .disableRedirect(disableRedirect)
                        .sendData(sendData)
                        .doPostForString(hostNetworkDepend)
            } else {
                val map = LinkedHashMap<String, String>()
                val iterator = postData.keys()
                while (iterator.hasNext()) {
                    val key = iterator.next()
                    val value = postData.optString(key, "")
                    map[key] = value
                }
                connection =
                    HttpRequest(targetUrl)
                        .headers(linkedHashMap)
                        .params(map)
                        .needAddCommonParams(addCommonParams ?: true)
                        .disableRedirect(disableRedirect)
                        .doPostForString(hostNetworkDepend)
            }

            handleConnection(connection, callback)
        } catch (throwable: Throwable) {
            Log.e(TAG, "get failed", throwable)
        }
    }

    fun put(
        targetUrl: String,
        headers: Map<String, String>,
        contentType: String,
        disableRedirect: Boolean,
        addCommonParams: Boolean?,
        postData: JSONObject,
        callback: IResponseCallback,
        hostNetworkDepend: IHostNetworkDepend,
    ) {
        val connection =
            HttpRequest(targetUrl)
                .headers(headers as LinkedHashMap<String, String>)
                .contentType(contentType)
                .needAddCommonParams(addCommonParams ?: true)
                .disableRedirect(disableRedirect)
                .sendData(postData.toString().toByteArray(charset("UTF-8")))
                .doPutForString(hostNetworkDepend)

        handleConnection(connection, callback)
    }

    fun delete(
        targetUrl: String,
        headers: Map<String, String>,
        disableRedirect: Boolean,
        addCommonParams: Boolean?,
        callback: IResponseCallback,
        hostNetworkDepend: IHostNetworkDepend,
    ) {
        val connection =
            HttpRequest(targetUrl)
                .headers(headers as LinkedHashMap<String, String>)
                .needAddCommonParams(addCommonParams ?: true)
                .disableRedirect(disableRedirect)
                .doDeleteForString(hostNetworkDepend)

        handleConnection(connection, callback)
    }

    fun downloadFile(
        targetUrl: String,
        headers: LinkedHashMap<String, String>,
        needAddCommonParams: Boolean,
        callback: IStreamResponseCallback,
        hostNetworkDepend: IHostNetworkDepend,
    ) {
        val connection =
            HttpRequest(targetUrl)
                .headers(headers)
                .needAddCommonParams(needAddCommonParams)
                .doDownloadFile(hostNetworkDepend)

        callback.handleConnection(connection)
    }

    private fun handleConnection(
        connection: AbsStringConnection?,
        callback: IResponseCallback,
    ) {
        if (connection == null) {
            Log.d(TAG, "connection is null")
            handleError(ERROR_CODE_408, null, "connection failed", null, callback)
            return
        }

        val body = connection.getStringResponseBody()?.takeIf { it.isNotEmpty() }

        if (body == null) {
            Log.d(TAG, "response body is null")
            if (!handleError(connection.getResponseCode(), connection.getClientCode(), connection.getErrorMsg(), connection.getException(), callback)) {
                handleSuccess(body, connection.getResponseHeader(), connection.getResponseCode(), connection.getClientCode(), callback)
            }
            return
        }

        if (!handleError(connection.getResponseCode(), connection.getClientCode(), connection.getErrorMsg(), connection.getException(), callback)) {
            Log.d(TAG, "handle response body")
            handleSuccess(body, connection.getResponseHeader(), connection.getResponseCode(), connection.getClientCode(), callback)
        }
    }

    private fun handleError(
        errorCode: Int?,
        clientCode: Int?,
        errorMsg: String?,
        throwable: Throwable?,
        callback: IResponseCallback,
    ): Boolean {
        if (throwable != null || !TextUtils.isEmpty(errorMsg)) {
            val nonNullErrMsg = errorMsg?.takeIf { it.isNotEmpty() } ?: throwable?.message ?: ""
            ThreadPool.runInMain {
                runCatching {
                    callback.onFailed(errorCode, clientCode, throwable ?: Throwable(nonNullErrMsg))
                }
            }
            Log.d(TAG, "handle error finish")
            return true
        }
        return false
    }

    private fun handleSuccess(
        body: String?,
        respHeader: LinkedHashMap<String, String>,
        respCode: Int?,
        clientCode: Int?,
        callback: IResponseCallback,
    ) {
        ThreadPool.runInMain {
            runCatching {
                var response: JSONObject
                var errorMsg: String? = null
                var throwable: Throwable? = null
                var rawResponse: String? = null
                try {
                    response = JSONObject(body)
                } catch (e: Throwable) {
                    response = JSONObject()
                    errorMsg = e.javaClass.toString() + ":" + e.message
                    throwable = e
                    rawResponse = body
                }
                if (errorMsg?.isNotEmpty() == true || throwable != null || rawResponse != null) {
                    callback.onParsingFailed(response, respHeader, rawResponse ?: "", throwable ?: Throwable(errorMsg), respCode, clientCode)
                } else {
                    callback.onSuccess(response, respHeader, respCode, clientCode)
                }
            }
        }
    }

    private fun handleSuccess(
        response: JSONObject,
        respHeader: LinkedHashMap<String, String>,
        respCode: Int?,
        clientCode: Int?,
        callback: IResponseCallback,
    ) {
        ThreadPool.runInMain {
            runCatching {
                callback.onSuccess(response, respHeader, respCode, clientCode)
            }
        }
    }

    fun filterHeaderEmptyValue(header: Map<String, Any>?): java.util.LinkedHashMap<String, String> {
        val resultHeader = linkedMapOf<String, String>()
        header?.let { headers ->
            headers.forEach {
                val key = it.key
                val value = it.value
                if (value is String && value.isNotEmpty()) {
                    resultHeader[key] = value
                }
            }
        }
        return resultHeader
    }

    /**
     * @param params XReadableMap?
     * @return Map<String, String>
     */
    fun convertParamValueToString(params: Map<String, Any>?): Map<String, String> {
        val resultParams =
            params
                ?.filter { (_, value) ->
                    value is Int || value is Number || value is Boolean || value is String
                }?.mapValues { (_, value) -> value.toString() }

        return resultParams ?: LinkedHashMap<String, String>()
    }
}
