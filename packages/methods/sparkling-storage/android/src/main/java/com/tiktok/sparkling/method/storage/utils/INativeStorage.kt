// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.storage.utils

interface INativeStorage {
    fun setStorageItem(
        key: String?,
        data: Any?,
    ): Boolean

    fun getStorageItem(key: String?): Any?

    fun removeStorageItem(key: String?): Boolean

    fun getStorageInfo(): Set<String>

    /**
     * @param biz String?
     * @param key String?
     * @return Any?
     */
    fun tryGetBizStorageItem(
        biz: String?,
        key: String?,
    ): Any? {
        if (!biz.isNullOrEmpty() && (this is IBizNativeStorage)) {
            return getBizStorageItem(biz, key)
        } else {
            return getStorageItem(key)
        }
    }

    /**
     * @param biz String?
     * @param key String?
     * @return Boolean
     */
    fun tryRemoveBizStorageItem(
        biz: String?,
        key: String?,
    ): Boolean {
        if (!biz.isNullOrEmpty() && (this is IBizNativeStorage)) {
            return removeBizStorageItem(biz, key)
        } else {
            return removeStorageItem(key)
        }
    }

    /**
     * @param biz String?
     * @param key String?
     * @param data Any?
     * @return Boolean
     */
    fun trySetBizStorageItem(
        biz: String?,
        key: String?,
        data: Any?,
    ): Boolean {
        if (!biz.isNullOrEmpty() && (this is IBizNativeStorage)) {
            return setBizStorageItem(biz, key, data)
        } else {
            return setStorageItem(key, data)
        }
    }
}
