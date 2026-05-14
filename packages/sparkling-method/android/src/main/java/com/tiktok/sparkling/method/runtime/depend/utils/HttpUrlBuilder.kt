// Copyright (c) 2023 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.runtime.depend.utils

import java.io.UnsupportedEncodingException
import java.net.URLEncoder

class HttpUrlBuilder {
    companion object {
        private const val PARAMETER_SEPARATOR = "&"
        private const val NAME_VALUE_SEPARATOR = "="
        private const val ENCODING_UTF_8 = "UTF-8"

        private fun formatUrl(
            parameters: Map<String, String?>,
            encoding: String?,
        ): String {
            val result = StringBuilder()
            val keySet = parameters.keys
            if (keySet == null || keySet.isEmpty()) return ""
            for (key in keySet) {
                val encodedName = encode(key, encoding)
                val value = parameters[key]
                val encodedValue = value?.let { encode(it, encoding) } ?: ""
                if (result.isNotEmpty()) result.append(PARAMETER_SEPARATOR)
                result.append(encodedName)
                result.append(NAME_VALUE_SEPARATOR)
                result.append(encodedValue)
            }
            return result.toString()
        }

        private fun encode(
            content: String,
            encoding: String?,
        ): String =
            try {
                if (encoding == null) {
                    URLEncoder.encode(content, "ISO_8859_1")
                } else {
                    if (encoding == "null_encoding") {
                        content
                    } else {
                        URLEncoder.encode(content, encoding)
                    }
                }
            } catch (problem: UnsupportedEncodingException) {
                throw IllegalArgumentException(problem)
            }
    }

    private val params = HashMap<String, String>()
    var url: String
    var encoding: String = ENCODING_UTF_8

    constructor(url: String) {
        this.url = url
    }

    fun addParam(
        name: String,
        value: Int,
    ): HttpUrlBuilder {
        params[name] = value.toString()
        return this
    }

    fun addParam(
        name: String,
        value: Long,
    ): HttpUrlBuilder {
        params[name] = value.toString()
        return this
    }

    fun addParam(
        name: String,
        value: Double,
    ): HttpUrlBuilder {
        params[name] = value.toString()
        return this
    }

    fun addParam(
        name: String,
        value: String,
    ): HttpUrlBuilder {
        params[name] = value
        return this
    }

    fun build(): String {
        if (params.isEmpty()) {
            return url
        }
        val formatUrl = formatUrl(params, encoding)
        if (url == null || url.isEmpty()) return formatUrl
        val index = url.indexOf('?')
        return if (index >= 0) {
            "$url&$formatUrl"
        } else {
            "$url?$formatUrl"
        }
    }

    override fun toString(): String = build()
}
