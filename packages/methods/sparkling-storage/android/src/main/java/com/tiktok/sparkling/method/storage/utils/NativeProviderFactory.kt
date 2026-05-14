// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.storage.utils

import android.content.Context

object NativeProviderFactory {
    /**
     * @param context Context
     * @return INativeStorage
     */
    @JvmStatic
    fun providerNativeStorage(context: Context): INativeStorage = NativeStorageImpl.Companion.getInstance(context.applicationContext)
}
