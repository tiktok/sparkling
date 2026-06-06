// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.playground

import android.app.Application
import com.tiktok.sparkling.hybridkit.config.SparklingHybridConfig

class PlaygroundDebugHooksImpl : PlaygroundDebugHooks {
    override fun onApplicationCreate(application: Application) = Unit

    override fun configureHybridConfig(builder: SparklingHybridConfig.Builder) = Unit
}
