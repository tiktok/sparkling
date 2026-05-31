// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Size
import android.view.View
import android.widget.FrameLayout
import com.tiktok.sparkling.hybridkit.HybridCommon
import com.tiktok.sparkling.hybridkit.HybridContext
import com.tiktok.sparkling.hybridkit.HybridKit
import com.tiktok.sparkling.hybridkit.base.HybridContainerType
import com.tiktok.sparkling.hybridkit.base.HybridKitError
import com.tiktok.sparkling.hybridkit.base.HybridKitType
import com.tiktok.sparkling.hybridkit.base.IHybridKitLifeCycle
import com.tiktok.sparkling.hybridkit.base.IKitView
import com.tiktok.sparkling.hybridkit.base.IPerformanceView
import com.tiktok.sparkling.hybridkit.scheme.HybridSchemeParam
import com.tiktok.sparkling.hybridkit.utils.ColorUtil
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    packageName = "com.tiktok.sparkling",
)
class SparklingViewTest {
    private lateinit var context: Context
    private lateinit var baseContext: SparklingContext

    @Before
    fun setUp() {
        clearAllMocks()
        context = RuntimeEnvironment.getApplication()
        mockkObject(HybridKit)

        baseContext =
            SparklingContext().apply {
                containerId = "test_container_id"
                hybridSchemeParam =
                    HybridSchemeParam().apply {
                        engineType = HybridKitType.LYNX
                        containerBgColor = "#123456"
                        loadingBgColor = "#abcdef"
                    }
            }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun prepareAttachesKitViewAndCustomUi() {
        val loadingView = View(context)
        val errorView = View(context)
        val kitView = RecordingKitView(context)
        every { HybridKit.createKitView(any(), any(), any(), any()) } returns kitView
        val sparklingView = SparklingView(context)
        baseContext.sparklingUIProvider =
            object : SparklingUIProvider {
                override fun getLoadingView(context: Context): View = loadingView

                override fun getErrorView(context: Context): View = errorView

                override fun getToolBar(context: Context) = null
            }

        sparklingView.prepare(baseContext)

        assertEquals(baseContext, sparklingView.sparklingContext)
        assertEquals(kitView, sparklingView.getKitView())
        val realView = kitView.realView()
        assertNotNull(realView)
        assertEquals(sparklingView, realView?.parent)
        val backgroundColor = (realView?.background as ColorDrawable).color
        assertEquals(ColorUtil.parseColorSafely("#123456"), backgroundColor)

        sparklingView.showLoadingView()
        assertEquals(View.VISIBLE, loadingView.visibility)
        assertEquals(sparklingView, loadingView.parent)
    }

    @Test
    fun loadUrlDelegatesToKitView() {
        val kitView = RecordingKitView(context)
        every { HybridKit.createKitView(any(), any(), any(), any()) } returns kitView
        val sparklingView = SparklingView(context)

        sparklingView.prepare(baseContext)
        sparklingView.loadUrl()

        assertTrue(kitView.loadCalled)
        assertEquals(IPerformanceView.LoadStatus.LOADING, sparklingView.loadStatus())
    }

    @Test
    fun lifecycleFinishHidesLoadingAfterLoadStart() {
        val loadingView = View(context)
        val errorView = View(context)
        val kitView = RecordingKitView(context)
        val lifeCycleSlot = slot<IHybridKitLifeCycle>()
        every {
            HybridKit.createKitView(any(), any(), any(), capture(lifeCycleSlot))
        } returns kitView
        baseContext.sparklingUIProvider =
            object : SparklingUIProvider {
                override fun getLoadingView(context: Context): View = loadingView

                override fun getErrorView(context: Context): View = errorView

                override fun getToolBar(context: Context) = null
            }
        val sparklingView = SparklingView(context)

        sparklingView.prepare(baseContext)
        lifeCycleSlot.captured.onLoadStart(kitView, "https://example.com/main.lynx.bundle")
        assertEquals(View.VISIBLE, loadingView.visibility)

        lifeCycleSlot.captured.onLoadFinish(kitView)

        assertEquals(View.GONE, loadingView.visibility)
        assertEquals(View.GONE, errorView.visibility)
        assertEquals(IPerformanceView.LoadStatus.SUCCESS, sparklingView.loadStatus())
    }

    @Test
    fun lifecycleFinishKeepsLoadingWhenAutoRemoveDisabled() {
        val loadingView = View(context)
        val errorView = View(context)
        val kitView = RecordingKitView(context)
        val lifeCycleSlot = slot<IHybridKitLifeCycle>()
        every {
            HybridKit.createKitView(any(), any(), any(), capture(lifeCycleSlot))
        } returns kitView
        baseContext.hybridSchemeParam?.disableAutoRemoveLoading = true
        baseContext.sparklingUIProvider =
            object : SparklingUIProvider {
                override fun getLoadingView(context: Context): View = loadingView

                override fun getErrorView(context: Context): View = errorView

                override fun getToolBar(context: Context) = null
            }
        val sparklingView = SparklingView(context)

        sparklingView.prepare(baseContext)
        lifeCycleSlot.captured.onLoadStart(kitView, "https://example.com/main.lynx.bundle")
        assertEquals(View.VISIBLE, loadingView.visibility)

        lifeCycleSlot.captured.onLoadFinish(kitView)

        assertEquals(View.VISIBLE, loadingView.visibility)
        assertEquals(View.GONE, errorView.visibility)
        assertEquals(IPerformanceView.LoadStatus.SUCCESS, sparklingView.loadStatus())
    }

    @Test
    fun lifecycleStartAfterSuccessResetsLoadStatusToLoading() {
        val kitView = RecordingKitView(context)
        val lifeCycleSlot = slot<IHybridKitLifeCycle>()
        every {
            HybridKit.createKitView(any(), any(), any(), capture(lifeCycleSlot))
        } returns kitView
        val sparklingView = SparklingView(context)

        sparklingView.prepare(baseContext)
        lifeCycleSlot.captured.onLoadStart(kitView, "https://example.com/main.lynx.bundle")
        lifeCycleSlot.captured.onLoadFinish(kitView)
        lifeCycleSlot.captured.onLoadStart(kitView, "https://example.com/main.lynx.bundle")

        assertEquals(IPerformanceView.LoadStatus.LOADING, sparklingView.loadStatus())
    }

    @Test
    fun lifecycleFailedUpdatesFailStatusAndHidesLoading() {
        val loadingView = View(context)
        val errorView = View(context)
        val kitView = RecordingKitView(context)
        val lifeCycleSlot = slot<IHybridKitLifeCycle>()
        every {
            HybridKit.createKitView(any(), any(), any(), capture(lifeCycleSlot))
        } returns kitView
        baseContext.sparklingUIProvider =
            object : SparklingUIProvider {
                override fun getLoadingView(context: Context): View = loadingView

                override fun getErrorView(context: Context): View = errorView

                override fun getToolBar(context: Context) = null
            }
        val sparklingView = SparklingView(context)

        sparklingView.prepare(baseContext)
        lifeCycleSlot.captured.onLoadStart(kitView, "https://example.com/main.lynx.bundle")
        assertEquals(View.VISIBLE, loadingView.visibility)

        lifeCycleSlot.captured.onLoadFailed(kitView, "https://example.com/main.lynx.bundle", "Network error")

        assertEquals(IPerformanceView.LoadStatus.FAIL, sparklingView.loadStatus())
        assertEquals(View.GONE, loadingView.visibility)
        assertEquals(View.VISIBLE, errorView.visibility)
    }

    @Test
    fun lifecycleReloadShowsLoadingAndClearsError() {
        val loadingView = View(context)
        val errorView = View(context)
        val kitView = RecordingKitView(context)
        val delegate = RecordingSparklingLifecycleDelegate()
        val lifeCycleSlot = slot<IHybridKitLifeCycle>()
        every {
            HybridKit.createKitView(any(), any(), any(), capture(lifeCycleSlot))
        } returns kitView
        baseContext.sparklingUIProvider =
            object : SparklingUIProvider {
                override fun getLoadingView(context: Context): View = loadingView

                override fun getErrorView(context: Context): View = errorView

                override fun getToolBar(context: Context) = null
            }
        baseContext.lifecycleDelegate = delegate
        val sparklingView = SparklingView(context)

        sparklingView.prepare(baseContext)
        lifeCycleSlot.captured.onLoadFailed(kitView, "https://example.com/main.lynx.bundle", "Network error")
        assertEquals(View.VISIBLE, errorView.visibility)

        lifeCycleSlot.captured.onReload(kitView)

        assertEquals(IPerformanceView.LoadStatus.LOADING, sparklingView.loadStatus())
        assertEquals(View.VISIBLE, loadingView.visibility)
        assertEquals(View.GONE, errorView.visibility)
        assertEquals(listOf("reload"), delegate.events)
    }

    @Test
    fun lifecycleCallbacksForwardToSparklingDelegate() {
        val kitView = RecordingKitView(context)
        val delegate = RecordingSparklingLifecycleDelegate()
        val error =
            HybridKitError().apply {
                errorCode = 210
                errorReason = "LynxReceiveError"
            }
        val perf = mapOf<String, Any>("setup_timing" to mapOf("draw_end" to 123L))
        val lifeCycleSlot = slot<IHybridKitLifeCycle>()
        every {
            HybridKit.createKitView(any(), any(), any(), capture(lifeCycleSlot))
        } returns kitView
        baseContext.lifecycleDelegate = delegate
        val sparklingView = SparklingView(context)

        sparklingView.prepare(baseContext)
        lifeCycleSlot.captured.onLoadStart(kitView, "https://example.com/main.lynx.bundle")
        lifeCycleSlot.captured.onRuntimeReady(HybridKitType.LYNX)
        lifeCycleSlot.captured.onFirstScreen(kitView)
        lifeCycleSlot.captured.onPageUpdate(kitView)
        lifeCycleSlot.captured.onUpdate(kitView)
        lifeCycleSlot.captured.onReceivedPerformance(kitView, perf)
        lifeCycleSlot.captured.onReceivedError(kitView, error)
        lifeCycleSlot.captured.onResourceLoadStart(kitView, "https://example.com/main.lynx.bundle")
        lifeCycleSlot.captured.onResourceLoadFinish(
            kitView,
            "https://example.com/main.lynx.bundle",
            byteArrayOf(4, 5, 6),
            null,
        )
        lifeCycleSlot.captured.onLoadFinish(kitView)

        assertEquals(
            listOf(
                "start:https://example.com/main.lynx.bundle",
                "runtime:LYNX",
                "first_screen",
                "page_update",
                "update",
                "perf",
                "error:LynxReceiveError",
                "resource_start:https://example.com/main.lynx.bundle",
                "resource_fetch:https://example.com/main.lynx.bundle",
                "resource_finish:https://example.com/main.lynx.bundle",
                "finish",
            ),
            delegate.events,
        )
    }

    @Test
    fun fixedWidthCardUpdatesHeightFromContentSizeOnFirstScreen() {
        val kitView = RecordingKitView(context)
        val delegate = RecordingSparklingLifecycleDelegate()
        val lifeCycleSlot = slot<IHybridKitLifeCycle>()
        every {
            HybridKit.createKitView(any(), any(), any(), capture(lifeCycleSlot))
        } returns kitView
        baseContext.hybridSchemeParam?.containerType = HybridContainerType.CARD
        baseContext.lifecycleDelegate = delegate
        val sparklingView =
            SparklingView(context).apply {
                sparkContentMode = SparklingViewContentMode.FIXED_WIDTH
                layoutParams = FrameLayout.LayoutParams(320, 80)
            }

        sparklingView.prepare(baseContext)
        kitView.realView().measure(
            View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(240, View.MeasureSpec.EXACTLY),
        )
        kitView.realView().layout(0, 0, 320, 240)
        lifeCycleSlot.captured.onFirstScreen(kitView)

        assertEquals(320, sparklingView.layoutParams.width)
        assertEquals(240, sparklingView.layoutParams.height)
        assertEquals(Size(320, 240), sparklingView.contentSize)
        assertEquals(Size(320, 240), sparklingView.preferredLayoutSize)
        assertTrue(delegate.events.contains("size:320x240"))
    }

    @Test
    fun fixedHeightCardUpdatesWidthFromContentSizeOnPageUpdate() {
        val kitView = RecordingKitView(context)
        val lifeCycleSlot = slot<IHybridKitLifeCycle>()
        every {
            HybridKit.createKitView(any(), any(), any(), capture(lifeCycleSlot))
        } returns kitView
        baseContext.hybridSchemeParam?.containerType = HybridContainerType.CARD
        val sparklingView =
            SparklingView(context).apply {
                sparkContentMode = SparklingViewContentMode.FIXED_HEIGHT
                layoutParams = FrameLayout.LayoutParams(100, 72)
            }

        sparklingView.prepare(baseContext)
        kitView.realView().measure(
            View.MeasureSpec.makeMeasureSpec(180, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(72, View.MeasureSpec.EXACTLY),
        )
        kitView.realView().layout(0, 0, 180, 72)
        lifeCycleSlot.captured.onPageUpdate(kitView)

        assertEquals(180, sparklingView.layoutParams.width)
        assertEquals(72, sparklingView.layoutParams.height)
        assertEquals(Size(180, 72), sparklingView.contentSize)
        assertEquals(Size(180, 72), sparklingView.preferredLayoutSize)
    }

    @Test
    fun fitSizeCardUpdatesWidthAndHeightFromContentSize() {
        val kitView = RecordingKitView(context)
        val lifeCycleSlot = slot<IHybridKitLifeCycle>()
        every {
            HybridKit.createKitView(any(), any(), any(), capture(lifeCycleSlot))
        } returns kitView
        baseContext.hybridSchemeParam?.containerType = HybridContainerType.CARD
        val sparklingView =
            SparklingView(context).apply {
                sparkContentMode = SparklingViewContentMode.FIT_SIZE
                layoutParams = FrameLayout.LayoutParams(100, 72)
            }

        sparklingView.prepare(baseContext)
        kitView.realView().measure(
            View.MeasureSpec.makeMeasureSpec(180, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(240, View.MeasureSpec.EXACTLY),
        )
        kitView.realView().layout(0, 0, 180, 240)
        lifeCycleSlot.captured.onFirstScreen(kitView)

        assertEquals(180, sparklingView.layoutParams.width)
        assertEquals(240, sparklingView.layoutParams.height)
        assertEquals(Size(180, 240), sparklingView.contentSize)
        assertEquals(Size(180, 240), sparklingView.preferredLayoutSize)
    }

    @Test
    fun fixedSizeCardKeepsFrameWhenContentSizeChanges() {
        val kitView = RecordingKitView(context)
        val lifeCycleSlot = slot<IHybridKitLifeCycle>()
        every {
            HybridKit.createKitView(any(), any(), any(), capture(lifeCycleSlot))
        } returns kitView
        baseContext.hybridSchemeParam?.containerType = HybridContainerType.CARD
        val sparklingView =
            SparklingView(context).apply {
                sparkContentMode = SparklingViewContentMode.FIXED_SIZE
                layoutParams = FrameLayout.LayoutParams(100, 72)
            }

        sparklingView.prepare(baseContext)
        kitView.realView().measure(
            View.MeasureSpec.makeMeasureSpec(180, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(240, View.MeasureSpec.EXACTLY),
        )
        kitView.realView().layout(0, 0, 180, 240)
        lifeCycleSlot.captured.onFirstScreen(kitView)

        assertEquals(100, sparklingView.layoutParams.width)
        assertEquals(72, sparklingView.layoutParams.height)
        assertEquals(Size(180, 240), sparklingView.contentSize)
        assertEquals(Size(100, 72), sparklingView.preferredLayoutSize)
    }

    @Test
    fun prepareWithoutSchemeShowsErrorView() {
        val loadingView = View(context)
        val errorView = View(context)
        val sparklingView = SparklingView(context)
        val contextWithoutScheme =
            SparklingContext().apply {
                containerId = baseContext.containerId
                hybridSchemeParam = null
                sparklingUIProvider =
                    object : SparklingUIProvider {
                        override fun getLoadingView(context: Context): View = loadingView

                        override fun getErrorView(context: Context): View = errorView

                        override fun getToolBar(context: Context) = null
                    }
            }

        sparklingView.prepare(contextWithoutScheme)

        assertEquals(contextWithoutScheme, sparklingView.sparklingContext)
        assertEquals(View.VISIBLE, errorView.visibility)
        assertEquals(View.GONE, loadingView.visibility)
        assertNull(sparklingView.getKitView())
    }

    @Test
    fun handleUIDefaultsToWhiteWhenNoContainerColor() {
        val kitView = RecordingKitView(context)
        every { HybridKit.createKitView(any(), any(), any(), any()) } returns kitView
        val sparklingView = SparklingView(context)
        val contextWithoutColor =
            SparklingContext().apply {
                containerId = baseContext.containerId
                hybridSchemeParam =
                    HybridSchemeParam().apply {
                        engineType = HybridKitType.LYNX
                        containerBgColor = null
                    }
            }

        sparklingView.prepare(contextWithoutColor)

        val realView = kitView.realView()
        assertTrue(realView?.background is ColorDrawable)
        val color = (realView?.background as ColorDrawable).color
        assertEquals(Color.WHITE, color)
    }

    // Tests for addDebugTagView are disabled because the method is commented out in SparklingView.kt
    // @Test
    // fun addDebugTagViewHonoursDebugFlag() {
    //   mockkObject(HybridCommon)
    //   every { HybridCommon.hybridConfig?.baseInfoConfig?.isDebug } returns true
    //   val sparklingView = SparklingView(context)
    //
    //   sparklingView.addDebugTagView()
    //
    //   assertEquals(1, sparklingView.childCount)
    // }

    // @Test
    // fun addDebugTagViewSkipsWhenDebugDisabled() {
    //   mockkObject(HybridCommon)
    //   every { HybridCommon.hybridConfig?.baseInfoConfig?.isDebug } returns false
    //   val sparklingView = SparklingView(context)
    //
    //   sparklingView.addDebugTagView()
    //
    //   assertEquals(0, sparklingView.childCount)
    // }

    @Test
    fun sparklingViewIsFrameLayout() {
        val sparklingView = SparklingView(context)
        assertTrue(sparklingView is FrameLayout)
    }

    private open class RecordingKitView(
        context: Context,
    ) : IKitView {
        override var hybridContext: HybridContext = HybridContext()
        private val realView = View(context)
        var loadCalled = false
        var onShowCalled = false
        var onHideCalled = false
        var lastGlobalPropsUpdate: Map<String, Any>? = null

        override fun realView(): View = realView

        override fun load() {
            loadCalled = true
        }

        override fun load(uri: String) {
            loadCalled = true
        }

        override fun reload() {
        }

        override fun updateGlobalPropsByIncrement(data: Map<String, Any>) {
            lastGlobalPropsUpdate = data
        }

        override fun onShow() {
            onShowCalled = true
        }

        override fun onHide() {
            onHideCalled = true
        }

        override fun destroy(clearContext: Boolean) {
        }

        override fun hasDestroyed(): Boolean = false

        override fun getGlobalProps(): MutableMap<String, Any>? = null

        override fun getScheme(): String? = null

        override fun onLoadSuccess() {
        }

        override fun sendEventByJSON(
            eventName: String,
            params: org.json.JSONObject?,
        ) {
        }

        override fun refreshContext(context: Context) {
        }

        override fun refreshSchemeParam(param: HybridSchemeParam) {
        }
    }

    private class RecordingSparklingLifecycleDelegate : SparklingLifecycleDelegate {
        val events = mutableListOf<String>()

        override fun onLoadStart(
            view: IKitView,
            url: String,
        ) {
            events += "start:$url"
        }

        override fun onLoadFinish(view: IKitView) {
            events += "finish"
        }

        override fun onRuntimeReady(kitType: HybridKitType) {
            events += "runtime:$kitType"
        }

        override fun onFirstScreen(view: IKitView) {
            events += "first_screen"
        }

        override fun onPageUpdate(view: IKitView) {
            events += "page_update"
        }

        override fun onUpdate(view: IKitView) {
            events += "update"
        }

        override fun onReceivedError(
            view: IKitView,
            error: HybridKitError,
        ) {
            events += "error:${error.errorReason}"
        }

        override fun onReceivedPerformance(
            view: IKitView,
            perf: Map<String, Any>?,
        ) {
            events += "perf"
        }

        override fun onResourceLoadStart(
            view: IKitView,
            url: String,
        ) {
            events += "resource_start:$url"
        }

        override fun onResourceLoadFinish(
            view: IKitView,
            url: String,
            templateArray: ByteArray?,
            error: HybridKitError?,
        ) {
            events += "resource_fetch:$url"
        }

        override fun onResourceLoadFinish(
            templateArray: ByteArray?,
            baseUrl: String?,
        ) {
            events += "resource_finish:$baseUrl"
        }

        override fun onIntrinsicContentSizeChanged(
            view: IKitView,
            size: Size,
        ) {
            events += "size:${size.width}x${size.height}"
        }

        override fun onReload(view: IKitView) {
            events += "reload"
        }
    }

    @Test
    fun getHybridViewContextReturnsContext() {
        val sparklingView = SparklingView(context)
        assertEquals(context, sparklingView.getHybridViewContext())
    }

    @Test
    fun actualViewReturnsSelf() {
        val sparklingView = SparklingView(context)
        assertEquals(sparklingView, sparklingView.actualView())
    }

    @Test
    fun obtainHybridContextReturnsSparklingContext() {
        val kitView = RecordingKitView(context)
        every { HybridKit.createKitView(any(), any(), any(), any()) } returns kitView
        val sparklingView = SparklingView(context)

        sparklingView.prepare(baseContext)

        assertEquals(baseContext, sparklingView.obtainHybridContext())
    }

    @Test
    fun obtainHybridContextReturnsNullWhenNotPrepared() {
        val sparklingView = SparklingView(context)
        assertNull(sparklingView.obtainHybridContext())
    }

    @Test
    fun getPerformanceViewHybridContextReturnsSparklingContext() {
        val kitView = RecordingKitView(context)
        every { HybridKit.createKitView(any(), any(), any(), any()) } returns kitView
        val sparklingView = SparklingView(context)

        sparklingView.prepare(baseContext)

        assertEquals(baseContext, sparklingView.getPerformanceViewHybridContext())
    }

    @Test
    fun hasReleaseReturnsFalseInitially() {
        val sparklingView = SparklingView(context)
        assertFalse(sparklingView.hasRelease())
    }

    @Test
    fun hasReleaseReturnsTrueAfterRelease() {
        val kitView = RecordingKitView(context)
        every { HybridKit.createKitView(any(), any(), any(), any()) } returns kitView
        val sparklingView = SparklingView(context)

        sparklingView.prepare(baseContext)
        sparklingView.release()

        assertTrue(sparklingView.hasRelease())
    }

    @Test
    fun releaseIsIdempotent() {
        val kitView = RecordingKitView(context)
        every { HybridKit.createKitView(any(), any(), any(), any()) } returns kitView
        val sparklingView = SparklingView(context)

        sparklingView.prepare(baseContext)
        sparklingView.release()
        sparklingView.release() // Second call should be no-op

        assertTrue(sparklingView.hasRelease())
        assertNull(sparklingView.sparklingContext)
    }

    @Test
    fun releaseClearsContext() {
        val kitView = RecordingKitView(context)
        every { HybridKit.createKitView(any(), any(), any(), any()) } returns kitView
        val sparklingView = SparklingView(context)

        sparklingView.prepare(baseContext)
        assertNotNull(sparklingView.sparklingContext)

        sparklingView.release()

        assertNull(sparklingView.sparklingContext)
    }

    @Test
    fun onShowEventDelegatesToKitView() {
        val kitView = RecordingKitView(context)
        every { HybridKit.createKitView(any(), any(), any(), any()) } returns kitView
        val sparklingView = SparklingView(context)

        sparklingView.prepare(baseContext)
        sparklingView.onShowEvent()

        assertTrue(kitView.onShowCalled)
    }

    @Test
    fun onHideEventDelegatesToKitView() {
        val kitView = RecordingKitView(context)
        every { HybridKit.createKitView(any(), any(), any(), any()) } returns kitView
        val sparklingView = SparklingView(context)

        sparklingView.prepare(baseContext)
        sparklingView.onHideEvent()

        assertTrue(kitView.onHideCalled)
    }

    @Test
    fun updateGlobalPropsByIncrementDelegatesToKitView() {
        val kitView = RecordingKitView(context)
        every { HybridKit.createKitView(any(), any(), any(), any()) } returns kitView
        val sparklingView = SparklingView(context)
        val props = mapOf("key" to "value")

        sparklingView.prepare(baseContext)
        sparklingView.updateGlobalPropsByIncrement(props)

        assertEquals(props, kitView.lastGlobalPropsUpdate)
    }

    @Test
    fun processAfterUseCachedDoesNotThrow() {
        val sparklingView = SparklingView(context)
        // Should not throw
        sparklingView.processAfterUseCached(baseContext)
        sparklingView.processAfterUseCached(null)
    }
}
