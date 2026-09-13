// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.router.setBackPressIntercept

import android.util.Log
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.model.idl.CompletionBlock
import com.tiktok.sparkling.method.registry.core.utils.createXModel
import com.tiktok.sparkling.method.router.utils.RouterProvider

/**
 * Let the page handle the hardware back button.
 *
 * While a container is intercepting, a back press is delivered to it as the
 * `onBackPress` event and the container does nothing else - closing is then the
 * page's decision, made with `router.close`. Turning interception off restores
 * the container's own behaviour.
 *
 * `containerID` defaults to the calling container, which is what a page wants;
 * naming another one is for a host that manages several.
 */
class RouterSetBackPressInterceptMethod : AbsRouterSetBackPressInterceptMethodIDL() {
    override fun handle(
        params: IDLMethodSetBackPressInterceptParamModel,
        callback: CompletionBlock<IDLMethodSetBackPressInterceptResultModel>,
        type: BridgePlatformType,
    ) {
        val routerDepend = RouterProvider.hostRouterDepend
        if (routerDepend == null) {
            Log.e(TAG, "Router dependency not registered")
            callback.onFailure(IDLBridgeMethod.FAIL, "Router service not available", null)
            return
        }

        val containerId = params.containerID?.takeIf { it.isNotBlank() } ?: getSDKContext()?.containerID
        val supported =
            try {
                routerDepend.setBackPressIntercept(getSDKContext(), containerId, params.intercept)
            } catch (e: Exception) {
                Log.e(TAG, "Exception while setting back press intercept: ${e.message}")
                false
            }

        val result = IDLMethodSetBackPressInterceptResultModel::class.java.createXModel()
        result.supported = supported
        callback.onSuccess(result)
    }

    companion object {
        private const val TAG = "RouterSetBackPressInterceptMethod"
    }
}
