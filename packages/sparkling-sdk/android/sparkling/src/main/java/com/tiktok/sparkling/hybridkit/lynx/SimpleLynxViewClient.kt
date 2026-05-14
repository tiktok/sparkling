// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.lynx

import android.net.Uri
import com.lynx.tasm.LynxError
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

    override fun onPageStart(url: String?) {
        super.onPageStart(url)
        uri = url?.toUri()
    }

    override fun onRuntimeReady() {
        super.onRuntimeReady()
        hybridLifeCycle?.onRuntimeReady(HybridKitType.LYNX)
    }

    override fun onReceivedError(error: LynxError?) {
        super.onReceivedError(error)
        if (error != null && error.isFatal() && uri != null) {
            hybridLifeCycle?.onLoadFailed(
                kitView,
                uri.toString(),
                HybridKitError().apply {
                    errorCode = HybridErrorConstantCode.LynxLoadError
                    errorReason = "LynxReceiveError"
                    originCode = error.errorCode
                    originReason = error.msg
                },
            )
        }
    }

    override fun onLynxViewAndJSRuntimeDestroy() {
        super.onLynxViewAndJSRuntimeDestroy()
        kitView.destroyWhenJSRuntimeCallback()
    }
}
