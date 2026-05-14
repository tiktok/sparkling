// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.utils

import android.util.Log
import com.tiktok.sparkling.method.registry.api.SparklingBridge

object LogUtils {
    fun e(
        tag: String,
        msg: String,
    ) {
        if (SparklingBridge.isDebugEnv) {
            Log.e(tag, msg)
        }
    }

    fun d(
        tag: String,
        msg: String,
    ) {
        if (SparklingBridge.isDebugEnv) {
            Log.d(tag, msg)
        }
    }

    fun w(
        tag: String,
        msg: String,
    ) {
        if (SparklingBridge.isDebugEnv) {
            Log.w(tag, msg)
        }
    }

    fun i(
        tag: String,
        msg: String,
    ) {
        if (SparklingBridge.isDebugEnv) {
            Log.i(tag, msg)
        }
    }

    fun v(
        tag: String,
        msg: String,
    ) {
        if (SparklingBridge.isDebugEnv) {
            Log.v(tag, msg)
        }
    }
}
