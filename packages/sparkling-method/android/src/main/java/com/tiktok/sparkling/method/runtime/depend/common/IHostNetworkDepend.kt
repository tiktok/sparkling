// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.runtime.depend.common

import androidx.annotation.Keep
import com.tiktok.sparkling.method.runtime.depend.network.AbsStreamConnection
import com.tiktok.sparkling.method.runtime.depend.network.AbsStringConnection
import com.tiktok.sparkling.method.runtime.depend.network.HttpRequest
import com.tiktok.sparkling.method.runtime.depend.network.RequestMethod

@Keep
interface IHostNetworkDepend {
    fun getAPIParams(): Map<String, Any>? = null

    fun requestForString(
        method: RequestMethod,
        request: HttpRequest,
    ): AbsStringConnection

    fun requestForStream(
        method: RequestMethod,
        request: HttpRequest,
    ): AbsStreamConnection
}
