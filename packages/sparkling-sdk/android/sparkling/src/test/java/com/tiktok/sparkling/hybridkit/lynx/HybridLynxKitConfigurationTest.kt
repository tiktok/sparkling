// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.lynx

import android.app.Application
import android.content.Context
import android.view.Display
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import com.lynx.tasm.LynxView
import com.lynx.tasm.ThreadStrategyForRendering
import com.tiktok.sparkling.SparklingContext
import com.tiktok.sparkling.SparklingLynxConfigurationError
import com.tiktok.sparkling.SparklingLynxConfigurationException
import com.tiktok.sparkling.SparklingLynxViewport
import com.tiktok.sparkling.SparklingThreadStrategy
import com.tiktok.sparkling.hybridkit.HybridCommon
import com.tiktok.sparkling.hybridkit.HybridEnvironment
import com.tiktok.sparkling.hybridkit.HybridKit
import com.tiktok.sparkling.hybridkit.base.HybridKitType
import com.tiktok.sparkling.hybridkit.config.BaseInfoConfig
import com.tiktok.sparkling.hybridkit.config.SparklingHybridConfig
import com.tiktok.sparkling.hybridkit.config.SparklingLynxConfig
import com.tiktok.sparkling.hybridkit.scheme.HybridSchemeParam
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HybridLynxKitConfigurationTest {
    private lateinit var application: Application
    private lateinit var context: Context

    @Before
    fun setUp() {
        clearAllMocks()
        application = mockk(relaxed = true)
        context = mockk(relaxed = true)

        val windowManager = mockk<WindowManager>(relaxed = true)
        val display = mockk<Display>(relaxed = true)
        every { display.rotation } returns 0
        every { windowManager.defaultDisplay } returns display
        every { context.getSystemService(Context.WINDOW_SERVICE) } returns windowManager
        every { application.getSystemService(Context.WINDOW_SERVICE) } returns windowManager

        val accessibilityManager = mockk<AccessibilityManager>(relaxed = true)
        every { context.getSystemService(Context.ACCESSIBILITY_SERVICE) } returns accessibilityManager
        every { application.getSystemService(Context.ACCESSIBILITY_SERVICE) } returns accessibilityManager

        HybridEnvironment.instance.context = application
        installGlobalThreadStrategy(null)
    }

    @Test
    fun globalMultiThreadsRejectsSchemeViewport() {
        installGlobalThreadStrategy(SparklingThreadStrategy.MULTI_THREADS)
        val scheme = scheme(viewport = VIEWPORT)
        val sparklingContext =
            SparklingContext().apply {
                hybridSchemeParam = scheme
            }

        assertUnsafeConfiguration {
            HybridKit.createKitView(scheme, sparklingContext, context)
        }
    }

    @Test
    fun pageMultiThreadsRejectsContextViewportRegardlessOfSetterOrder() {
        listOf(
            { sparklingContext: SparklingContext ->
                sparklingContext.lynxViewport = VIEWPORT
                sparklingContext.threadStrategy = SparklingThreadStrategy.MULTI_THREADS
            },
            { sparklingContext: SparklingContext ->
                sparklingContext.threadStrategy = SparklingThreadStrategy.MULTI_THREADS
                sparklingContext.lynxViewport = VIEWPORT
            },
        ).forEach { configure ->
            val scheme = scheme()
            val sparklingContext =
                SparklingContext().apply {
                    hybridSchemeParam = scheme
                }
            configure(sparklingContext)

            assertUnsafeConfiguration {
                HybridKit.createKitView(scheme, sparklingContext, context)
            }
        }
    }

    @Test
    fun globalConfigurationOrderDoesNotAffectPageViewportValidation() {
        listOf(
            true,
            false,
        ).forEach { installGlobalFirst ->
            installGlobalThreadStrategy(null)
            val scheme = scheme()
            val sparklingContext =
                SparklingContext().apply {
                    hybridSchemeParam = scheme
                }

            if (installGlobalFirst) {
                installGlobalThreadStrategy(SparklingThreadStrategy.MULTI_THREADS)
                sparklingContext.lynxViewport = VIEWPORT
            } else {
                sparklingContext.lynxViewport = VIEWPORT
                installGlobalThreadStrategy(SparklingThreadStrategy.MULTI_THREADS)
            }

            assertUnsafeConfiguration {
                HybridKit.createKitView(scheme, sparklingContext, context)
            }
        }
    }

    @Test
    fun globalMultiThreadsRejectsInitParamsViewport() {
        installGlobalThreadStrategy(SparklingThreadStrategy.MULTI_THREADS)
        val scheme = scheme()
        val sparklingContext =
            SparklingContext().apply {
                hybridSchemeParam = scheme
                lynxViewport = SparklingLynxViewport(100, 150)
                hybridParams =
                    LynxKitInitParams(loadUri = null).apply {
                        lynxViewport = VIEWPORT
                    }
            }

        assertUnsafeConfiguration {
            HybridKit.createKitView(scheme, sparklingContext, context)
        }
    }

    @Test
    fun safePageStrategyOverridesUnsafeGlobalDefault() {
        installGlobalThreadStrategy(SparklingThreadStrategy.MULTI_THREADS)
        val scheme = scheme(viewport = VIEWPORT)
        val sparklingContext =
            SparklingContext().apply {
                hybridSchemeParam = scheme
                threadStrategy = SparklingThreadStrategy.PART_ON_LAYOUT
            }

        val result = HybridKit.createKitView(scheme, sparklingContext, context)

        assertNotNull(result)
        assertEquals(
            ThreadStrategyForRendering.PART_ON_LAYOUT,
            (result as LynxView).threadStrategyForRendering,
        )
    }

    @Test
    fun unsafePageStrategyOverridesSafeGlobalDefaultAndIsRejected() {
        installGlobalThreadStrategy(SparklingThreadStrategy.PART_ON_LAYOUT)
        val scheme = scheme(viewport = VIEWPORT)
        val sparklingContext =
            SparklingContext().apply {
                hybridSchemeParam = scheme
                threadStrategy = SparklingThreadStrategy.MULTI_THREADS
            }

        assertUnsafeConfiguration {
            HybridKit.createKitView(scheme, sparklingContext, context)
        }
    }

    @Test
    fun fullPageNavigationRejectsUnsafeResolvedConfigurationBeforeLaunch() {
        installGlobalThreadStrategy(SparklingThreadStrategy.MULTI_THREADS)
        val sparklingContext =
            SparklingContext().apply {
                scheme = "hybrid://lynxview_page?bundle=main.lynx.bundle&width=320&height=480"
            }

        assertUnsafeConfiguration {
            com.tiktok.sparkling.Sparkling
                .build(context, sparklingContext)
                .navigate()
        }
    }

    @Test
    fun fixedViewportRemainsSupportedWithEverySafeResolvedStrategy() {
        listOf(
            null,
            SparklingThreadStrategy.ALL_ON_UI,
            SparklingThreadStrategy.MOST_ON_TASM,
            SparklingThreadStrategy.PART_ON_LAYOUT,
        ).forEach { strategy ->
            installGlobalThreadStrategy(strategy)
            val scheme = scheme(viewport = VIEWPORT)
            val sparklingContext =
                SparklingContext().apply {
                    hybridSchemeParam = scheme
                }

            assertNotNull(HybridKit.createKitView(scheme, sparklingContext, context))
        }
    }

    @Test
    fun multiThreadsRemainsSupportedWithoutFixedViewport() {
        installGlobalThreadStrategy(SparklingThreadStrategy.MULTI_THREADS)
        val scheme = scheme()
        val sparklingContext =
            SparklingContext().apply {
                hybridSchemeParam = scheme
            }

        val result = HybridKit.createKitView(scheme, sparklingContext, context)

        assertNotNull(result)
        assertEquals(
            ThreadStrategyForRendering.MULTI_THREADS,
            (result as LynxView).threadStrategyForRendering,
        )
    }

    private fun assertUnsafeConfiguration(block: () -> Unit) {
        val exception =
            assertThrows(SparklingLynxConfigurationException::class.java) {
                block()
            }
        assertEquals(
            SparklingLynxConfigurationError.FIXED_VIEWPORT_WITH_MULTI_THREADS,
            exception.error,
        )
    }

    private fun installGlobalThreadStrategy(threadStrategy: SparklingThreadStrategy?) {
        val lynxConfig =
            SparklingLynxConfig.build(application) {
                setDefaultThreadStrategy(threadStrategy)
            }
        HybridCommon.setHybridConfig(
            SparklingHybridConfig.build(BaseInfoConfig(isDebug = false)) {
                setLynxConfig(lynxConfig)
            },
            application,
        )
    }

    private fun scheme(viewport: SparklingLynxViewport? = null): HybridSchemeParam =
        HybridSchemeParam(
            engineType = HybridKitType.LYNX,
            bundle = "https://example.com/main.lynx.bundle",
        ).apply {
            lynxViewport = viewport
        }

    private companion object {
        val VIEWPORT = SparklingLynxViewport(320, 480)
    }
}
