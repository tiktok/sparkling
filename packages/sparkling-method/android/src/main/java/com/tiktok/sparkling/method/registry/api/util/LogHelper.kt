// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.api.util

import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

const val TAG = "SparklingBridge"

internal fun Any?.log() {
    when (this) {
        is Map<*, *> -> {
        }

        else -> {
            Log.d(TAG, this.toString())
        }
    }
}

internal fun Throwable.printStackString(): String {
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    this.printStackTrace(pw)
    pw.flush()
    return sw.toString()
}
