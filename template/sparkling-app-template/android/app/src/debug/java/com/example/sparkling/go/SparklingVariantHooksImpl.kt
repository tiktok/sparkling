// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.example.sparkling.go

import android.app.Activity
import android.app.Application
import com.tiktok.sparkling.SparklingContext
import com.tiktok.sparkling.debugtool.SparklingDebugTool

/**
 * Debug-only host wiring. The debug tool itself reflectively wires up the
 * KitView lifecycle, GlobalProps provider, and method-invocation observer,
 * so the host only needs to call [SparklingDebugTool.init].
 */
class SparklingVariantHooksImpl : SparklingVariantHooks {
    override fun onApplicationCreate(application: Application) {
        SparklingDebugTool.init(
            application,
            SparklingDebugTool.Config(
                enableJsConsole = true,
                enableFloatingBall = true,
            ),
        )
    }

    override fun createMainContext(
        activity: Activity,
        initialDataJson: String,
    ): SparklingContext {
        val debugScheme = DebugDevUrlSupport.buildMainPageScheme(activity)
        return SparklingContext().apply {
            scheme = debugScheme
            sparklingUIProvider = DebugSparklingUiProvider(initialDataJson, debugScheme)
            withInitData(initialDataJson)
        }
    }
}
