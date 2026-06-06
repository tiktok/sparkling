// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.example.sparkling.go

import android.app.Activity
import android.app.Application
import com.tiktok.sparkling.SparklingContext

class SparklingVariantHooksImpl : SparklingVariantHooks {
    override fun onApplicationCreate(application: Application) = Unit

    override fun createMainContext(
        activity: Activity,
        initialDataJson: String,
    ): SparklingContext =
        SparklingContext().apply {
            scheme = "hybrid://lynxview_page?bundle=main.lynx.bundle&hide_nav_bar=1&screen_orientation=portrait"
            withInitData(initialDataJson)
        }
}
