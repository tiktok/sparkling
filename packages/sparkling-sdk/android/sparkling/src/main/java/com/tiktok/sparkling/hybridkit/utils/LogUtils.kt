// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.hybridkit.utils

import android.util.Log

val TAG = "HybridKit"

object LogUtils {
    var logger: HybridLogger? = null

    fun printLog(
        msg: String,
        logLevel: LogLevel = LogLevel.I,
        tag: String = "",
    ) {
        runCatching {
            AsyncUtils.submit(TaskType.Sequence) {
                if (logger == null) {
                    when (logLevel) {
                        LogLevel.D -> {
                            Log.d("${TAG}_$tag", "onLog: $msg")
                        }

                        LogLevel.E -> {
                            Log.e("${TAG}_$tag", "onLog: $msg")
                        }

                        LogLevel.W -> {
                            Log.w("${TAG}_$tag", "onLog: $msg")
                        }

                        LogLevel.V -> {
                            Log.v("${TAG}_$tag", "onLog: $msg")
                        }

                        else -> {
                            Log.i("${TAG}_$tag", "onLog: $msg")
                        }
                    }
                } else {
                    logger?.onLog(msg, logLevel, "${TAG}_$tag")
                }
            }
        }
    }

    fun printReject(
        e: Throwable,
        extraMsg: String = "",
        tag: String = TAG,
    ) {
        runCatching {
            AsyncUtils.submit(TaskType.Sequence) {
                if (logger == null) {
                    Log.e("${TAG}_$tag", "onReject: ${e.message}")
                } else {
                    logger?.onReject(e, extraMsg, "${TAG}_$tag")
                }
            }
        }
    }
}
