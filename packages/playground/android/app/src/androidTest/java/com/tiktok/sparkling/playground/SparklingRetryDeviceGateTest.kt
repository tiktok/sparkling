// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.playground

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lynx.tasm.resourceprovider.LynxResourceCallback
import com.lynx.tasm.resourceprovider.LynxResourceRequest
import com.lynx.tasm.resourceprovider.LynxResourceResponse
import com.lynx.tasm.resourceprovider.template.LynxTemplateResourceFetcher
import com.lynx.tasm.resourceprovider.template.TemplateProviderResult
import com.tiktok.sparkling.Sparkling
import com.tiktok.sparkling.SparklingContext
import com.tiktok.sparkling.SparklingFailedViewRetry
import com.tiktok.sparkling.SparklingLifecycleDelegate
import com.tiktok.sparkling.SparklingResourceFetcherConfig
import com.tiktok.sparkling.SparklingRetryableErrorView
import com.tiktok.sparkling.SparklingUIProvider
import com.tiktok.sparkling.SparklingView
import com.tiktok.sparkling.hybridkit.base.HybridKitError
import com.tiktok.sparkling.hybridkit.base.IKitView
import com.tiktok.sparkling.hybridkit.base.IPerformanceView
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SparklingRetryDeviceGateTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val targetContext = instrumentation.targetContext
    private val activities = CopyOnWriteArrayList<Activity>()
    private val views = CopyOnWriteArrayList<SparklingView>()
    private val bundle by lazy {
        targetContext.assets.open(BUNDLE_NAME).use {
            it.readBytes()
        }
    }

    @After
    fun tearDown() {
        instrumentation.runOnMainSync {
            views.forEach {
                if (!it.hasRelease()) {
                    it.release()
                }
            }
            activities.forEach(Activity::finish)
        }
        instrumentation.waitForIdleSync()
    }

    @Test
    fun retryContractTransitionsFailFailSuccessAndInvalidatesCallbacks() {
        val activity = launchHostActivity()
        val fetcher = SequencedTemplateFetcher(bundle)
        val retryableErrorView = RecordingRetryableErrorView(targetContext)
        val load = launch(activity, fetcher, retryableErrorView)

        assertTrue("initial failure missing", waitUntil { load.failures.get() == 1 })
        assertTrue(
            "retry #1 missing",
            waitUntil { retryableErrorView.registrations.size == 1 },
        )
        val retry1 = retryableErrorView.registrations[0]
        assertEquals(IPerformanceView.LoadStatus.FAIL, load.view.loadStatus())
        saveScreenshot("retry-1-fail")

        assertTrue("retry #1 was not accepted", invokeRetry(retry1))
        assertTrue("second failure missing", waitUntil { load.failures.get() == 2 })
        assertTrue(
            "retry #2 missing",
            waitUntil { retryableErrorView.registrations.size == 2 },
        )
        val retry2 = retryableErrorView.registrations[1]
        assertNotSame(retry1, retry2)
        assertFalse("stale retry #1 must be rejected", invokeRetry(retry1))
        assertEquals(IPerformanceView.LoadStatus.FAIL, load.view.loadStatus())
        saveScreenshot("retry-2-fail")

        assertTrue("retry #2 was not accepted", invokeRetry(retry2))
        assertTrue("first screen missing", waitUntil { load.firstScreen.get() })
        assertTrue("load finish missing", waitUntil { load.loadFinish.get() })
        assertTrue(
            "success status missing",
            waitUntil {
                load.view.loadStatus() == IPerformanceView.LoadStatus.SUCCESS
            },
        )
        assertTrue(
            "retry callback not cleared",
            waitUntil { retryableErrorView.current.get() == null },
        )
        assertTrue(
            "error view remained visible",
            waitUntil { retryableErrorView.visibility == View.GONE },
        )
        assertEquals(3, fetcher.fetchCount.get())
        saveScreenshot("retry-success")
        release(load.view)

        val releaseFetcher = AlwaysFailingTemplateFetcher()
        val releaseErrorView = RecordingRetryableErrorView(targetContext)
        val releaseLoad = launch(activity, releaseFetcher, releaseErrorView)
        assertTrue(
            "release failure missing",
            waitUntil { releaseLoad.failures.get() == 1 },
        )
        assertTrue(
            "release retry missing",
            waitUntil { releaseErrorView.registrations.size == 1 },
        )
        val retryBeforeRelease = releaseErrorView.registrations.single()
        release(releaseLoad.view)
        assertTrue(releaseLoad.view.hasRelease())
        assertTrue(
            "release did not clear callback",
            waitUntil { releaseErrorView.current.get() == null },
        )
        assertFalse(
            "released callback must be rejected",
            invokeRetry(retryBeforeRelease),
        )

        val legacyErrorView =
            TextView(targetContext).apply {
                text = "Plain legacy error view"
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.DKGRAY)
            }
        val legacyLoad =
            launch(
                activity,
                AlwaysFailingTemplateFetcher(),
                legacyErrorView,
            )
        assertTrue(
            "legacy failure missing",
            waitUntil { legacyLoad.failures.get() == 1 },
        )
        assertEquals(IPerformanceView.LoadStatus.FAIL, legacyLoad.view.loadStatus())
        saveScreenshot("plain-legacy-error")
        release(legacyLoad.view)

        println(
            "RETRY_GATE event=pass fetches=${fetcher.fetchCount.get()} " +
                "failures=${load.failures.get()} status=${load.view.loadStatus()} " +
                "release_invalidated=true legacy=true",
        )
    }

    private fun launch(
        activity: DeviceValidationActivity,
        fetcher: LynxTemplateResourceFetcher,
        errorView: View,
    ): Load {
        val failures = AtomicInteger()
        val firstScreen = AtomicBoolean()
        val loadFinish = AtomicBoolean()
        val context =
            SparklingContext().apply {
                scheme = "hybrid://lynxview_page?url=$BUNDLE_URL"
                resourceFetcherConfig =
                    SparklingResourceFetcherConfig
                        .builder()
                        .setTemplateResourceFetcher(fetcher)
                        .build()
                sparklingUIProvider =
                    object : SparklingUIProvider {
                        override fun getLoadingView(context: Context): View = ProgressBar(context)

                        override fun getErrorView(context: Context): View = errorView

                        override fun getToolBar(context: Context): Toolbar? = null
                    }
                lifecycleDelegate =
                    object : SparklingLifecycleDelegate {
                        override fun onLoadFailed(
                            view: IKitView,
                            url: String,
                            error: HybridKitError,
                        ) {
                            failures.incrementAndGet()
                        }

                        override fun onFirstScreen(view: IKitView) {
                            firstScreen.set(true)
                        }

                        override fun onLoadFinish(view: IKitView) {
                            loadFinish.set(true)
                        }
                    }
            }
        lateinit var view: SparklingView
        instrumentation.runOnMainSync {
            val sparkling = Sparkling.build(activity, context)
            sparkling.processSparklingContext(context)
            view = requireNotNull(sparkling.createView())
            val host = FrameLayout(activity)
            host.addView(
                view,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
            activity.setContentView(host)
            views += view
            view.loadUrl()
        }
        return Load(view, failures, firstScreen, loadFinish)
    }

    private fun launchHostActivity(): DeviceValidationActivity {
        val intent =
            Intent(targetContext, DeviceValidationActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        return (instrumentation.startActivitySync(intent) as DeviceValidationActivity).also {
            activities += it
            instrumentation.waitForIdleSync()
        }
    }

    private fun release(view: SparklingView) {
        instrumentation.runOnMainSync {
            view.release()
        }
        instrumentation.waitForIdleSync()
    }

    private fun invokeRetry(retry: SparklingFailedViewRetry): Boolean {
        val accepted = AtomicBoolean()
        instrumentation.runOnMainSync {
            accepted.set(retry.retry())
        }
        instrumentation.waitForIdleSync()
        return accepted.get()
    }

    private fun waitUntil(condition: () -> Boolean): Boolean {
        val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) {
                return true
            }
            SystemClock.sleep(50)
        }
        return condition()
    }

    private fun saveScreenshot(name: String) {
        instrumentation.waitForIdleSync()
        val directory =
            requireNotNull(
                targetContext.getExternalFilesDir("retry-validation"),
            )
        directory.mkdirs()
        val output = directory.resolve("$name.png")
        output.outputStream().use {
            instrumentation.uiAutomation
                .takeScreenshot()
                .compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        println("RETRY_GATE artifact=${output.absolutePath}")
    }

    private data class Load(
        val view: SparklingView,
        val failures: AtomicInteger,
        val firstScreen: AtomicBoolean,
        val loadFinish: AtomicBoolean,
    )

    private inner class RecordingRetryableErrorView(
        context: Context,
    ) : TextView(context),
        SparklingRetryableErrorView {
        val current = AtomicReference<SparklingFailedViewRetry?>()
        val registrations = CopyOnWriteArrayList<SparklingFailedViewRetry>()

        init {
            text = "Retryable failure"
            gravity = Gravity.CENTER
            textSize = 28f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(180, 30, 60))
        }

        override fun setSparklingRetry(retry: SparklingFailedViewRetry?) {
            current.set(retry)
            retry?.let(registrations::add)
        }
    }

    private class SequencedTemplateFetcher(
        private val bundle: ByteArray,
    ) : LynxTemplateResourceFetcher() {
        val fetchCount = AtomicInteger()

        override fun fetchTemplate(
            request: LynxResourceRequest,
            callback: LynxResourceCallback<TemplateProviderResult>,
        ) {
            val attempt = fetchCount.incrementAndGet()
            if (attempt <= FAILURE_COUNT) {
                callback.onResponse(
                    failedResponse(IllegalStateException("missing attempt $attempt")),
                )
            } else {
                callback.onResponse(
                    LynxResourceResponse.onSuccess(
                        TemplateProviderResult.fromBinary(bundle),
                    ),
                )
            }
        }

        override fun fetchSSRData(
            request: LynxResourceRequest,
            callback: LynxResourceCallback<ByteArray>,
        ) {
            callback.onResponse(LynxResourceResponse.onSuccess(bundle))
        }
    }

    private class AlwaysFailingTemplateFetcher : LynxTemplateResourceFetcher() {
        override fun fetchTemplate(
            request: LynxResourceRequest,
            callback: LynxResourceCallback<TemplateProviderResult>,
        ) {
            callback.onResponse(
                failedResponse(IllegalStateException("missing")),
            )
        }

        override fun fetchSSRData(
            request: LynxResourceRequest,
            callback: LynxResourceCallback<ByteArray>,
        ) {
            callback.onResponse(
                failedResponse(IllegalStateException("missing")),
            )
        }
    }

    private companion object {
        const val BUNDLE_NAME = "device-acceptance.lynx.bundle"
        const val BUNDLE_URL = "https://device.acceptance/retry.lynx.bundle"
        const val FAILURE_COUNT = 2
        const val TIMEOUT_MS = 30_000L

        @Suppress("UNCHECKED_CAST")
        fun <T> failedResponse(error: Throwable): LynxResourceResponse<T> = LynxResourceResponse.onFailed(error) as LynxResourceResponse<T>
    }
}
