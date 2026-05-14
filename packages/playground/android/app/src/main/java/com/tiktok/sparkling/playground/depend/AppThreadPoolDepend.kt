// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.playground.depend

import com.tiktok.sparkling.method.runtime.depend.common.IHostThreadPoolExecutorDepend
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AppThreadPoolDepend : IHostThreadPoolExecutorDepend {
    private val executor: ExecutorService by lazy { Executors.newCachedThreadPool() }

    override fun getNormalThreadExecutor(): ExecutorService = executor
}
