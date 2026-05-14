// Copyright 2025 TikTok Inc.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.media.depend

import android.content.Context

interface IHostMediaDepend {
    /**
     * @param context Context
     * @param params ChooseMediaParams
     * @param callback IChooseMediaResultCallback
     */
    fun handleJsInvoke(
        context: Context,
        params: ChooseMediaParams,
        callback: IChooseMediaResultCallback,
    )
}

/**
 */
interface IChooseMediaResultCallback {
    /**
     * @param result ChooseMediaResults
     * @param msg String
     */
    fun onSuccess(
        result: ChooseMediaResults,
        msg: String = "",
    )

    /**
     * @param code Int
     * @param msg String
     */
    fun onFailure(
        code: Int,
        msg: String = "",
    )
}
