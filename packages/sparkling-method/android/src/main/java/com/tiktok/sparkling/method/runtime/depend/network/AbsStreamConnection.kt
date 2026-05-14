// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.runtime.depend.network

import java.io.InputStream

abstract class AbsStreamConnection {
    open fun getInputStreamResponseBody(): InputStream? = null

    open fun getResponseHeader(): LinkedHashMap<String, String> = LinkedHashMap<String, String>()

    open fun getResponseCode(): Int = 0

    open fun getClientCode(): Int = 0

    open fun getErrorMsg(): String = ""

    open fun getException(): Throwable? = null

    open fun cancel() {
    }
}
