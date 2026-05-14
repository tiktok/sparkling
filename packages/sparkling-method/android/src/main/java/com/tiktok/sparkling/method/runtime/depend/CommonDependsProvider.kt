// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.runtime.depend

import com.tiktok.sparkling.method.runtime.depend.common.IHostPermissionDepend
import com.tiktok.sparkling.method.runtime.depend.common.IHostThreadPoolExecutorDepend
import com.tiktok.sparkling.method.runtime.depend.common.IHostNetworkDepend

object CommonDependsProvider {
    var hostNetworkDepend: IHostNetworkDepend? = null
    var hostPermissionDepend: IHostPermissionDepend? = null
    var hostThreadPoolExecutorDepend: IHostThreadPoolExecutorDepend? = null
}
