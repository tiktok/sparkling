// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.router.close

import android.util.Log
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.model.idl.CompletionBlock
import com.tiktok.sparkling.method.registry.core.utils.createXModel
import com.tiktok.sparkling.method.router.utils.IHostRouterDepend
import com.tiktok.sparkling.method.router.utils.RouterProvider

/**
 * Router close method implementation.
 * Handles closing pages/containers.
 */
class RouterCloseMethod : AbsRouterCloseMethodIDL() {
    companion object {
        private const val TAG = "RouterCloseMethod"
    }

    private fun getRouterDependInstance(): IHostRouterDepend? = RouterProvider.hostRouterDepend

    override fun handle(
        params: IDLMethodCloseParamModel,
        callback: CompletionBlock<IDLMethodCloseResultModel>,
        type: BridgePlatformType,
    ) {
        // Check if router dependency is available
        val routerDepend = getRouterDependInstance()
        if (routerDepend == null) {
            Log.e(TAG, "Router dependency not registered")
            callback.onFailure(IDLBridgeMethod.FAIL, "Router service not available", null)
            return
        }

        val containerID = params.containerID
        val animated = params.animated ?: true // Default to animated close

        val success =
            try {
                routerDepend.closeView(getSDKContext(), type, containerID, animated)
            } catch (e: Exception) {
                Log.e(TAG, "Exception while closing container: ${e.message}")
                false
            }

        if (success) {
            callback.onSuccess(IDLMethodCloseResultModel::class.java.createXModel())
        } else {
            val errorMsg =
                if (containerID.isNullOrBlank()) {
                    "Failed to close current container"
                } else {
                    "Failed to close container: $containerID"
                }
            callback.onFailure(IDLBridgeMethod.FAIL, errorMsg, null)
        }
    }
}
