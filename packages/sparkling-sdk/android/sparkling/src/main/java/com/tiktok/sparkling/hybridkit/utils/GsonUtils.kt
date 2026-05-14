// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.utils

import com.google.gson.Gson
import com.google.gson.JsonElement

object GsonUtils {
    private val gson: Gson by lazy { Gson() }

    fun toJson(any: Any?): String? = gson.toJson(any)

    fun <T> fromJson(
        jsonString: String?,
        objectClass: Class<T>?,
    ): T = gson.fromJson(jsonString, objectClass)

    fun <T> fromJson(
        jsonElement: JsonElement?,
        objectClass: Class<T>?,
    ): T = gson.fromJson(jsonElement, objectClass)
}
