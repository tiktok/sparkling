// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import android.content.Context
import android.graphics.Color
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
// import androidx.core.graphics.toColorInt
import com.tiktok.sparkling.hybridkit.HybridCommon
import com.tiktok.sparkling.hybridkit.HybridContext
import com.tiktok.sparkling.hybridkit.HybridKit
import com.tiktok.sparkling.hybridkit.base.IHybridKitLifeCycle
import com.tiktok.sparkling.hybridkit.base.IHybridView
import com.tiktok.sparkling.hybridkit.base.IKitView
import com.tiktok.sparkling.hybridkit.base.IPerformanceView
import com.tiktok.sparkling.hybridkit.utils.ColorUtil
import org.json.JSONObject


class SparklingView(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), IHybridView{


    private var loadingView: View? = null
    private var errorView: View? = null
    private var loadingViewBgColor: Int? = null
    private var hideLoading: Boolean = false
    private var hideError: Boolean = false
    private var kitViewDelegate: IKitView? = null
    var sparklingContext: SparklingContext? = null
    // private var debugInfoTag: TextView? = null

    private var loadStatus = IPerformanceView.LoadStatus.INIT
    private var isReleased = false
    private val defaultErrorText = "Oops, something went wrong!"

    fun prepare(sparklingContext: SparklingContext) {
        this.sparklingContext = sparklingContext

        val uiProvider = sparklingContext.sparklingUIProvider
        if (uiProvider != null) {
            loadingView = uiProvider.getLoadingView(context)
            errorView = uiProvider.getErrorView(context)
        } else {
            loadingView = ProgressBar(context).apply {
                layoutParams =
                    LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                        gravity = Gravity.CENTER
                    }
            }
            errorView = TextView(context).apply {
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
        hideError = hybridSchemeParam.hideError
        hybridSchemeParam.loadingBgColor?.let {
            loadingViewBgColor = ColorUtil.parseColorSafely(it)
        }
        loadingViewBgColor?.let { color ->
            loadingView?.setBackgroundColor(color)
        }


        val kitView = HybridKit.createKitView(
            hybridSchemeParam,
            sparklingContext,
            context,
            object : IHybridKitLifeCycle() {
                override fun onLoadStart(view: IKitView, url: String) {
                    super.onLoadStart(view, url)
                    if (Looper.myLooper() != Looper.getMainLooper()) {
                        post {
                            showLoadingView()
                        }
                    } else {
                        showLoadingView()
                    }
                }


                override fun onLoadFailed(view: IKitView, url: String, reason: String?) {
                    super.onLoadFailed(view, url, reason)
                    updateErrorMessage(url, reason)
                    if (Looper.myLooper() != Looper.getMainLooper()) {
                        post {
                            showErrorView()
                        }
                    } else {
                        showErrorView()
                    }
                }

                override fun onLoadFinish(view: IKitView) {
                    super.onLoadFinish(view)
                    if (loadStatus ==  IPerformanceView.LoadStatus.LOADING || loadStatus ==  IPerformanceView.LoadStatus.INIT) {
                        loadStatus = IPerformanceView.LoadStatus.SUCCESS
                    }
                    loadingView?.visibility = GONE
                    errorView?.visibility = GONE
                }
            }
        )
        kitViewDelegate = kitView
        addView(kitView?.realView())

        handleUI()

    }

    override fun getHybridViewContext(): Context {
        return context
    }

    override fun loadUrl() {
        loadStatus = IPerformanceView.LoadStatus.LOADING
        kitViewDelegate?.load()
    }

    fun loadStatus(): IPerformanceView.LoadStatus {
        return loadStatus
    }


    override fun refreshData(
        context: Context,
        hybridContext: HybridContext?
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

    override fun obtainHybridContext(): HybridContext? {
        return sparklingContext
    }

    override fun updateGlobalPropsByIncrement(data: Map<String, Any>) {
        kitViewDelegate?.updateGlobalPropsByIncrement(data)
    }

    override fun sendEventByJSON(eventName: String, params: JSONObject?) {
        kitViewDelegate?.sendEventByJSON(eventName, params)
    }

    override fun actualView(): View {
        return this
    }

    override fun onShowEvent() {
        kitViewDelegate?.onShow()
    }

    override fun onHideEvent() {
        kitViewDelegate?.onHide()
    }

    override fun release() {
        if (isReleased) return
        isReleased = true
        kitViewDelegate?.destroy(true)
        loadingView = null
        errorView = null
        sparklingContext = null
    }

    override fun getPerformanceViewHybridContext(): HybridContext? {
        return sparklingContext
    }

    override fun hasRelease(): Boolean {
        return isReleased
    }

    fun getKitView(): IKitView? {
        return kitViewDelegate
    }

    fun handleUI() {
        if (sparklingContext?.hybridSchemeParam?.containerBgColor == null) {
            kitViewDelegate?.realView()?.setBackgroundColor(Color.WHITE)
        } else {
            sparklingContext?.hybridSchemeParam?.containerBgColor?.let {
                kitViewDelegate?.realView()
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


    override fun isLoadSuccess(): Boolean {
        return loadStatus == IPerformanceView.LoadStatus.SUCCESS
    }

    private fun updateErrorMessage(url: String, reason: String?) {
        val view = errorView as? TextView ?: return
        val suffix = buildString {
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
