// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.lynx

import com.lynx.tasm.LynxViewBuilder
import com.lynx.tasm.ThreadStrategyForRendering
import com.tiktok.sparkling.SparklingThreadStrategy
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class HybridLynxKitThreadStrategyTest {
    @Test
    fun pageStrategyOverridesGlobalDefault() {
        val builder = LynxViewBuilder()

        HybridLynxKit.applyThreadStrategy(
            builder,
            SparklingThreadStrategy.MULTI_THREADS,
        )

        assertEquals(ThreadStrategyForRendering.MULTI_THREADS, builder.threadStrategy)
    }

    @Test
    fun globalDefaultAppliesWhenPageStrategyIsUnset() {
        val builder = LynxViewBuilder()

        HybridLynxKit.applyThreadStrategy(
            builder,
            SparklingThreadStrategy.MOST_ON_TASM,
        )

        assertEquals(ThreadStrategyForRendering.MOST_ON_TASM, builder.threadStrategy)
    }

    @Test
    fun lynxDefaultRemainsUntouchedWhenBothStrategiesAreUnset() {
        val builder = mockk<LynxViewBuilder>(relaxed = true)

        HybridLynxKit.applyThreadStrategy(builder, null)

        verify(exactly = 0) {
            builder.setThreadStrategyForRendering(any())
        }
    }

    @Test
    fun pageStrategyOverridesGlobalDefaultDuringResolution() {
        assertEquals(
            SparklingThreadStrategy.PART_ON_LAYOUT,
            HybridLynxKit.resolveThreadStrategy(
                SparklingThreadStrategy.PART_ON_LAYOUT,
                SparklingThreadStrategy.MULTI_THREADS,
            ),
        )
    }
}
