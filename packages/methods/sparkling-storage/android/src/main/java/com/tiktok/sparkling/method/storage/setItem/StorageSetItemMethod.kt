// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.storage.setItem

import android.util.Log
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.model.idl.CompletionBlock
import com.tiktok.sparkling.method.registry.core.utils.createXModel
import com.tiktok.sparkling.method.runtime.depend.BridgeBaseRuntime
import com.tiktok.sparkling.method.storage.removeItem.StorageRemoveItemMethod
import com.tiktok.sparkling.method.storage.utils.NativeProviderFactory
import java.util.Date

const val STORAGE_ITEM_TIME_SUFFIX = "_TIME"

/**
 */
class StorageSetItemMethod : AbsStorageSetItemMethodIDL() {
    override fun handle(
        params: IDLMethodSetStorageItemParamModel,
        callback: CompletionBlock<IDLMethodSetStorageItemResultModel>,
        type: BridgePlatformType,
    ) {
        val context = BridgeBaseRuntime.applicationContext
        context ?: return callback.onFailure(
            0,
            "Context not provided in host",
        )

        val key =
            params.key.ifEmpty {
                return callback.onFailure(IDLBridgeMethod.INVALID_PARAM, "Key in the params is empty")
            }
        val data = params.data
        val biz = params.biz ?: ""
        val validDuration = params.validDuration
        var success =
            when {
                data is Boolean -> {
                    NativeProviderFactory
                        .providerNativeStorage(context)
                        .trySetBizStorageItem(biz, key, data as? Boolean)
                }

                data is Int -> {
                    NativeProviderFactory
                        .providerNativeStorage(context)
                        .trySetBizStorageItem(biz, key, data as? Int)
                }

                data is String -> {
                    NativeProviderFactory
                        .providerNativeStorage(context)
                        .trySetBizStorageItem(biz, key, data as? String)
                }

                data is Number -> {
                    NativeProviderFactory
                        .providerNativeStorage(context)
                        .trySetBizStorageItem(biz, key, data.toDouble())
                }

                data is List<*> -> {
                    NativeProviderFactory
                        .providerNativeStorage(context)
                        .trySetBizStorageItem(biz, key, data as? List<*>)
                }

                data is Map<*, *> -> {
                    NativeProviderFactory
                        .providerNativeStorage(context)
                        .trySetBizStorageItem(biz, key, data as? Map<*, *>)
                }

                else -> {
                    false
                }
            }
        if (success && validDuration != null) {
            try {
                success =
                    NativeProviderFactory
                        .providerNativeStorage(context)
                        .trySetBizStorageItem(biz, key + STORAGE_ITEM_TIME_SUFFIX, Date(System.currentTimeMillis() + (validDuration.toDouble() * 1000).toLong()).time.toString())
            } catch (e: Exception) {
                e.message?.let { Log.e(TAG, it) }
                StorageRemoveItemMethod.Companion.removeStorage(context, biz, key)
                success = false
            }
        }

        if (success) {
            callback.onSuccess(
                IDLMethodSetStorageItemResultModel::class.java.createXModel().apply {},
            )
        } else {
            callback.onFailure(IDLBridgeMethod.INVALID_PARAM, "Illegal value type")
        }
    }

    companion object {
        const val TAG = "XSetStorageItemMethod"
    }
}
