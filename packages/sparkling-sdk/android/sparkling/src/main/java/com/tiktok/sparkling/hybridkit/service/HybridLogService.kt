// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.service

import android.util.Log
import com.tiktok.sparkling.hybridkit.utils.HybridLogger
import com.tiktok.sparkling.hybridkit.utils.LogLevel

class HybridLogService : HybridLogger {
    override fun onLog(
        msg: String,
        logLevel: LogLevel,
        tag: String,
    ) {
        when (logLevel) {
            LogLevel.D -> {
                Log.d(tag, msg)
            }

            LogLevel.V -> {
                Log.v(tag, msg)
            }

            LogLevel.I -> {
                Log.i(tag, msg)
            }

            LogLevel.W -> {
                Log.w(tag, msg)
            }

            LogLevel.E -> {
                Log.e(tag, msg)
            }
        }
    }

    override fun onReject(
        e: Throwable,
        extraMsg: String,
        tag: String,
    ) {
        Log.e(tag, extraMsg)
    }
}
