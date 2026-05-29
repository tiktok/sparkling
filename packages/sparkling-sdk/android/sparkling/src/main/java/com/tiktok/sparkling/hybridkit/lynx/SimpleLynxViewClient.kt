// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.lynx

import android.net.Uri
import com.lynx.tasm.LynxError
import com.lynx.tasm.LynxPerfMetric
import com.lynx.tasm.LynxViewClient
import com.tiktok.sparkling.hybridkit.base.IHybridKitLifeCycle
import androidx.core.net.toUri
import com.tiktok.sparkling.hybridkit.base.HybridErrorConstantCode
import com.tiktok.sparkling.hybridkit.base.HybridKitError
import com.tiktok.sparkling.hybridkit.base.HybridKitType

class SimpleLynxViewClient(
    var kitView: SimpleLynxKitView,
    val hybridLifeCycle: IHybridKitLifeCycle?,
) : LynxViewClient() {
    var uri: Uri? = null
    private var loadFinished = false
    private var loadFailed = false
    private var destroyed = false

    override fun onPageStart(url: String?) {
        super.onPageStart(url)
        uri = url?.toUri()
        loadFinished = false
        loadFailed = false
    }

    override fun onRuntimeReady() {
        super.onRuntimeReady()
        if (canAccessKitView()) {
            hybridLifeCycle?.onRuntimeReady(HybridKitType.LYNX)
        }
    }

    override fun onFirstScreen() {
        super.onFirstScreen()
        if (canAccessKitView()) {
            hybridLifeCycle?.onFirstScreen(kitView)
        }
    }

    override fun onPageUpdate() {
        super.onPageUpdate()
        if (canAccessKitView()) {
            hybridLifeCycle?.onPageUpdate(kitView)
        }
    }

    override fun onDataUpdated() {
        super.onDataUpdated()
        if (canAccessKitView()) {
            hybridLifeCycle?.onUpdate(kitView)
        }
    }

    override fun onUpdateDataWithoutChange() {
        super.onUpdateDataWithoutChange()
        if (canAccessKitView()) {
            hybridLifeCycle?.onUpdate(kitView)
        }
    }

    override fun onTimingSetup(timingInfo: MutableMap<String, Any>?) {
        super.onTimingSetup(timingInfo)
        if (canAccessKitView()) {
            hybridLifeCycle?.onReceivedPerformance(kitView, timingInfo)
        }
    }

    override fun onTimingUpdate(
        timingInfo: MutableMap<String, Any>?,
        updateTiming: MutableMap<String, Long>?,
        flag: String?,
    ) {
        super.onTimingUpdate(timingInfo, updateTiming, flag)
        if (!canAccessKitView()) {
            return
        }
        hybridLifeCycle?.onReceivedPerformance(
            kitView,
            buildMap {
                timingInfo?.let { put("timing_info", it) }
                updateTiming?.let { put("update_timing", it) }
                flag?.let { put("flag", it) }
            },
        )
    }

    override fun onFirstLoadPerfReady(metric: LynxPerfMetric?) {
        super.onFirstLoadPerfReady(metric)
        if (canAccessKitView()) {
            hybridLifeCycle?.onReceivedPerformance(kitView, metric.toPerformanceMap("first_load"))
        }
    }

    override fun onUpdatePerfReady(metric: LynxPerfMetric?) {
        super.onUpdatePerfReady(metric)
        if (canAccessKitView()) {
            hybridLifeCycle?.onReceivedPerformance(kitView, metric.toPerformanceMap("update"))
        }
    }

    override fun onLoadSuccess() {
        super.onLoadSuccess()
        if (loadFinished || loadFailed || !canAccessKitView()) {
            return
        }
        loadFinished = true
        kitView.onLoadSuccess()
    }

    override fun onReceivedError(error: LynxError?) {
        super.onReceivedError(error)
        if (!canAccessKitView() || error == null) {
            return
        }

        val hybridKitError = error.toHybridKitError()
        hybridLifeCycle?.onReceivedError(kitView, hybridKitError)
        if (
            error.isFatal() &&
            uri != null &&
            !loadFailed
        ) {
            loadFailed = true
            hybridLifeCycle?.onLoadFailed(
                kitView,
                uri.toString(),
                hybridKitError,
            )
        }
    }

    override fun onLynxViewAndJSRuntimeDestroy() {
        super.onLynxViewAndJSRuntimeDestroy()
        if (destroyed) {
            return
        }
        destroyed = true
        kitView.destroyWhenJSRuntimeCallback()
    }

    private fun canAccessKitView(): Boolean = !destroyed && !kitView.hasDestroyed()

    private fun LynxError.toHybridKitError(): HybridKitError =
        HybridKitError().apply {
            errorCode = HybridErrorConstantCode.LynxLoadError
            errorReason = "LynxReceiveError"
            originCode = this@toHybridKitError.errorCode
            originReason = this@toHybridKitError.msg
        }

    private fun LynxPerfMetric?.toPerformanceMap(type: String): Map<String, Any>? =
        this?.let {
            mapOf(
                "type" to type,
                "metric" to it,
                "metric_json" to it.toJSONObject().toString(),
            )
        }
}
