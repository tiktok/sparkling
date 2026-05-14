// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.hybridkit.utils

enum class LogLevel {
    D,
    V,
    I,
    W,
    E,
}

interface HybridLogger {
    fun onLog(
        msg: String,
        logLevel: LogLevel,
        tag: String,
    )

    fun onReject(
        e: Throwable,
        extraMsg: String = "",
        tag: String,
    )
}
