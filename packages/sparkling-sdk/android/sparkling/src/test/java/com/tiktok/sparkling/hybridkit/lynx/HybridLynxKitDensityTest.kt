// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.lynx

import android.app.Application
import com.lynx.tasm.LynxViewBuilder
import com.tiktok.sparkling.SparklingContext
import com.tiktok.sparkling.hybridkit.HybridContext
import com.tiktok.sparkling.hybridkit.config.SparklingLynxConfig
import com.tiktok.sparkling.hybridkit.scheme.HybridSchemeParam
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HybridLynxKitDensityTest {
    @Test
    fun defaultConfigLeavesEveryLynxViewBuilderDensityUnset() {
        val config = SparklingLynxConfig.Builder(mockk<Application>(relaxed = true)).build()

        val builders =
            listOf(
                HybridLynxKit.createLynxViewBuilder(config),
                HybridLynxKit.createLynxViewBuilder(config),
            )

        builders.forEach { builder ->
            assertNull(builder.density)
        }
    }

    @Test
    fun sharedProcessDensityOverrideIsAppliedToEveryLynxViewBuilder() {
        val config =
            SparklingLynxConfig.build(mockk<Application>(relaxed = true)) {
                setSharedProcessDensityOverride(3.25f)
            }

        val builders =
            listOf(
                HybridLynxKit.createLynxViewBuilder(config),
                HybridLynxKit.createLynxViewBuilder(config),
            )

        builders.forEach { builder ->
            assertEquals(3.25f, builder.density)
        }
    }

    @Test
    fun containerConfigurationDoesNotExposeDensityOverride() {
        val forbiddenTypes =
            listOf(
                SparklingContext::class.java,
                HybridContext::class.java,
                LynxKitInitParams::class.java,
                HybridSchemeParam::class.java,
            )

        forbiddenTypes.forEach { type ->
            val memberNames =
                (type.declaredFields.map { it.name } + type.declaredMethods.map { it.name })
                    .map(String::lowercase)

            assertFalse(
                "${type.simpleName} must not expose a per-container density API",
                memberNames.any { it.contains("density") },
            )
        }
    }
}
