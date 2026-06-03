// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.storage.getItem

import android.util.Log
import com.lynx.react.bridge.PiperData
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.model.idl.CompletionBlock
import com.tiktok.sparkling.method.registry.core.utils.createXModel
import com.tiktok.sparkling.method.runtime.depend.BridgeBaseRuntime
import com.tiktok.sparkling.method.storage.setItem.STORAGE_ITEM_TIME_SUFFIX
import com.tiktok.sparkling.method.storage.removeItem.StorageRemoveItemMethod
import com.tiktok.sparkling.method.storage.utils.NativeProviderFactory
import java.util.Date

class StorageGetItemMethod : AbsStorageGetItemMethodIDL() {
    override fun handle(
        params: IDLMethodGetStorageItemParamModel,
        callback: CompletionBlock<IDLMethodGetStorageItemResultModel>,
        type: BridgePlatformType,
    ) {
        val context = BridgeBaseRuntime.applicationContext
        context ?: return callback.onFailure(IDLBridgeMethod.FAIL, "Context not provided in host")

        val key =
            params.key.ifEmpty {
                return callback.onFailure(IDLBridgeMethod.INVALID_PARAM, "Key in the params is empty")
            }
        val biz = params.biz ?: ""
        try {
            var value =
                NativeProviderFactory.providerNativeStorage(context).tryGetBizStorageItem(biz, key)

            val validTime =
                NativeProviderFactory
                    .providerNativeStorage(context)
                    .tryGetBizStorageItem(biz, key + STORAGE_ITEM_TIME_SUFFIX)
            if (validTime is String && !checkStorageAvailable(validTime)) {
                value = null
                StorageRemoveItemMethod.Companion.removeStorage(context, biz, key)
            }

            val isPiperData = true
            callback.onSuccess(
                IDLMethodGetStorageItemResultModel::class.java.createXModel().apply {
                    this.data =
                        value.let {
                            if (isPiperData && it !is String) {
                                PiperData.createDisposableFromObject(it)
                            } else {
                                it
                            }
                        }
                },
            )
        } catch (e: Exception) {
            Log.e("XGetStorageItemMethod", "failed to properly getStorageItem with exception $e")
            callback.onFailure(IDLBridgeMethod.FAIL, "failed to properly getStorageItem with exception $e")
        }
    }

    private fun checkStorageAvailable(time: String?): Boolean {
        if (time == null) {
            return true
        }
        return try {
            val currentDate = Date()
            val validDate = Date(time.toLong())
            // check
            currentDate.before(validDate)
        } catch (e: Exception) {
            Log.e(TAG, "checkStorageAvailable: ${e.message}")
            false
        }
    }

    companion object {
        const val TAG = "XGetStorageItemMethod"
    }
}
