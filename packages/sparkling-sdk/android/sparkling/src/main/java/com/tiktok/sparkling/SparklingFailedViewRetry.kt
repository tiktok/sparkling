// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

/**
 * A single-use request to retry the current failed load through Sparkling.
 *
 * Returns `true` only when this request belongs to the current failed load and
 * Sparkling accepts it. A request returns `false` after it has been used, after
 * a newer load or failure supersedes it, after the container is released, or
 * when called off the Android main thread.
 */
fun interface SparklingFailedViewRetry {
    fun retry(): Boolean
}

/**
 * Optional capability for a custom error view returned by [SparklingUIProvider].
 *
 * Sparkling supplies a new retry request for each current load failure and
 * clears it with `null` when it is no longer valid. Implementations should
 * replace any previously registered click listener or retry request.
 */
interface SparklingRetryableErrorView {
    fun setSparklingRetry(retry: SparklingFailedViewRetry?)
}
