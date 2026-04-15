// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.hybridkit.scheme

import android.net.Uri
import android.os.Bundle
import com.tiktok.sparkling.hybridkit.base.HybridContainerType
import com.tiktok.sparkling.hybridkit.base.HybridKitType
import com.tiktok.sparkling.hybridkit.utils.safeGetQueryParameter
import java.util.concurrent.ConcurrentHashMap

object SparklingUriParser {

    private val parsedUriMap: MutableMap<String, MutableMap<String, String>> =
        ConcurrentHashMap()

    @JvmStatic
    fun queryParsedParams(containerId: String): MutableMap<String, String> {
        val result = parsedUriMap[containerId]
        return result ?: mutableMapOf()
    }

    @JvmStatic
    @JvmOverloads
    fun parseQueryMap(
        uri: Uri,
        extra: Map<String, String>? = null,
        bundle: Bundle? = null
    ): MutableMap<String, String> {
        // priority: extra < innerUri < Uri
        val queryMap = mutableMapOf<String, String>()
        if (extra != null) {
            queryMap.putAll(extra)
        }
        if (bundle != null) {
            queryMap.putAll(bundleToMap(bundle))
        }

        // SECURITY: The nested "url" query parameter is parsed and its query keys are merged
        // into queryMap without scheme validation. A malicious deep link could inject a
        // javascript:, file:, or data: URL here. Callers that consume these merged parameters
        // to load resources should enforce an https-only allowlist before use.
        runCatching {
            var url = uri.safeGetQueryParameter("url")
            if (url == null) {
                url = runCatching { bundle?.getString("url") }.getOrNull()
            }
            if (url == null) {
                url = extra?.get("url")
            }
            url?.let {
                Uri.parse(it)
            }?.let { _uri ->
                queryMap.putAll(parseUri(_uri))
            }
        }

        queryMap.putAll(parseUri(uri))
        return queryMap
    }

    @JvmStatic
    @JvmOverloads
    internal fun parseUri(
        uri: Uri,
        extra: Map<String, String>? = null
    ): MutableMap<String, String> {
        val queryMap = mutableMapOf<String, String>()
        if (extra != null) {
            queryMap.putAll(extra)
        }
        runCatching {
            val queryParameterNames = uri.queryParameterNames
            for (queryName in queryParameterNames) {
                val queryValue = uri.safeGetQueryParameter(queryName)
                if (queryValue != null) {
                    queryMap[queryName] = queryValue
                }
            }
        }
        return queryMap
    }

    @JvmStatic
    fun saveUriAndQueries(
        containerId: String,
        queryMap: MutableMap<String, String>
    ) {
        parsedUriMap[containerId] = queryMap
    }

    /**
     * Appends query keys from [mergedParams] that are not already present on [originUri].
     */
    @JvmStatic
    fun appendExtraQueryToUri(originUri: Uri, mergedParams: Map<String, String>): Uri {
        val existingNames = originUri.queryParameterNames
        val builder = originUri.buildUpon().clearQuery()
        for (name in originUri.queryParameterNames) {
            for (value in originUri.getQueryParameters(name)) {
                builder.appendQueryParameter(name, value)
            }
        }
        for ((key, value) in mergedParams) {
            if (!existingNames.contains(key)) {
                builder.appendQueryParameter(key, value)
            }
        }
        return builder.build()
    }


    inline fun <reified T : BaseSchemeParam> T.applyEngine(uri: Uri) {
        this.apply {
            val host = uri.host?.lowercase()
            engineType = when {
                host?.contains(SchemeConstants.Host.WEB_VIEW) == true -> {
                    HybridKitType.WEB
                }
                host?.contains(SchemeConstants.Host.LYNX_VIEW) == true -> {
                    HybridKitType.LYNX
                }
                else -> {
                    HybridKitType.UNKNOWN
                }
            }

            if (this is HybridSchemeParam) {
                containerType = when {
                    host?.contains(SchemeConstants.Host.LYNX_VIEW_CARD) == true -> HybridContainerType.CARD
                    host?.contains(SchemeConstants.Host.LYNX_VIEW_PAGE) == true -> HybridContainerType.PAGE
                    host?.contains(SchemeConstants.Host.LYNX_VIEW) == true -> HybridContainerType.PAGE
                    else -> HybridContainerType.UNKNOWN
                }
            }
        }
    }

    fun bundleToMap(bundle: Bundle): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (key in bundle.keySet()) {
            val v = bundle.get(key)
            map[key] = v.toString()
        }
        return map
    }
}
