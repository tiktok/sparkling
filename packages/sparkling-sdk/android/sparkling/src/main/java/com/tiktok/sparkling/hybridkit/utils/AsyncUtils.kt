// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.utils

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Utility object to manage asynchronous tasks.
 */
object AsyncUtils {
    private val sequenceExecutor = Executors.newSingleThreadExecutor()
    private val blockedExecutor =
        ThreadPoolExecutor(10, 10, 3, TimeUnit.SECONDS, LinkedBlockingQueue()).apply { allowCoreThreadTimeOut(true) }

    /**
     * Submits a task to the appropriate executor based on the specified [TaskType].
     * @param type The type of task to execute.
     * @param ignoreException If true, exceptions during task submission will be ignored.
     *        If false, exceptions will be rethrown and must be handled by the caller.
     * @param task The runnable task to be submitted to an executor service.
     */
    fun submit(
        type: TaskType,
        ignoreException: Boolean = true,
        task: Runnable,
    ) {
        runCatching {
            getExecutor(type).submit(task)
        }.onFailure {
            if (!ignoreException) {
                throw it
            }
        }
    }

    /**
     * Retrieves the executor associated with a given [TaskType].
     * @param type The type of task associated with the executor to retrieve.
     * @return The executor service associated with the specified type.
     */
    fun getExecutor(type: TaskType): ExecutorService =
        when (type) {
            TaskType.Sequence -> sequenceExecutor
            TaskType.Blocked -> blockedExecutor
        }
}

/**
 * Defines types of tasks that can be submitted to the AsyncUtils executors.
 * Each task type is associated with its relevant executor service.
 */
enum class TaskType {
    /**
     * Represents tasks that should be executed sequentially, one after another.
     * Ideal for tasks that are not thread-safe and require sequential execution.
     * In addition, regular background tasks should also use this type.
     */
    Sequence,

    /**
     * Represents tasks that can be executed in parallel but might be I/O blocked.
     * This type of executor allows multiple tasks to run in parallel with a larger pool,
     * which is useful when tasks involve blocking I/O operations.
     */
    Blocked,
}
