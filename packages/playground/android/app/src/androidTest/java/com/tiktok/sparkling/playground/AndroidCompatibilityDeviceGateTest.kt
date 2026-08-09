// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.playground

import android.app.Activity
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lynx.tasm.LynxView
import com.lynx.tasm.ThreadStrategyForRendering
import com.lynx.tasm.resourceprovider.LynxResourceCallback
import com.lynx.tasm.resourceprovider.LynxResourceRequest
import com.lynx.tasm.resourceprovider.LynxResourceResponse
import com.lynx.tasm.resourceprovider.template.LynxTemplateResourceFetcher
import com.lynx.tasm.resourceprovider.template.TemplateProviderResult
import com.tiktok.sparkling.Sparkling
import com.tiktok.sparkling.SparklingActivity
import com.tiktok.sparkling.SparklingContext
import com.tiktok.sparkling.SparklingContextTransferStation
import com.tiktok.sparkling.SparklingLifecycleDelegate
import com.tiktok.sparkling.SparklingLynxConfigurationError
import com.tiktok.sparkling.SparklingLynxConfigurationException
import com.tiktok.sparkling.SparklingLynxViewCreatedListener
import com.tiktok.sparkling.SparklingResourceFetcherConfig
import com.tiktok.sparkling.SparklingResourceFetcherFactory
import com.tiktok.sparkling.SparklingThreadStrategy
import com.tiktok.sparkling.SparklingView
import com.tiktok.sparkling.hybridkit.HybridCommon
import com.tiktok.sparkling.hybridkit.HybridKit
import com.tiktok.sparkling.hybridkit.base.HybridKitError
import com.tiktok.sparkling.hybridkit.base.IKitView
import com.tiktok.sparkling.hybridkit.config.SparklingHybridConfig
import com.tiktok.sparkling.hybridkit.config.SparklingLynxConfig
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidCompatibilityDeviceGateTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val targetContext = instrumentation.targetContext
    private val application = targetContext.applicationContext as SparklingApplication
    private lateinit var originalConfig: SparklingHybridConfig
    private var activity: Activity? = null
    private var sparklingView: SparklingView? = null

    @Before
    fun setUp() {
        originalConfig = requireNotNull(HybridCommon.hybridConfig)
    }

    @After
    fun tearDown() {
        instrumentation.runOnMainSync {
            sparklingView?.release()
            sparklingView = null
            activity?.finish()
            activity = null
        }
        HybridKit.setHybridConfig(originalConfig, application)
        instrumentation.waitForIdleSync()
    }

    @Test
    fun fixedViewportUsesSharedDensityDefaultThreadAndPageFetcher() {
        val bundle =
            targetContext.assets.open(BUNDLE_NAME).use {
                it.readBytes()
            }
        val fetchCount = AtomicInteger()
        val factoryCount = AtomicInteger()
        val requestedUrl = AtomicReference<String>()
        val listenerBridgeReady = AtomicBoolean()
        val listenerBeforeFetch = AtomicBoolean()
        installConfig(
            density = SHARED_DENSITY,
            defaultThreadStrategy = SparklingThreadStrategy.PART_ON_LAYOUT,
            resourceFetcherFactory =
                SparklingResourceFetcherFactory {
                    factoryCount.incrementAndGet()
                    SparklingResourceFetcherConfig
                        .builder()
                        .setTemplateResourceFetcher(
                            successfulTemplateFetcher(bundle, fetchCount, requestedUrl),
                        ).build()
                },
        )

        val firstScreen = CountDownLatch(1)
        val loadFinish = CountDownLatch(1)
        val loadFailure = AtomicReference<HybridKitError?>()
        val createdLynxView = AtomicReference<LynxView>()
        lateinit var context: SparklingContext
        context =
            SparklingContext().apply {
                scheme =
                    "hybrid://lynxview_page?url=$BUNDLE_URL" +
                    "&width=$VIEWPORT_WIDTH_PX&height=$VIEWPORT_HEIGHT_PX"
                lynxViewCreatedListener =
                    SparklingLynxViewCreatedListener {
                        listenerBridgeReady.set(context.bridge != null)
                        listenerBeforeFetch.set(fetchCount.get() == 0)
                        createdLynxView.set(it)
                    }
                lifecycleDelegate =
                    object : SparklingLifecycleDelegate {
                        override fun onFirstScreen(view: IKitView) {
                            firstScreen.countDown()
                        }

                        override fun onLoadFinish(view: IKitView) {
                            loadFinish.countDown()
                        }

                        override fun onLoadFailed(
                            view: IKitView,
                            url: String,
                            error: HybridKitError,
                        ) {
                            loadFailure.set(error)
                            while (firstScreen.count > 0) {
                                firstScreen.countDown()
                            }
                            while (loadFinish.count > 0) {
                                loadFinish.countDown()
                            }
                        }
                    }
            }

        val host = launchHostActivity()
        instrumentation.runOnMainSync {
            val sparkling = Sparkling.build(host, context)
            sparkling.processSparklingContext(context)
            sparklingView = requireNotNull(sparkling.createView())
            val root = FrameLayout(host)
            root.addView(
                sparklingView,
                FrameLayout.LayoutParams(HOST_WIDTH_PX, HOST_HEIGHT_PX),
            )
            host.setContentView(root)
            root.measure(
                View.MeasureSpec.makeMeasureSpec(HOST_WIDTH_PX, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(HOST_HEIGHT_PX, View.MeasureSpec.EXACTLY),
            )
            root.layout(0, 0, HOST_WIDTH_PX, HOST_HEIGHT_PX)
            sparklingView?.loadUrl()
        }

        assertTrue("Timed out waiting for first-screen", firstScreen.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
        assertTrue("Timed out waiting for load-finish", loadFinish.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
        assertNull(loadFailure.get()?.errorReason, loadFailure.get())
        assertEquals(1, factoryCount.get())
        assertEquals(1, fetchCount.get())
        assertEquals(BUNDLE_URL, requestedUrl.get())
        assertTrue("listener ran before bridge initialization", listenerBridgeReady.get())
        assertTrue("listener ran after template fetch started", listenerBeforeFetch.get())
        instrumentation.runOnMainSync {
            val lynxView = requireNotNull(createdLynxView.get())
            assertEquals(VIEWPORT_WIDTH_PX, lynxView.measuredWidth)
            assertEquals(VIEWPORT_HEIGHT_PX, lynxView.measuredHeight)
            assertEquals(
                ThreadStrategyForRendering.PART_ON_LAYOUT,
                lynxView.threadStrategyForRendering,
            )
            assertEquals(
                SHARED_DENSITY,
                lynxView.lynxContext.screenMetrics.density,
                0.0001f,
            )
            assertTrue(requireNotNull(sparklingView).isLoadSuccess())

            val secondCreatedLynxView = AtomicReference<LynxView>()
            val secondContext =
                SparklingContext().apply {
                    scheme = "hybrid://lynxview_page?bundle=$BUNDLE_NAME"
                    lynxViewCreatedListener =
                        SparklingLynxViewCreatedListener {
                            secondCreatedLynxView.set(it)
                        }
                }
            val secondSparkling = Sparkling.build(host, secondContext)
            secondSparkling.processSparklingContext(secondContext)
            val secondSparklingView = requireNotNull(secondSparkling.createView())
            val secondLynxView = requireNotNull(secondCreatedLynxView.get())
            assertNotSame(lynxView, secondLynxView)
            assertEquals(
                SHARED_DENSITY,
                secondLynxView.lynxContext.screenMetrics.density,
                0.0001f,
            )
            secondSparklingView.release()
        }
        println(
            "COMPAT_GATE event=pass viewport=${VIEWPORT_WIDTH_PX}x$VIEWPORT_HEIGHT_PX " +
                "density=$SHARED_DENSITY density_views=2 " +
                "thread=PART_ON_LAYOUT fetches=${fetchCount.get()} factory_calls=${factoryCount.get()} " +
                "listener_bridge_ready=${listenerBridgeReady.get()} listener_before_fetch=${listenerBeforeFetch.get()}",
        )
    }

    @Test
    fun fixedViewportRejectsMultiThreadsBeforeViewCreation() {
        installConfig(
            density = null,
            defaultThreadStrategy = SparklingThreadStrategy.MULTI_THREADS,
        )
        val createdLynxView = AtomicReference<LynxView?>()
        val context =
            SparklingContext().apply {
                scheme =
                    "hybrid://lynxview_page?bundle=$BUNDLE_NAME" +
                    "&width=$VIEWPORT_WIDTH_PX&height=$VIEWPORT_HEIGHT_PX"
                lynxViewCreatedListener =
                    SparklingLynxViewCreatedListener {
                        createdLynxView.set(it)
                    }
            }

        val exception =
            runCatching {
                val sparkling = Sparkling.build(targetContext, context)
                sparkling.processSparklingContext(context)
                sparkling.createView()
            }.exceptionOrNull()

        assertTrue(exception is SparklingLynxConfigurationException)
        assertEquals(
            SparklingLynxConfigurationError.FIXED_VIEWPORT_WITH_MULTI_THREADS,
            (exception as SparklingLynxConfigurationException).error,
        )
        assertNull(createdLynxView.get())

        val fullPageContext =
            SparklingContext().apply {
                scheme =
                    "hybrid://lynxview_page?bundle=$BUNDLE_NAME" +
                    "&width=$VIEWPORT_WIDTH_PX&height=$VIEWPORT_HEIGHT_PX"
            }
        val monitor = instrumentation.addMonitor(SparklingActivity::class.java.name, null, false)
        val navigateException =
            runCatching {
                Sparkling.build(targetContext, fullPageContext).navigate()
            }.exceptionOrNull()
        val launchedActivity = instrumentation.waitForMonitorWithTimeout(monitor, 500)
        instrumentation.removeMonitor(monitor)

        assertTrue(navigateException is SparklingLynxConfigurationException)
        assertEquals(
            SparklingLynxConfigurationError.FIXED_VIEWPORT_WITH_MULTI_THREADS,
            (navigateException as SparklingLynxConfigurationException).error,
        )
        assertNull(launchedActivity)
        assertNull(
            SparklingContextTransferStation.getSparklingContext(fullPageContext.containerId),
        )
        println(
            "COMPAT_GATE event=unsafe_rejected error=${exception.error} " +
                "full_page_activity_started=${launchedActivity != null} transfer_saved=" +
                "${SparklingContextTransferStation.getSparklingContext(fullPageContext.containerId) != null}",
        )
    }

    @Test
    fun pageThreadOverridesRenderRealBundles() {
        val bundle =
            targetContext.assets.open(BUNDLE_NAME).use {
                it.readBytes()
            }
        installConfig(
            density = null,
            defaultThreadStrategy = SparklingThreadStrategy.PART_ON_LAYOUT,
        )
        val host = launchHostActivity()
        val strategies =
            listOf(
                SparklingThreadStrategy.ALL_ON_UI to ThreadStrategyForRendering.ALL_ON_UI,
                SparklingThreadStrategy.MOST_ON_TASM to ThreadStrategyForRendering.MOST_ON_TASM,
                SparklingThreadStrategy.MULTI_THREADS to ThreadStrategyForRendering.MULTI_THREADS,
            )

        strategies.forEachIndexed { index, (sparklingStrategy, lynxStrategy) ->
            val firstScreen = CountDownLatch(1)
            val loadFinish = CountDownLatch(1)
            val loadFailure = AtomicReference<HybridKitError?>()
            val createdLynxView = AtomicReference<LynxView>()
            val fetchCount = AtomicInteger()
            val context =
                SparklingContext().apply {
                    scheme = "hybrid://lynxview_page?url=$BUNDLE_URL?strategy=$index"
                    threadStrategy = sparklingStrategy
                    resourceFetcherConfig =
                        SparklingResourceFetcherConfig
                            .builder()
                            .setTemplateResourceFetcher(
                                successfulTemplateFetcher(
                                    bundle,
                                    fetchCount,
                                    AtomicReference(),
                                ),
                            ).build()
                    lynxViewCreatedListener =
                        SparklingLynxViewCreatedListener {
                            createdLynxView.set(it)
                        }
                    lifecycleDelegate =
                        object : SparklingLifecycleDelegate {
                            override fun onFirstScreen(view: IKitView) {
                                firstScreen.countDown()
                            }

                            override fun onLoadFinish(view: IKitView) {
                                loadFinish.countDown()
                            }

                            override fun onLoadFailed(
                                view: IKitView,
                                url: String,
                                error: HybridKitError,
                            ) {
                                loadFailure.set(error)
                                while (firstScreen.count > 0) {
                                    firstScreen.countDown()
                                }
                                while (loadFinish.count > 0) {
                                    loadFinish.countDown()
                                }
                            }
                        }
                }
            lateinit var currentView: SparklingView
            instrumentation.runOnMainSync {
                val sparkling = Sparkling.build(host, context)
                sparkling.processSparklingContext(context)
                currentView = requireNotNull(sparkling.createView())
                val root = FrameLayout(host)
                root.addView(
                    currentView,
                    FrameLayout.LayoutParams(HOST_WIDTH_PX, HOST_HEIGHT_PX),
                )
                host.setContentView(root)
                root.measure(
                    View.MeasureSpec.makeMeasureSpec(HOST_WIDTH_PX, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(HOST_HEIGHT_PX, View.MeasureSpec.EXACTLY),
                )
                root.layout(0, 0, HOST_WIDTH_PX, HOST_HEIGHT_PX)
                currentView.loadUrl()
            }

            assertTrue(
                "$sparklingStrategy first-screen timed out",
                firstScreen.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
            )
            assertTrue(
                "$sparklingStrategy load-finish timed out",
                loadFinish.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
            )
            assertNull(loadFailure.get()?.errorReason, loadFailure.get())
            assertEquals(1, fetchCount.get())
            assertEquals(lynxStrategy, createdLynxView.get().threadStrategyForRendering)
            assertTrue(currentView.isLoadSuccess())
            instrumentation.runOnMainSync {
                currentView.release()
            }
        }

        assertFalse(strategies.isEmpty())
        println(
            "COMPAT_GATE event=thread_matrix strategies=" +
                strategies.joinToString(",") { it.first.name },
        )
    }

    private fun launchHostActivity(): DeviceValidationActivity {
        val intent =
            Intent(targetContext, DeviceValidationActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        return (instrumentation.startActivitySync(intent) as DeviceValidationActivity).also {
            activity = it
            instrumentation.waitForIdleSync()
            SystemClock.sleep(250)
        }
    }

    private fun installConfig(
        density: Float?,
        defaultThreadStrategy: SparklingThreadStrategy?,
        resourceFetcherFactory: SparklingResourceFetcherFactory? = null,
    ) {
        val originalLynxConfig = requireNotNull(originalConfig.lynxConfig as SparklingLynxConfig)
        val lynxConfig =
            SparklingLynxConfig.build(application) {
                setCheckPropsSetter(originalLynxConfig.isCheckPropsSetter)
                setLibraryLoader(originalLynxConfig.libraryLoader)
                setTemplateProvider(originalLynxConfig.templateProvider)
                setResourceFetcherFactory(
                    resourceFetcherFactory ?: originalLynxConfig.resourceFetcherFactory,
                )
                addBehaviors(originalLynxConfig.globalBehaviors)
                addLynxModules(originalLynxConfig.globalModules)
                setAdditionInit(originalLynxConfig.additionInit)
                density?.let(::setSharedProcessDensityOverride)
                setDefaultThreadStrategy(defaultThreadStrategy)
            }
        val config =
            SparklingHybridConfig.build(originalConfig.baseInfoConfig) {
                setLynxConfig(lynxConfig)
                setWebConfig(originalConfig.webConfig)
                setBridgeConfig(originalConfig.bridgeConfig)
                setLogConfig(originalConfig.logConfig)
                originalConfig.debugConfig?.let(::setDebugConfig)
                setDefaultScreenOrientationPolicy(originalConfig.defaultScreenOrientationPolicy)
            }
        HybridKit.setHybridConfig(config, application)
    }

    private fun successfulTemplateFetcher(
        bundle: ByteArray,
        fetchCount: AtomicInteger,
        requestedUrl: AtomicReference<String>,
    ): LynxTemplateResourceFetcher =
        object : LynxTemplateResourceFetcher() {
            override fun fetchTemplate(
                request: LynxResourceRequest,
                callback: LynxResourceCallback<TemplateProviderResult>,
            ) {
                fetchCount.incrementAndGet()
                requestedUrl.set(request.url)
                callback.onResponse(
                    LynxResourceResponse.onSuccess(
                        TemplateProviderResult.fromBinary(bundle),
                    ),
                )
            }

            override fun fetchSSRData(
                request: LynxResourceRequest,
                callback: LynxResourceCallback<ByteArray>,
            ) {
                callback.onResponse(LynxResourceResponse.onSuccess(bundle))
            }
        }

    private companion object {
        const val BUNDLE_NAME = "device-acceptance.lynx.bundle"
        const val BUNDLE_URL = "https://device.acceptance/main.lynx.bundle"
        const val VIEWPORT_WIDTH_PX = 320
        const val VIEWPORT_HEIGHT_PX = 480
        const val HOST_WIDTH_PX = 900
        const val HOST_HEIGHT_PX = 1600
        const val SHARED_DENSITY = 3.25f
        const val TIMEOUT_SECONDS = 20L
    }
}
