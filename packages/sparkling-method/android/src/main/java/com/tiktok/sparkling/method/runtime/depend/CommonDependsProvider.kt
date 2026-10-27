// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.runtime.depend

import com.tiktok.sparkling.method.runtime.depend.common.IHostNetworkDepend
import com.tiktok.sparkling.method.runtime.depend.common.IHostPermissionDepend
import com.tiktok.sparkling.method.runtime.depend.common.IHostThreadPoolExecutorDepend

/**
 * Host implementations read by the bridge runtime and its methods.
 *
 * The host sets each field once at startup. [hostThreadPoolExecutorDepend] is read by the runtime;
 * [hostNetworkDepend] and [hostPermissionDepend] are read by methods that make network requests or
 * ask for runtime permissions.
 */
object CommonDependsProvider {
    var hostNetworkDepend: IHostNetworkDepend? = null
    var hostPermissionDepend: IHostPermissionDepend? = null
    var hostThreadPoolExecutorDepend: IHostThreadPoolExecutorDepend? = null
}
