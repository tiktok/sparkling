// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.hybridkit.utils

import android.net.Uri
import android.util.Log

fun String?.safeToUri(): Uri =
    try {
        Uri.parse(this)
    } catch (e: Exception) {
        // ignore exception
        Uri.EMPTY
    }

fun Uri.safeGetQueryParameter(key: String): String? {
    return try {
        this.getQueryParameter(key)
    } catch (e: Throwable) {
        Log.e("Sparkling", "uri get query parameter error", e)
        return null
    }
}
