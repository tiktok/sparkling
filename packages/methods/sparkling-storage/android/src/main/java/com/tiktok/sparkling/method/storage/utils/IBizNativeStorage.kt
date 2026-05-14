// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.storage.utils

/**
 * Desc:
 */
interface IBizNativeStorage : INativeStorage {
    fun setBizStorageItem(
        biz: String,
        key: String?,
        data: Any?,
    ): Boolean

    fun getBizStorageItem(
        biz: String,
        key: String?,
    ): Any?

    fun removeBizStorageItem(
        biz: String,
        key: String?,
    ): Boolean

    fun getBizStorageInfo(biz: String): Set<String>
}
