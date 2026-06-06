// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.playground

import android.app.Application
import com.tiktok.sparkling.debugtool.SparklingDebugTool
import com.tiktok.sparkling.hybridkit.config.LogConfig
import com.tiktok.sparkling.hybridkit.config.SparklingHybridConfig
import com.tiktok.sparkling.hybridkit.utils.HybridLogger
import com.tiktok.sparkling.hybridkit.utils.LogLevel

/**
 * Debug-only host wiring. The debug tool now auto-installs the JS-console
 * sink, GlobalProps provider, and Sparkling Method observer when
 * [SparklingDebugTool.init] runs with `enableFloatingBall = true`, so the
 * host only needs to forward host-side logs (HybridLogger) into the console
 * store.
 */
class PlaygroundDebugHooksImpl : PlaygroundDebugHooks {
    override fun onApplicationCreate(application: Application) {
        SparklingDebugTool.init(
            application,
            SparklingDebugTool.Config(
                enableJsConsole = true,
                enableFloatingBall = true,
            ),
        )
    }

    override fun configureHybridConfig(builder: SparklingHybridConfig.Builder) {
        builder.setLogConfig(
            LogConfig(
                object : HybridLogger {
                    override fun onLog(
                        msg: String,
                        logLevel: LogLevel,
                        tag: String,
                    ) {
                        SparklingDebugTool.recordConsoleLine(logLevel.name, tag, msg)
                    }

                    override fun onReject(
                        e: Throwable,
                        extraMsg: String,
                        tag: String,
                    ) {
                        val suffix = if (extraMsg.isBlank()) e.message.orEmpty() else "$extraMsg ${e.message.orEmpty()}".trim()
                        SparklingDebugTool.recordConsoleLine("E", tag, suffix)
                    }
                },
            ),
        )
    }
}
