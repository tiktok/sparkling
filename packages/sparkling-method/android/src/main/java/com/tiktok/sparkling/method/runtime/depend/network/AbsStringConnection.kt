// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.runtime.depend.network

abstract class AbsStringConnection {
    open fun getStringResponseBody(): String? = null

    open fun getResponseHeader(): LinkedHashMap<String, String> = LinkedHashMap()

    open fun getResponseCode(): Int? = null

    open fun getErrorMsg(): String = ""

    open fun getException(): Throwable? = null

    open fun getClientCode(): Int? = null
}
