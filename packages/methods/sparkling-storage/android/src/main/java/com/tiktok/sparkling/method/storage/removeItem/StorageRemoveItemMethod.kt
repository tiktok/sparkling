// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.storage.removeItem

import android.content.Context
import com.tiktok.sparkling.method.storage.utils.NativeProviderFactory
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.model.idl.CompletionBlock
import com.tiktok.sparkling.method.registry.core.utils.createXModel
import com.tiktok.sparkling.method.runtime.depend.BridgeBaseRuntime
import com.tiktok.sparkling.method.storage.setItem.STORAGE_ITEM_TIME_SUFFIX

class StorageRemoveItemMethod : AbsStorageRemoveItemMethodIDL() {
    override fun handle(
        params: IDLMethodRemoveStorageItemParamModel,
        callback: CompletionBlock<IDLMethodRemoveStorageItemResultModel>,
        type: BridgePlatformType,
    ) {
        val context = BridgeBaseRuntime.applicationContext
        context ?: return callback.onFailure(IDLBridgeMethod.FAIL, "Context not provided in host")

        val key =
            params.key.ifEmpty {
                return callback.onFailure(IDLBridgeMethod.INVALID_PARAM, "Key in the params is empty")
            }
        val biz = params.biz ?: ""
        val success = removeStorage(context, biz, key)

        callback.onSuccess(IDLMethodRemoveStorageItemResultModel::class.java.createXModel().apply {})
    }

    companion object {
        /**
         * @param context Context
         * @param biz String?
         * @param key String?
         * @return Boolean
         */
        fun removeStorage(
            context: Context,
            biz: String?,
            key: String?,
        ): Boolean =
            NativeProviderFactory.providerNativeStorage(context).tryRemoveBizStorageItem(biz, key) &&
                // try to remove time
                NativeProviderFactory
                    .providerNativeStorage(context)
                    .tryRemoveBizStorageItem(biz, key + STORAGE_ITEM_TIME_SUFFIX)
    }
}
