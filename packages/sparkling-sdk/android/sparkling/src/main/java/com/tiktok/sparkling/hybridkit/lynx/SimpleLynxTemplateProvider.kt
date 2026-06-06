// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.lynx

import com.lynx.tasm.behavior.LynxContext
import com.lynx.tasm.provider.AbsTemplateProvider
import com.tiktok.sparkling.hybridkit.base.HybridErrorConstantCode
import com.tiktok.sparkling.hybridkit.base.HybridKitError
import com.tiktok.sparkling.hybridkit.base.IHybridKitLifeCycle
import com.tiktok.sparkling.hybridkit.base.IKitView

internal class SimpleLynxTemplateProvider(
    private val delegate: AbsTemplateProvider,
    private val lifeCycle: IHybridKitLifeCycle?,
    private val kitViewProvider: () -> IKitView?,
) : AbsTemplateProvider() {
    override fun loadTemplate(
        url: String,
        callback: Callback,
    ) {
        loadTemplateInternal(url, callback) { wrappedCallback ->
            delegate.loadTemplate(url, wrappedCallback)
        }
    }

    override fun loadTemplate(
        url: String,
        callback: Callback,
        context: LynxContext,
    ) {
        loadTemplateInternal(url, callback) { wrappedCallback ->
            delegate.loadTemplate(url, wrappedCallback, context)
        }
    }

    private fun loadTemplateInternal(
        url: String,
        callback: Callback,
        loadTemplate: (Callback) -> Unit,
    ) {
        notifyResourceLoadStart(url)
        loadTemplate(
            object : Callback {
                override fun onSuccess(template: ByteArray?) {
                    notifyResourceLoadFinish(url, template, null)
                    callback.onSuccess(template)
                }

                override fun onFailed(msg: String?) {
                    notifyResourceLoadFinish(
                        url,
                        null,
                        HybridKitError().apply {
                            errorCode = HybridErrorConstantCode.ResourceLoadError
                            errorReason = "ResourceLoadFailed"
                            originReason = msg
                        },
                    )
                    callback.onFailed(msg)
                }
            },
        )
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
}
