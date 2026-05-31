// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import android.content.Context
import android.graphics.Color
import android.os.Looper
import android.util.AttributeSet
import android.util.Size
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.lynx.tasm.LynxView
// import androidx.core.graphics.toColorInt
import com.tiktok.sparkling.hybridkit.HybridCommon
import com.tiktok.sparkling.hybridkit.HybridContext
import com.tiktok.sparkling.hybridkit.HybridKit
import com.tiktok.sparkling.hybridkit.base.HybridKitError
import com.tiktok.sparkling.hybridkit.base.HybridKitType
import com.tiktok.sparkling.hybridkit.base.IHybridKitLifeCycle
import com.tiktok.sparkling.hybridkit.base.IHybridView
import com.tiktok.sparkling.hybridkit.base.IKitView
import com.tiktok.sparkling.hybridkit.base.IPerformanceView
import com.tiktok.sparkling.hybridkit.utils.ColorUtil
import org.json.JSONObject

class SparklingView(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr),
    IHybridView {
    private var loadingView: View? = null
    private var errorView: View? = null
    private var loadingViewBgColor: Int? = null
    private var hideLoading: Boolean = false
    private var disableAutoRemoveLoading: Boolean = false
    private var hideError: Boolean = false
    private var kitViewDelegate: IKitView? = null
    private var observedKitRealView: View? = null
    var sparklingContext: SparklingContext? = null
    // private var debugInfoTag: TextView? = null

    var sparkContentMode: SparklingViewContentMode = SparklingViewContentMode.FIXED_SIZE
    var contentSize: Size = Size(0, 0)
        private set
    val preferredLayoutSize: Size
        get() {
            val frameWidth = currentFrameWidth()
            val frameHeight = currentFrameHeight()
            return when (sparkContentMode) {
                SparklingViewContentMode.FIT_SIZE -> {
                    contentSize.withFallback(frameWidth, frameHeight)
                }

                SparklingViewContentMode.FIXED_WIDTH -> {
                    Size(frameWidth.takeIf { it > 0 } ?: contentSize.width, contentSize.height)
                }

                SparklingViewContentMode.FIXED_HEIGHT -> {
                    Size(contentSize.width, frameHeight.takeIf { it > 0 } ?: contentSize.height)
                }

                SparklingViewContentMode.FIXED_SIZE -> {
                    Size(frameWidth, frameHeight)
                }
            }
        }

    private var loadStatus = IPerformanceView.LoadStatus.INIT
    private var isReleased = false
    private val defaultErrorText = "Oops, something went wrong!"
    private val kitLayoutChangeListener =
        View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            kitViewDelegate?.let { updateAdaptiveContentSize(it) }
        }

    fun prepare(sparklingContext: SparklingContext) {
        this.sparklingContext = sparklingContext

        val uiProvider = sparklingContext.sparklingUIProvider
        if (uiProvider != null) {
            loadingView = uiProvider.getLoadingView(context)
            errorView = uiProvider.getErrorView(context)
        } else {
            loadingView =
                ProgressBar(context).apply {
                    layoutParams =
                        LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                            gravity = Gravity.CENTER
                        }
                }
            errorView =
                TextView(context).apply {
                    text = defaultErrorText
                    setTextColor(Color.BLACK)
                    gravity = Gravity.CENTER
                    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                }
        }
        val hybridSchemeParam = sparklingContext.hybridSchemeParam
        if (hybridSchemeParam == null) {
            updateErrorMessage("", "invalid scheme or bundle address")
            errorView?.let {
                if (it.parent == null) {
                    addView(it)
                }
                it.visibility = View.VISIBLE
            }
            loadingView?.visibility = View.GONE
            return
        }

        hideLoading = hybridSchemeParam.hideLoading
        disableAutoRemoveLoading = hybridSchemeParam.disableAutoRemoveLoading
        hideError = hybridSchemeParam.hideError
        hybridSchemeParam.loadingBgColor?.let {
            loadingViewBgColor = ColorUtil.parseColorSafely(it)
        }
        loadingViewBgColor?.let { color ->
            loadingView?.setBackgroundColor(color)
        }

        val kitView =
            HybridKit.createKitView(
                hybridSchemeParam,
                sparklingContext,
                context,
                object : IHybridKitLifeCycle() {
                    override fun onLoadStart(
                        view: IKitView,
                        url: String,
                    ) {
                        super.onLoadStart(view, url)
                        if (isReleased) {
                            return
                        }
                        loadStatus = IPerformanceView.LoadStatus.LOADING
                        runOnMain {
                            errorView?.visibility = GONE
                            showLoadingView()
                        }
                        this@SparklingView.sparklingContext?.lifecycleDelegate?.onLoadStart(view, url)
                    }

                    override fun onLoadFailed(
                        view: IKitView,
                        url: String,
                    ) {
                        handleLoadFailed(view, url, HybridKitError())
                    }

                    override fun onLoadFailed(
                        view: IKitView,
                        url: String,
                        reason: String?,
                    ) {
                        handleLoadFailed(
                            view,
                            url,
                            HybridKitError().apply {
                                errorReason = reason
                            },
                        )
                    }

                    override fun onLoadFailed(
                        view: IKitView,
                        url: String,
                        hybridKitError: HybridKitError,
                    ) {
                        handleLoadFailed(view, url, hybridKitError)
                    }

                    override fun onRuntimeReady(kitType: HybridKitType) {
                        super.onRuntimeReady(kitType)
                        if (isReleased) {
                            return
                        }
                        this@SparklingView.sparklingContext?.lifecycleDelegate?.onRuntimeReady(kitType)
                    }

                    override fun onFirstScreen(view: IKitView) {
                        super.onFirstScreen(view)
                        if (isReleased) {
                            return
                        }
                        updateAdaptiveContentSize(view)
                        this@SparklingView.sparklingContext?.lifecycleDelegate?.onFirstScreen(view)
                    }

                    override fun onPageUpdate(view: IKitView) {
                        super.onPageUpdate(view)
                        if (isReleased) {
                            return
                        }
                        updateAdaptiveContentSize(view)
                        this@SparklingView.sparklingContext?.lifecycleDelegate?.onPageUpdate(view)
                    }

                    override fun onUpdate(view: IKitView) {
                        super.onUpdate(view)
                        if (isReleased) {
                            return
                        }
                        this@SparklingView.sparklingContext?.lifecycleDelegate?.onUpdate(view)
                    }

                    override fun onReceivedError(
                        view: IKitView,
                        hybridKitError: HybridKitError,
                    ) {
                        super.onReceivedError(view, hybridKitError)
                        if (isReleased) {
                            return
                        }
                        this@SparklingView.sparklingContext?.lifecycleDelegate?.onReceivedError(view, hybridKitError)
                    }

                    override fun onReceivedPerformance(
                        view: IKitView,
                        perf: Map<String, Any>?,
                    ) {
                        super.onReceivedPerformance(view, perf)
                        if (isReleased) {
                            return
                        }
                        this@SparklingView.sparklingContext?.lifecycleDelegate?.onReceivedPerformance(view, perf)
                    }

                    override fun onLoadFinish(view: IKitView) {
                        super.onLoadFinish(view)
                        if (
                            isReleased ||
                            (
                                loadStatus != IPerformanceView.LoadStatus.LOADING &&
                                    loadStatus != IPerformanceView.LoadStatus.INIT
                            )
                        ) {
                            return
                        }
                        loadStatus = IPerformanceView.LoadStatus.SUCCESS
                        runOnMain {
                            removeLoadingView()
                            errorView?.visibility = GONE
                        }
                        this@SparklingView.sparklingContext?.lifecycleDelegate?.onLoadFinish(view)
                    }

                    override fun onReload(view: IKitView) {
                        super.onReload(view)
                        if (isReleased) {
                            return
                        }
                        loadStatus = IPerformanceView.LoadStatus.LOADING
                        runOnMain {
                            errorView?.visibility = GONE
                            showLoadingView()
                        }
                        this@SparklingView.sparklingContext?.lifecycleDelegate?.onReload(view)
                    }

                    override fun onResourceLoadFinish(
                        templateArray: ByteArray?,
                        baseUrl: String?,
                    ) {
                        super.onResourceLoadFinish(templateArray, baseUrl)
                        if (isReleased) {
                            return
                        }
                        this@SparklingView.sparklingContext?.lifecycleDelegate?.onResourceLoadFinish(templateArray, baseUrl)
                    }

                    override fun onResourceLoadStart(
                        view: IKitView,
                        url: String,
                    ) {
                        super.onResourceLoadStart(view, url)
                        if (isReleased) {
                            return
                        }
                        this@SparklingView.sparklingContext?.lifecycleDelegate?.onResourceLoadStart(view, url)
                    }

                    override fun onResourceLoadFinish(
                        view: IKitView,
                        url: String,
                        templateArray: ByteArray?,
                        hybridKitError: HybridKitError?,
                    ) {
                        if (isReleased) {
                            return
                        }
                        this@SparklingView.sparklingContext?.lifecycleDelegate?.onResourceLoadFinish(
                            view,
                            url,
                            templateArray,
                            hybridKitError,
                        )
                        if (hybridKitError == null) {
                            this@SparklingView.sparklingContext?.lifecycleDelegate?.onResourceLoadFinish(templateArray, url)
                        }
                    }
                },
            )
        kitViewDelegate = kitView
        addView(kitView?.realView())
        observeKitViewLayout(kitView)

        handleUI()
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (sparkContentMode == SparklingViewContentMode.FIXED_SIZE) {
            return
        }
        val preferredSize = preferredLayoutSize
        if (preferredSize.width <= 0 || preferredSize.height <= 0) {
            return
        }
        setMeasuredDimension(
            resolveSize(preferredSize.width, widthMeasureSpec),
            resolveSize(preferredSize.height, heightMeasureSpec),
        )
    }

    override fun getHybridViewContext(): Context = context

    override fun loadUrl() {
        loadStatus = IPerformanceView.LoadStatus.LOADING
        kitViewDelegate?.load()
    }

    fun loadStatus(): IPerformanceView.LoadStatus = loadStatus

    override fun refreshData(
        context: Context,
        hybridContext: HybridContext?,
    ) {
        kitViewDelegate?.refreshContext(context)
        (hybridContext as? SparklingContext)?.let { updatedContext ->
            updatedContext.hybridSchemeParam?.let {
                kitViewDelegate?.refreshSchemeParam(it)
            }
        }
    }

    override fun processAfterUseCached(hybridContext: HybridContext?) {
        // No-op: reserved for future cache processing logic
    }

    override fun obtainHybridContext(): HybridContext? = sparklingContext

    override fun updateGlobalPropsByIncrement(data: Map<String, Any>) {
        kitViewDelegate?.updateGlobalPropsByIncrement(data)
    }

    override fun sendEventByJSON(
        eventName: String,
        params: JSONObject?,
    ) {
        kitViewDelegate?.sendEventByJSON(eventName, params)
    }

    override fun actualView(): View = this

    override fun onShowEvent() {
        kitViewDelegate?.onShow()
    }

    override fun onHideEvent() {
        kitViewDelegate?.onHide()
    }

    override fun release() {
        if (isReleased) return
        isReleased = true
        observedKitRealView?.removeOnLayoutChangeListener(kitLayoutChangeListener)
        observedKitRealView = null
        kitViewDelegate?.destroy(true)
        loadingView = null
        errorView = null
        sparklingContext = null
    }

    override fun getPerformanceViewHybridContext(): HybridContext? = sparklingContext

    override fun hasRelease(): Boolean = isReleased

    fun getKitView(): IKitView? = kitViewDelegate

    fun handleUI() {
        if (sparklingContext?.hybridSchemeParam?.containerBgColor == null) {
            kitViewDelegate?.realView()?.setBackgroundColor(Color.WHITE)
        } else {
            sparklingContext?.hybridSchemeParam?.containerBgColor?.let {
                kitViewDelegate
                    ?.realView()
                    ?.setBackgroundColor(ColorUtil.parseColorSafely(it))
            }
        }
        // addDebugTagView()
    }

    /*
    fun addDebugTagView() {
        if (HybridCommon.hybridConfig?.baseInfoConfig?.isDebug == true) {
            debugInfoTag = TextView(context).apply {
                text = "Sparkling-LynxView"
                setTextColor(Color.BLACK)
                textSize = 10f
                setPadding(55, 4, 7, 4)
                setBackgroundColor("#440066CC".toColorInt())
                layoutParams =
                    LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                        gravity = Gravity.BOTTOM or Gravity.START
                    }
            }
            addView(debugInfoTag)
        }
    }
     */

    fun showLoadingView() {
        if (hideLoading) return
        loadingView?.let {
            if (it.parent == null) {
                addView(it)
            }
            it.visibility = View.VISIBLE
            loadingViewBgColor?.let { color ->
                it.setBackgroundColor(color)
            }
        }
    }

    fun showErrorView() {
        if (hideError) return
        errorView?.let {
            if (it.parent == null) {
                addView(it)
            }
            it.visibility = View.VISIBLE
        }
    }

    override fun isLoadSuccess(): Boolean = loadStatus == IPerformanceView.LoadStatus.SUCCESS

    private fun handleLoadFailed(
        view: IKitView,
        url: String,
        hybridKitError: HybridKitError,
    ) {
        if (isReleased) {
            return
        }
        loadStatus = IPerformanceView.LoadStatus.FAIL
        updateErrorMessage(url, hybridKitError.errorReason)
        runOnMain {
            removeLoadingView()
            showErrorView()
        }
        sparklingContext?.lifecycleDelegate?.onLoadFailed(view, url, hybridKitError)
    }

    private fun removeLoadingView() {
        if (!disableAutoRemoveLoading) {
            loadingView?.visibility = GONE
        }
    }

    private fun observeKitViewLayout(kitView: IKitView?) {
        observedKitRealView?.removeOnLayoutChangeListener(kitLayoutChangeListener)
        observedKitRealView = kitView?.realView()
        observedKitRealView?.addOnLayoutChangeListener(kitLayoutChangeListener)
    }

    private fun updateAdaptiveContentSize(view: IKitView) {
        if (isReleased) {
            return
        }
        val realView = view.realView() ?: return
        val newContentSize = realView.resolvedContentSize() ?: return
        if (newContentSize != contentSize) {
            contentSize = newContentSize
            sparklingContext?.lifecycleDelegate?.onIntrinsicContentSizeChanged(view, newContentSize)
        }
        applyContentModeSize(newContentSize)
    }

    private fun applyContentModeSize(size: Size) {
        if (sparkContentMode == SparklingViewContentMode.FIXED_SIZE) {
            return
        }
        val currentLayoutParams =
            layoutParams ?: LayoutParams(
                currentFrameWidth().takeIf { it > 0 } ?: size.width,
                currentFrameHeight().takeIf { it > 0 } ?: size.height,
            )
        var targetWidth = currentLayoutParams.width
        var targetHeight = currentLayoutParams.height
        when (sparkContentMode) {
            SparklingViewContentMode.FIT_SIZE -> {
                if (size.width > 0) targetWidth = size.width
                if (size.height > 0) targetHeight = size.height
            }

            SparklingViewContentMode.FIXED_WIDTH -> {
                if (size.height > 0) targetHeight = size.height
            }

            SparklingViewContentMode.FIXED_HEIGHT -> {
                if (size.width > 0) targetWidth = size.width
            }

            SparklingViewContentMode.FIXED_SIZE -> {
                return
            }
        }
        if (targetWidth != currentLayoutParams.width || targetHeight != currentLayoutParams.height) {
            currentLayoutParams.width = targetWidth
            currentLayoutParams.height = targetHeight
            layoutParams = currentLayoutParams
        } else {
            requestLayout()
        }
    }

    private fun currentFrameWidth(): Int =
        width.takeIf { it > 0 }
            ?: measuredWidth.takeIf { it > 0 }
            ?: layoutParams?.width?.takeIf { it > 0 }
            ?: 0

    private fun currentFrameHeight(): Int =
        height.takeIf { it > 0 }
            ?: measuredHeight.takeIf { it > 0 }
            ?: layoutParams?.height?.takeIf { it > 0 }
            ?: 0

    private fun Size.withFallback(
        fallbackWidth: Int,
        fallbackHeight: Int,
    ): Size =
        Size(
            width.takeIf { it > 0 } ?: fallbackWidth,
            height.takeIf { it > 0 } ?: fallbackHeight,
        )

    private fun View.resolvedContentSize(): Size? {
        val resolvedWidth =
            (this as? LynxView)?.intrinsicWidth?.takeIf { it > 0 }
                ?: measuredWidth.takeIf { it > 0 }
                ?: width.takeIf { it > 0 }
        val resolvedHeight =
            (this as? LynxView)?.intrinsicHeight?.takeIf { it > 0 }
                ?: measuredHeight.takeIf { it > 0 }
                ?: height.takeIf { it > 0 }
        if (resolvedWidth == null || resolvedHeight == null) {
            return null
        }
        return Size(resolvedWidth, resolvedHeight)
    }

    private fun runOnMain(action: () -> Unit) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            post {
                if (!isReleased) {
                    action()
                }
            }
        } else {
            action()
        }
    }

    private fun updateErrorMessage(
        url: String,
        reason: String?,
    ) {
        val view = errorView as? TextView ?: return
        val suffix =
            buildString {
                if (!reason.isNullOrBlank()) {
                    append('\n')
                    append(reason)
                }
                if (url.isNotBlank()) {
                    append("\nURL: ")
                    append(url)
                }
            }
        view.text = defaultErrorText + suffix
    }
}
