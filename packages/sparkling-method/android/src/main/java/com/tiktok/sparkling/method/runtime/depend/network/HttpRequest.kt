// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.runtime.depend.network

import com.tiktok.sparkling.method.runtime.depend.network.AbsStringConnection
import com.tiktok.sparkling.method.runtime.depend.common.IHostNetworkDepend
import java.io.File

class HttpRequest(
    private var url: String,
) {
    var cacheTime = 0
        private set
    var headers: LinkedHashMap<String, String>? = null
        private set
    var needAddCommonParams = false
        private set
    var disableRedirect = false
        private set
    var params: Map<String, String>? = null
        private set
    var sendData: ByteArray? = null
        private set
    var contentEncoding: String? = null
        private set
    var contentType: String? = null
        private set
    var connectTimeOut: Long = 0
        private set
    var readTimeOut: Long = 0
        private set
    var writeTimeOut: Long = 0
        private set
    var postFilePart: LinkedHashMap<String, File>? = null
        private set

    fun getUrl(): String = url

    fun cacheTime(cacheTime: Int): HttpRequest {
        this.cacheTime = cacheTime
        return this
    }

    fun headers(headers: LinkedHashMap<String, String>): HttpRequest {
        this.headers = headers
        return this
    }

    fun needAddCommonParams(needAddCommonParams: Boolean): HttpRequest {
        this.needAddCommonParams = needAddCommonParams
        return this
    }

    fun disableRedirect(disableRedirect: Boolean): HttpRequest {
        this.disableRedirect = disableRedirect
        return this
    }

    fun params(params: Map<String, String>): HttpRequest {
        this.params = params
        return this
    }

    fun postFilePart(postFilePart: LinkedHashMap<String, File>): HttpRequest {
        this.postFilePart = postFilePart
        return this
    }

    fun sendData(sendData: ByteArray?): HttpRequest {
        this.sendData = sendData
        return this
    }

    fun contentEncoding(contentEncoding: String): HttpRequest {
        this.contentEncoding = contentEncoding
        return this
    }

    fun contentType(contentType: String): HttpRequest {
        this.contentType = contentType
        return this
    }

    fun connectTimeOut(connectTimeOut: Long): HttpRequest {
        this.connectTimeOut = connectTimeOut
        return this
    }

    fun readTimeOut(readTimeOut: Long): HttpRequest {
        this.readTimeOut = readTimeOut
        return this
    }

    fun writeTimeOut(writeTimeOut: Long): HttpRequest {
        this.writeTimeOut = writeTimeOut
        return this
    }

    fun doGetForString(hostNetworkDepend: IHostNetworkDepend): AbsStringConnection = NetworkRequestImpl.requestForString(RequestMethod.GET, this, hostNetworkDepend)

    fun doGetForStream(hostNetworkDepend: IHostNetworkDepend): AbsStreamConnection = NetworkRequestImpl.requestForStream(RequestMethod.GET, this, hostNetworkDepend)

    fun doPostForString(hostNetworkDepend: IHostNetworkDepend): AbsStringConnection = NetworkRequestImpl.requestForString(RequestMethod.POST, this, hostNetworkDepend)

    fun doPostForStream(hostNetworkDepend: IHostNetworkDepend): AbsStreamConnection = NetworkRequestImpl.requestForStream(RequestMethod.POST, this, hostNetworkDepend)

    fun doPutForString(hostNetworkDepend: IHostNetworkDepend): AbsStringConnection = NetworkRequestImpl.requestForString(RequestMethod.PUT, this, hostNetworkDepend)

    fun doDeleteForString(hostNetworkDepend: IHostNetworkDepend): AbsStringConnection = NetworkRequestImpl.requestForString(RequestMethod.DELETE, this, hostNetworkDepend)

    fun doDownloadFile(hostNetworkDepend: IHostNetworkDepend): AbsStreamConnection = NetworkRequestImpl.requestForStream(RequestMethod.DOWNLOAD, this, hostNetworkDepend)
}

enum class RequestMethod {
    GET,
    POST,
    PUT,
    DELETE,
    DOWNLOAD,
}
