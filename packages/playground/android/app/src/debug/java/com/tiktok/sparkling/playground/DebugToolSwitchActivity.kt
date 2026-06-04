// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.playground

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.tiktok.sparkling.debugtool.SparklingDebugTool

class DebugToolSwitchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container =
            androidx.fragment.app.FragmentContainerView(this).apply {
                id = View.generateViewId()
            }
        setContentView(container)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(container.id, SparklingDebugTool.createFragment())
                .commit()
        }
    }
}
