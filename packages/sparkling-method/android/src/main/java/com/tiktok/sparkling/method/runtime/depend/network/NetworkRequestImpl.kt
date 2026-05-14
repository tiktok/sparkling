// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.runtime.depend.network

import com.tiktok.sparkling.method.runtime.depend.network.AbsStringConnection
import com.tiktok.sparkling.method.runtime.depend.common.IHostNetworkDepend

/**
 * Delegate network requests to host-provided implementation.
 */
object NetworkRequestImpl {
    fun requestForString(
        method: RequestMethod,
        request: HttpRequest,
        hostNetworkDepend: IHostNetworkDepend,
    ): AbsStringConnection = hostNetworkDepend.requestForString(method, request)

    fun requestForStream(
        method: RequestMethod,
        request: HttpRequest,
        hostNetworkDepend: IHostNetworkDepend,
    ): AbsStreamConnection = hostNetworkDepend.requestForStream(method, request)
}
