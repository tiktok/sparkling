// Copyright (c) 2023 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.


package com.tiktok.sparkling.method.router.open


import android.util.Log
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.model.idl.CompletionBlock
import com.tiktok.sparkling.method.registry.core.utils.createXModel
import com.tiktok.sparkling.method.router.utils.IHostRouterDepend
import com.tiktok.sparkling.method.router.utils.RouterProvider


/**
 * Router open method implementation.
 * Handles opening new pages/routes with various configuration options.
 */
class RouterOpenMethod : AbsRouterOpenMethodIDL() {

    companion object {
        private const val TAG = "RouterOpenMethod"
    }

    private fun getRouterDependInstance(): IHostRouterDepend? {
        return RouterProvider.hostRouterDepend
    }

    override fun handle(
        params: IDLMethodOpenParamModel,
        callback: CompletionBlock<IDLMethodOpenResultModel>,
        type: BridgePlatformType
    ) {
        // Validate scheme is not null or empty
        val scheme = params.scheme
        if (scheme.isNullOrBlank()) {
            Log.w(TAG, "Invalid params: scheme is null or empty")
            callback.onFailure(IDLBridgeMethod.INVALID_PARAM, "Invalid params: scheme must be a non-empty string", null)
            return
        }

        // Validate replaceType if provided
        val replaceType: ReplaceType? = if (!params.replaceType.isNullOrBlank()) {
            try {
                ReplaceType.valueOf(params.replaceType!!)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Invalid replaceType: ${params.replaceType}")
                callback.onFailure(
                    IDLBridgeMethod.INVALID_PARAM,
                    "Invalid replaceType: ${params.replaceType}. Valid values are: ${ReplaceType.values().joinToString()}",
                    null
                )
                return
            }
        } else {
            ReplaceType.onlyCloseAfterOpenSucceed
        }

        // Get and validate context
        val context = getSDKContext()?.context
        if (context == null) {
            Log.e(TAG, "Context not provided in host")
            callback.onFailure(IDLBridgeMethod.FAIL, "Context not provided in host", null)
            return
        }

        // Check if router dependency is available
        val routerDepend = getRouterDependInstance()
        if (routerDepend == null) {
            Log.e(TAG, "Router dependency not registered")
            callback.onFailure(IDLBridgeMethod.FAIL, "Router service not available", null)
            return
        }

        val replace = params.replace ?: false
        val useSysBrowser = params.useSysBrowser ?: false
        val extra = params.extra

        val extraInfo = mutableMapOf<String, Any>(
            "useSysBrowser" to useSysBrowser,
            "extra" to (extra ?: emptyMap<Any, Any>())
        )

        // Handle non-replace open
        if (!replace) {
            val success = try {
                routerDepend.openScheme(getSDKContext(), scheme, extraInfo, type, context = context)
            } catch (e: Exception) {
                Log.e(TAG, "Exception while opening scheme: ${e.message}")
                false
            }

            if (success) {
                callback.onSuccess(IDLMethodOpenResultModel::class.java.createXModel())
            } else {
                callback.onFailure(IDLBridgeMethod.FAIL, "Failed to open scheme: $scheme", null)
            }
            return
        }

        // Handle replace open with different replace types
        val success: Boolean = try {
            when (replaceType) {
                ReplaceType.alwaysCloseBeforeOpen -> {
                    routerDepend.closeView(getSDKContext(), type)
                    routerDepend.openScheme(getSDKContext(), scheme, extraInfo, type, context = context)
                }
                ReplaceType.alwaysCloseAfterOpen -> {
                    val opened = routerDepend.openScheme(getSDKContext(), scheme, extraInfo, type, context = context)
                    routerDepend.closeView(getSDKContext(), type)
                    opened
                }
                ReplaceType.onlyCloseAfterOpenSucceed -> {
                    val opened = routerDepend.openScheme(getSDKContext(), scheme, extraInfo, type, context = context)
                    if (opened) {
                        routerDepend.closeView(getSDKContext(), type)
                    }
                    opened
                }
                null -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during replace open: ${e.message}")
            false
        }

        if (success) {
            callback.onSuccess(IDLMethodOpenResultModel::class.java.createXModel())
        } else {
            callback.onFailure(IDLBridgeMethod.FAIL, "Failed to open scheme: $scheme", null)
        }
    }
}
enum class ReplaceType {
    alwaysCloseAfterOpen,
    alwaysCloseBeforeOpen,
    onlyCloseAfterOpenSucceed
}
