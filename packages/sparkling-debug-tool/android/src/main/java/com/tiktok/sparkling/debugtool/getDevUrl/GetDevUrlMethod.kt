// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool.getDevUrl

import android.content.Context
import com.tiktok.sparkling.debugtool.SparklingDebugTool
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.model.idl.CompletionBlock
import com.tiktok.sparkling.method.registry.core.utils.createXModel

class GetDevUrlMethod(
    private val context: Context,
) : AbsGetDevUrlMethodIDL() {
    override fun handle(
        params: IDLMethodGetDevUrlParamModel,
        callback: CompletionBlock<IDLMethodGetDevUrlResultModel>,
        type: BridgePlatformType,
    ) {
        callback.onSuccess(
            IDLMethodGetDevUrlResultModel::class.java.createXModel(getSDKContext()?.containerID).apply {
                url = SparklingDebugTool.getDevUrl(context, "")
            },
        )
    }
}
