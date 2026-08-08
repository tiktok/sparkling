// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import com.lynx.tasm.ThreadStrategyForRendering

/**
 * Controls how Lynx rendering work is distributed across threads.
 */
enum class SparklingThreadStrategy {
    ALL_ON_UI,
    MOST_ON_TASM,
    PART_ON_LAYOUT,
    MULTI_THREADS,
    ;

    internal fun toLynxThreadStrategy(): ThreadStrategyForRendering =
        when (this) {
            ALL_ON_UI -> ThreadStrategyForRendering.ALL_ON_UI
            MOST_ON_TASM -> ThreadStrategyForRendering.MOST_ON_TASM
            PART_ON_LAYOUT -> ThreadStrategyForRendering.PART_ON_LAYOUT
            MULTI_THREADS -> ThreadStrategyForRendering.MULTI_THREADS
        }
}
