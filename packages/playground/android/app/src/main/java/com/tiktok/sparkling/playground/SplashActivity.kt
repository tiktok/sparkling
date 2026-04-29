// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.playground

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.tiktok.sparkling.Sparkling
import com.tiktok.sparkling.SparklingContext
import com.tiktok.sparkling.method.registry.core.utils.JsonUtils

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        gotoSparklingPage()
    }

    private fun gotoSparklingPage() {
        val initData = mapOf<Any, Any>()
        val initialData: String = JsonUtils.toJson(initData)
        val initialDataJson = "{ \"initial_data\":$initialData}"

        val context = SparklingContext()
        context.scheme = if (BuildConfig.DEBUG) {
            val debugScheme = DebugDevUrlSupport.buildMainPageScheme(this)
            context.sparklingUIProvider = DebugSparklingUiProvider(initialDataJson, debugScheme)
            debugScheme
        } else {
            "hybrid://lynxview_page?bundle=main.lynx.bundle&hide_nav_bar=1&screen_orientation=portrait"
        }
        context.withInitData(initialDataJson)
        Sparkling.build(this, context).navigate()
        finish()
    }
}
