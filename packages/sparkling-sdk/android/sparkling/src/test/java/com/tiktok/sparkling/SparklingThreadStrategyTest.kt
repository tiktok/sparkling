// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import com.lynx.tasm.ThreadStrategyForRendering
import org.junit.Assert.assertEquals
import org.junit.Test

class SparklingThreadStrategyTest {
    @Test
    fun allStrategiesMapToTheirLynxCounterparts() {
        val expected =
            mapOf(
                SparklingThreadStrategy.ALL_ON_UI to ThreadStrategyForRendering.ALL_ON_UI,
                SparklingThreadStrategy.MOST_ON_TASM to ThreadStrategyForRendering.MOST_ON_TASM,
                SparklingThreadStrategy.PART_ON_LAYOUT to ThreadStrategyForRendering.PART_ON_LAYOUT,
                SparklingThreadStrategy.MULTI_THREADS to ThreadStrategyForRendering.MULTI_THREADS,
            )

        assertEquals(expected.keys, SparklingThreadStrategy.values().toSet())
        expected.forEach { (strategy, lynxStrategy) ->
            assertEquals(lynxStrategy, strategy.toLynxThreadStrategy())
        }
    }
}
