// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.runtime.depend.common

import androidx.annotation.Keep
import java.util.concurrent.ExecutorService

@Keep
interface IHostThreadPoolExecutorDepend {
    fun getNormalThreadExecutor(): ExecutorService
}
