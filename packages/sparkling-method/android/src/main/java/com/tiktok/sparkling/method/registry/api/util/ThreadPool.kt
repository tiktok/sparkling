// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.api.util

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.tiktok.sparkling.method.runtime.depend.CommonDependsProvider
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Shared pool for bridge work that must leave the calling thread.
 *
 * [configExecutorService] replaces the executor directly. Otherwise [ensureExecutorNotNull] uses
 * [CommonDependsProvider.hostThreadPoolExecutorDepend], then [createDefault].
 */
object ThreadPool {
    private const val TAG = "ThreadPool"

    private var executor: ExecutorService? = null

    private val mainHandler: Handler by lazy {
        Handler(Looper.getMainLooper())
    }

    /**
     * run task on main thread
     */
    fun runInMain(runnable: Runnable) {
        mainHandler.post(runnable)
    }

    /**
     * run task on background thread
     */
    fun runInBackGround(runnable: Runnable) {
        ensureExecutorNotNull().submit(runnable)
    }

    /**
     * config custom executor by user
     * used by runInBackGround
     * @see runInBackGround
     */
    fun configExecutorService(outerExecutors: ExecutorService) {
        executor = outerExecutors
    }

    fun getExecutorService(): ExecutorService = ensureExecutorNotNull()

    private fun ensureExecutorNotNull(): ExecutorService {
        val current = executor
        if (current != null) {
            return current
        }
        val created = CommonDependsProvider.hostThreadPoolExecutorDepend?.getNormalThreadExecutor()
            ?: createDefault()
        executor = created
        return created
    }

    private fun createDefault(): ExecutorService {
        val cupNumber = Runtime.getRuntime().availableProcessors()
        var threadPoolSize = cupNumber / 2
        if (threadPoolSize == 0) {
            threadPoolSize = 1
        }
        Log.d(TAG, "jsb thread pool size: $threadPoolSize")
        return Executors.newFixedThreadPool(threadPoolSize)
    }
}
