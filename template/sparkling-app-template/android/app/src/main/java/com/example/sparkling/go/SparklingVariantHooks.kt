// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.example.sparkling.go

import android.app.Activity
import android.app.Application
import com.tiktok.sparkling.SparklingContext

interface SparklingVariantHooks {
    fun onApplicationCreate(application: Application)

    fun createMainContext(
        activity: Activity,
        initialDataJson: String,
    ): SparklingContext
}

internal fun createSparklingVariantHooks(): SparklingVariantHooks = SparklingVariantHooksImpl()
