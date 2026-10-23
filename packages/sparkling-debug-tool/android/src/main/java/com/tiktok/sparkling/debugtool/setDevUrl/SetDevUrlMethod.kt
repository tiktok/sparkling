// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool.setDevUrl

import android.content.Context
import com.tiktok.sparkling.debugtool.DEV_URL_ERROR_MESSAGE
import com.tiktok.sparkling.debugtool.SparklingDebugTool
import com.tiktok.sparkling.debugtool.normalizedDevUrl
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.model.idl.CompletionBlock
import com.tiktok.sparkling.method.registry.core.utils.createXModel

class SetDevUrlMethod(
    private val context: Context,
) : AbsSetDevUrlMethodIDL() {
    override fun handle(
        params: IDLMethodSetDevUrlParamModel,
        callback: CompletionBlock<IDLMethodSetDevUrlResultModel>,
        type: BridgePlatformType,
    ) {
        val url =
            normalizedDevUrl(params.url)
                ?: return callback.onFailure(IDLBridgeMethod.INVALID_PARAM, DEV_URL_ERROR_MESSAGE)

        SparklingDebugTool.setDevUrl(context, url)
        callback.onSuccess(
            IDLMethodSetDevUrlResultModel::class.java.createXModel(getSDKContext()?.containerID),
        )
    }
}
