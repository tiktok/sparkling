// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.Toolbar
import com.tiktok.sparkling.hybridkit.HybridContext
import com.tiktok.sparkling.hybridkit.HybridKit
import com.tiktok.sparkling.hybridkit.base.IHybridKitLifeCycle
import com.tiktok.sparkling.hybridkit.base.IKitView
import com.tiktok.sparkling.hybridkit.base.IPerformanceView
import com.tiktok.sparkling.hybridkit.scheme.HybridSchemeParam
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    packageName = "com.tiktok.sparkling",
)
class SparklingFailedViewRetryTest {
    private lateinit var context: Context
    private lateinit var sparklingContext: SparklingContext

    @Before
    fun setUp() {
        clearAllMocks()
        mockkObject(HybridKit)
        context = RuntimeEnvironment.getApplication()
        sparklingContext =
            SparklingContext().apply {
                hybridSchemeParam = HybridSchemeParam()
            }
    }

    @After
    fun tearDown() {
        SparklingContextTransferStation.clearAllContexts()
        unmockkAll()
    }

    @Test
    fun failureRegistersRetryThatReloadsExactlyOnce() {
        val fixture = prepareRetryableView()

        fixture.lifecycle.onLoadFailed(fixture.kitView, TEST_URL, "network error")

        val retry = fixture.errorView.currentRetry
        assertTrue(retry != null)
        assertTrue(retry!!.retry())
        assertFalse(retry.retry())
        assertEquals(1, fixture.kitView.reloadCount)
        assertNull(fixture.errorView.currentRetry)
    }

    @Test
    fun multipleFailuresProvideOneRetryPerFailure() {
        val fixture = prepareRetryableView()

        fixture.lifecycle.onLoadFailed(fixture.kitView, TEST_URL, "first")
        val firstRetry = fixture.errorView.currentRetry!!
        assertTrue(firstRetry.retry())
        fixture.lifecycle.onLoadFailed(fixture.kitView, TEST_URL, "second")
        val secondRetry = fixture.errorView.currentRetry!!

        assertFalse(firstRetry.retry())
        assertTrue(secondRetry.retry())
        assertFalse(secondRetry.retry())
        assertEquals(2, fixture.kitView.reloadCount)
    }

    @Test
    fun retryFailureReturnsToFailAndRegistersNewRetry() {
        val fixture = prepareRetryableView()
        fixture.lifecycle.onLoadFailed(fixture.kitView, TEST_URL, "missing template")
        val firstRetry = fixture.errorView.currentRetry!!

        assertTrue(firstRetry.retry())
        assertEquals(IPerformanceView.LoadStatus.LOADING, fixture.sparklingView.loadStatus())
        fixture.lifecycle.onLoadFailed(fixture.kitView, TEST_URL, "still missing")

        assertEquals(IPerformanceView.LoadStatus.FAIL, fixture.sparklingView.loadStatus())
        val secondRetry = fixture.errorView.currentRetry
        assertTrue(secondRetry != null)
        assertFalse(firstRetry.retry())
        assertTrue(secondRetry!!.retry())
        assertEquals(2, fixture.kitView.reloadCount)
    }

    @Test
    fun retrySuccessTransitionsFromLoadingToSuccess() {
        val fixture = prepareRetryableView()
        fixture.lifecycle.onLoadFailed(fixture.kitView, TEST_URL, "missing template")

        assertTrue(fixture.errorView.currentRetry!!.retry())
        assertEquals(IPerformanceView.LoadStatus.LOADING, fixture.sparklingView.loadStatus())
        fixture.lifecycle.onLoadFinish(fixture.kitView)

        assertEquals(IPerformanceView.LoadStatus.SUCCESS, fixture.sparklingView.loadStatus())
        assertNull(fixture.errorView.currentRetry)
    }

    @Test
    fun newerFailureInvalidatesUnconsumedRetry() {
        val fixture = prepareRetryableView()

        fixture.lifecycle.onLoadFailed(fixture.kitView, TEST_URL, "first")
        val staleRetry = fixture.errorView.currentRetry!!
        fixture.lifecycle.onLoadFailed(fixture.kitView, TEST_URL, "second")
        val currentRetry = fixture.errorView.currentRetry!!

        assertFalse(staleRetry.retry())
        assertTrue(currentRetry.retry())
        assertEquals(1, fixture.kitView.reloadCount)
    }

    @Test
    fun releaseClearsAndInvalidatesRetry() {
        val fixture = prepareRetryableView()
        fixture.lifecycle.onLoadFailed(fixture.kitView, TEST_URL, "network error")
        val retry = fixture.errorView.currentRetry!!

        fixture.sparklingView.release()

        assertNull(fixture.errorView.currentRetry)
        assertFalse(retry.retry())
        assertEquals(0, fixture.kitView.reloadCount)
        assertEquals(1, fixture.kitView.destroyCount)
    }

    @Test
    fun retryOffMainThreadIsRejectedWithoutConsumingCurrentFailure() {
        val fixture = prepareRetryableView()
        fixture.lifecycle.onLoadFailed(fixture.kitView, TEST_URL, "network error")
        val retry = fixture.errorView.currentRetry!!
        val backgroundResult = AtomicBoolean(true)
        val thread =
            Thread {
                backgroundResult.set(retry.retry())
            }

        thread.start()
        thread.join()

        assertFalse(backgroundResult.get())
        assertEquals(0, fixture.kitView.reloadCount)
        assertTrue(retry.retry())
        assertEquals(1, fixture.kitView.reloadCount)
    }

    @Test
    fun staleKitViewFailureCannotReplaceCurrentContainerRetry() {
        val fixture = prepareRetryableView()
        fixture.lifecycle.onLoadFailed(fixture.kitView, TEST_URL, "current failure")
        val currentRetry = fixture.errorView.currentRetry!!
        val otherKitView = RecordingKitView(context)

        fixture.lifecycle.onLoadFailed(otherKitView, TEST_URL, "stale failure")

        assertSame(currentRetry, fixture.errorView.currentRetry)
        assertTrue(currentRetry.retry())
        assertEquals(1, fixture.kitView.reloadCount)
        assertEquals(0, otherKitView.reloadCount)
    }

    @Test
    fun retriesRemainIsolatedAcrossEmbeddedContainers() {
        val first = prepareRetryableView()
        val second = prepareRetryableView()
        first.lifecycle.onLoadFailed(first.kitView, TEST_URL, "first")
        second.lifecycle.onLoadFailed(second.kitView, TEST_URL, "second")

        assertTrue(first.errorView.currentRetry!!.retry())

        assertEquals(1, first.kitView.reloadCount)
        assertEquals(0, second.kitView.reloadCount)
        assertTrue(second.errorView.currentRetry!!.retry())
        assertEquals(1, second.kitView.reloadCount)
    }

    @Test
    fun fullPageFragmentReleaseInvalidatesRegisteredRetry() {
        val kitView = RecordingKitView(context)
        val errorView = RecordingRetryableErrorView(context)
        val lifecycleSlot = slot<IHybridKitLifeCycle>()
        every {
            HybridKit.createKitView(any(), any(), any(), capture(lifecycleSlot))
        } returns kitView
        val fullPageContext =
            SparklingContext().apply {
                containerId = "full-page-retry"
                hybridSchemeParam = HybridSchemeParam()
                sparklingUIProvider =
                    object : SparklingUIProvider {
                        override fun getLoadingView(context: Context): View = View(context)

                        override fun getErrorView(context: Context): View = errorView

                        override fun getToolBar(context: Context): Toolbar? = null
                    }
            }
        SparklingContextTransferStation.saveSparklingContext(fullPageContext)
        val intent =
            android.content.Intent(context, SparklingActivity::class.java).apply {
                putExtra(Sparkling.SPARKLING_CONTEXT_CONTAINER_ID, fullPageContext.containerId)
            }
        val controller =
            Robolectric
                .buildActivity(SparklingActivity::class.java, intent)
                .create()
                .start()
                .resume()
        controller.get().supportFragmentManager.executePendingTransactions()
        lifecycleSlot.captured.onLoadFailed(kitView, TEST_URL, "network error")
        val retry = errorView.currentRetry!!

        controller.pause().stop().destroy()

        assertNull(errorView.currentRetry)
        assertFalse(retry.retry())
        assertEquals(1, kitView.destroyCount)
    }

    @Test
    fun fragmentViewRecreationReleasesOldViewAndCreatesFreshRetryState() {
        val firstKitView = RecordingKitView(context)
        val secondKitView = RecordingKitView(context)
        val lifecycles = mutableListOf<IHybridKitLifeCycle>()
        every {
            HybridKit.createKitView(any(), any(), any(), any())
        } answers {
            lifecycles += arg<IHybridKitLifeCycle>(3)
            if (lifecycles.size == 1) firstKitView else secondKitView
        }
        val errorViews = mutableListOf<RecordingRetryableErrorView>()
        val retainedContext =
            SparklingContext().apply {
                containerId = "full-page-recreate"
                hybridSchemeParam = HybridSchemeParam()
                sparklingUIProvider =
                    object : SparklingUIProvider {
                        override fun getLoadingView(context: Context): View = View(context)

                        override fun getErrorView(context: Context): View = RecordingRetryableErrorView(context).also(errorViews::add)

                        override fun getToolBar(context: Context): Toolbar? = null
                    }
            }
        SparklingContextTransferStation.saveSparklingContext(retainedContext)
        val intent =
            android.content.Intent(context, SparklingActivity::class.java).apply {
                putExtra(Sparkling.SPARKLING_CONTEXT_CONTAINER_ID, retainedContext.containerId)
            }
        val controller =
            Robolectric
                .buildActivity(SparklingActivity::class.java, intent)
                .create()
                .start()
                .resume()
        val activity = controller.get()
        activity.supportFragmentManager.executePendingTransactions()
        val fragment =
            activity.supportFragmentManager.findFragmentById(R.id.main_view_container)
                as SparklingFragment
        lifecycles.single().onLoadFailed(firstKitView, TEST_URL, "first failure")
        val staleRetry = errorViews.single().currentRetry!!

        activity.supportFragmentManager
            .beginTransaction()
            .detach(fragment)
            .commitNow()

        assertEquals(1, firstKitView.destroyCount)
        assertNull(errorViews.first().currentRetry)
        assertFalse(staleRetry.retry())

        activity.supportFragmentManager
            .beginTransaction()
            .attach(fragment)
            .commitNow()
        lifecycles.last().onLoadFailed(secondKitView, TEST_URL, "second failure")

        assertEquals(2, errorViews.size)
        assertTrue(errorViews.last().currentRetry!!.retry())
        assertEquals(0, firstKitView.reloadCount)
        assertEquals(1, secondKitView.reloadCount)
        assertEquals(0, secondKitView.destroyCount)

        controller.pause().stop().destroy()

        assertEquals(1, firstKitView.destroyCount)
        assertEquals(1, secondKitView.destroyCount)
    }

    @Test
    fun legacyProviderWithPlainErrorViewRemainsCompatible() {
        val kitView = RecordingKitView(context)
        val lifecycleSlot = slot<IHybridKitLifeCycle>()
        every {
            HybridKit.createKitView(any(), any(), any(), capture(lifecycleSlot))
        } returns kitView
        sparklingContext.sparklingUIProvider =
            object : SparklingUIProvider {
                override fun getLoadingView(context: Context): View = View(context)

                override fun getErrorView(context: Context): View = View(context)

                override fun getToolBar(context: Context): Toolbar? = null
            }
        val sparklingView = SparklingView(context)
        sparklingView.prepare(sparklingContext)

        lifecycleSlot.captured.onLoadFailed(kitView, TEST_URL, "network error")

        assertEquals(0, kitView.reloadCount)
        assertEquals(IPerformanceView.LoadStatus.FAIL, sparklingView.loadStatus())
    }

    private fun prepareRetryableView(): RetryFixture {
        val kitView = RecordingKitView(context)
        val errorView = RecordingRetryableErrorView(context)
        val lifecycleSlot = slot<IHybridKitLifeCycle>()
        every {
            HybridKit.createKitView(any(), any(), any(), capture(lifecycleSlot))
        } returns kitView
        sparklingContext =
            SparklingContext().apply {
                hybridSchemeParam = HybridSchemeParam()
                sparklingUIProvider =
                    object : SparklingUIProvider {
                        override fun getLoadingView(context: Context): View = View(context)

                        override fun getErrorView(context: Context): View = errorView

                        override fun getToolBar(context: Context): Toolbar? = null
                    }
            }
        val sparklingView = SparklingView(context)
        sparklingView.prepare(sparklingContext)
        return RetryFixture(sparklingView, kitView, errorView, lifecycleSlot.captured)
    }

    private data class RetryFixture(
        val sparklingView: SparklingView,
        val kitView: RecordingKitView,
        val errorView: RecordingRetryableErrorView,
        val lifecycle: IHybridKitLifeCycle,
    )

    private class RecordingRetryableErrorView(
        context: Context,
    ) : FrameLayout(context),
        SparklingRetryableErrorView {
        val registrations = mutableListOf<SparklingFailedViewRetry?>()
        var currentRetry: SparklingFailedViewRetry? = null
            private set

        override fun setSparklingRetry(retry: SparklingFailedViewRetry?) {
            registrations += retry
            currentRetry = retry
        }
    }

    private class RecordingKitView(
        context: Context,
    ) : IKitView {
        override var hybridContext: HybridContext = HybridContext()
        private val view = View(context)
        var reloadCount = 0
        var destroyCount = 0

        override fun realView(): View = view

        override fun load() = Unit

        override fun load(uri: String) = Unit

        override fun reload() {
            reloadCount++
        }

        override fun updateGlobalPropsByIncrement(data: Map<String, Any>) = Unit

        override fun onShow() = Unit

        override fun onHide() = Unit

        override fun destroy(clearContext: Boolean) {
            destroyCount++
        }

        override fun hasDestroyed(): Boolean = destroyCount > 0

        override fun getGlobalProps(): MutableMap<String, Any>? = null

        override fun getScheme(): String? = null

        override fun onLoadSuccess() = Unit

        override fun sendEventByJSON(
            eventName: String,
            params: org.json.JSONObject?,
        ) = Unit
    }

    private companion object {
        const val TEST_URL = "https://example.com/main.lynx.bundle"
    }
}
