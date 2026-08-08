// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.playground

import android.view.View
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lynx.tasm.LynxView
import com.tiktok.sparkling.Sparkling
import com.tiktok.sparkling.SparklingContext
import com.tiktok.sparkling.SparklingLifecycleDelegate
import com.tiktok.sparkling.SparklingLynxViewCreatedListener
import com.tiktok.sparkling.SparklingLynxViewport
import com.tiktok.sparkling.SparklingView
import com.tiktok.sparkling.hybridkit.base.HybridKitError
import com.tiktok.sparkling.hybridkit.base.IKitView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class FixedLynxViewportInstrumentedTest {
    @Test
    fun canonicalSchemeCreatesExactPhysicalPixelLynxViewport() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val targetContext = instrumentation.targetContext
        val firstScreenSeen = AtomicBoolean(false)
        val loadFinishSeen = AtomicBoolean(false)
        val renderLatch = CountDownLatch(2)
        val loadFailure = AtomicReference<HybridKitError?>()
        val bundleAvailable =
            runCatching {
                targetContext.assets.open(BUNDLE_NAME).close()
                true
            }.getOrDefault(false)
        var createdLynxView: LynxView? = null
        var sparklingView: SparklingView? = null

        instrumentation.runOnMainSync {
            val context =
                SparklingContext().apply {
                    scheme =
                        "hybrid://lynxview_page?" +
                        "bundle=$BUNDLE_NAME&width=$VIEWPORT_WIDTH_PX&height=$VIEWPORT_HEIGHT_PX"
                    lynxViewCreatedListener =
                        SparklingLynxViewCreatedListener { lynxView ->
                            createdLynxView = lynxView
                        }
                    lifecycleDelegate =
                        object : SparklingLifecycleDelegate {
                            override fun onFirstScreen(view: IKitView) {
                                if (firstScreenSeen.compareAndSet(false, true)) {
                                    renderLatch.countDown()
                                }
                            }

                            override fun onLoadFinish(view: IKitView) {
                                if (loadFinishSeen.compareAndSet(false, true)) {
                                    renderLatch.countDown()
                                }
                            }

                            override fun onLoadFailed(
                                view: IKitView,
                                url: String,
                                error: HybridKitError,
                            ) {
                                loadFailure.set(error)
                                while (renderLatch.count > 0) {
                                    renderLatch.countDown()
                                }
                            }
                        }
                }
            val sparkling = Sparkling.build(targetContext, context)
            sparkling.processSparklingContext(context)
            assertEquals(
                SparklingLynxViewport(VIEWPORT_WIDTH_PX, VIEWPORT_HEIGHT_PX),
                context.hybridSchemeParam?.lynxViewport,
            )
            sparklingView = requireNotNull(sparkling.createView())
            val host = FrameLayout(targetContext)
            host.addView(
                sparklingView,
                FrameLayout.LayoutParams(HOST_WIDTH_PX, HOST_HEIGHT_PX),
            )

            host.measure(
                View.MeasureSpec.makeMeasureSpec(HOST_WIDTH_PX, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(HOST_HEIGHT_PX, View.MeasureSpec.EXACTLY),
            )
            host.layout(0, 0, HOST_WIDTH_PX, HOST_HEIGHT_PX)

            val lynxView = createdLynxView
            assertFixedViewport(sparklingView, lynxView)
            if (bundleAvailable) {
                sparklingView?.loadUrl()
            }
        }

        if (bundleAvailable) {
            assertTrue(
                "Timed out waiting for Lynx first-screen and load-finish callbacks",
                renderLatch.await(RENDER_TIMEOUT_SECONDS, TimeUnit.SECONDS),
            )
            assertNull(loadFailure.get()?.errorReason, loadFailure.get())
            assertTrue("Lynx first-screen callback was not received", firstScreenSeen.get())
            assertTrue("Lynx load-finish callback was not received", loadFinishSeen.get())
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                assertFixedViewport(sparklingView, createdLynxView)
                assertTrue(requireNotNull(sparklingView).isLoadSuccess())
            }
        }

        instrumentation.runOnMainSync {
            sparklingView?.release()
        }
    }

    private fun assertFixedViewport(
        sparklingView: SparklingView?,
        lynxView: LynxView?,
    ) {
        assertNotNull(lynxView)
        assertSame(sparklingView, lynxView?.parent)
        assertEquals(VIEWPORT_WIDTH_PX, lynxView?.layoutParams?.width)
        assertEquals(VIEWPORT_HEIGHT_PX, lynxView?.layoutParams?.height)
        assertEquals(VIEWPORT_WIDTH_PX, lynxView?.measuredWidth)
        assertEquals(VIEWPORT_HEIGHT_PX, lynxView?.measuredHeight)
    }

    private companion object {
        const val BUNDLE_NAME = "main.lynx.bundle"
        const val VIEWPORT_WIDTH_PX = 320
        const val VIEWPORT_HEIGHT_PX = 480
        const val HOST_WIDTH_PX = 900
        const val HOST_HEIGHT_PX = 1600
        const val RENDER_TIMEOUT_SECONDS = 15L
    }
}
