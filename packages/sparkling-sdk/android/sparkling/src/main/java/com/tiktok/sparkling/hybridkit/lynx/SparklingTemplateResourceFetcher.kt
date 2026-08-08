// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.lynx

import com.lynx.tasm.resourceprovider.LynxResourceCallback
import com.lynx.tasm.resourceprovider.LynxResourceRequest
import com.lynx.tasm.resourceprovider.LynxResourceResponse
import com.lynx.tasm.resourceprovider.template.LynxTemplateResourceFetcher
import com.lynx.tasm.resourceprovider.template.TemplateProviderResult
import com.tiktok.sparkling.hybridkit.base.HybridErrorConstantCode
import com.tiktok.sparkling.hybridkit.base.HybridKitError
import com.tiktok.sparkling.hybridkit.base.IHybridKitLifeCycle
import com.tiktok.sparkling.hybridkit.base.IKitView
import java.util.concurrent.atomic.AtomicBoolean

internal class SparklingTemplateResourceFetcher(
    private val delegate: LynxTemplateResourceFetcher,
    private val lifeCycle: IHybridKitLifeCycle?,
    private val kitViewProvider: () -> IKitView?,
) : LynxTemplateResourceFetcher() {
    override fun fetchTemplate(
        request: LynxResourceRequest,
        callback: LynxResourceCallback<TemplateProviderResult>,
    ) {
        notifyResourceLoadStart(request.url)
        val wrappedCallback =
            onceCallback(request.url, callback) { response ->
                response.data?.templateBinary
            }
        runCatching {
            delegate.fetchTemplate(request, wrappedCallback)
        }.onFailure {
            wrappedCallback.onResponse(failedResponse(it))
        }
    }

    override fun fetchSSRData(
        request: LynxResourceRequest,
        callback: LynxResourceCallback<ByteArray>,
    ) {
        notifyResourceLoadStart(request.url)
        val wrappedCallback =
            onceCallback(request.url, callback) { response ->
                response.data
            }
        runCatching {
            delegate.fetchSSRData(request, wrappedCallback)
        }.onFailure {
            wrappedCallback.onResponse(failedResponse(it))
        }
    }

    private fun <T> onceCallback(
        url: String,
        callback: LynxResourceCallback<T>,
        templateProvider: (LynxResourceResponse<T>) -> ByteArray?,
    ): LynxResourceCallback<T> {
        val completed = AtomicBoolean(false)
        return LynxResourceCallback { response ->
            if (!completed.compareAndSet(false, true)) {
                return@LynxResourceCallback
            }
            val error =
                if (response.state == LynxResourceResponse.ResponseState.FAILED) {
                    HybridKitError().apply {
                        errorCode = HybridErrorConstantCode.ResourceLoadError
                        errorReason = "ResourceLoadFailed"
                        originReason = response.error?.message
                    }
                } else {
                    null
                }
            notifyResourceLoadFinish(url, templateProvider(response), error)
            callback.onResponse(response)
        }
    }

    private fun notifyResourceLoadStart(url: String) {
        val kitView = kitViewProvider()?.takeIf { !it.hasDestroyed() } ?: return
        lifeCycle?.onResourceLoadStart(kitView, url)
    }

    private fun notifyResourceLoadFinish(
        url: String,
        template: ByteArray?,
        error: HybridKitError?,
    ) {
        val kitView = kitViewProvider()?.takeIf { !it.hasDestroyed() } ?: return
        lifeCycle?.onResourceLoadFinish(kitView, url, template, error)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> failedResponse(error: Throwable): LynxResourceResponse<T> = LynxResourceResponse.onFailed(error) as LynxResourceResponse<T>
}
