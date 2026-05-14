// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import android.content.Context
import android.view.View
import androidx.appcompat.widget.Toolbar
import com.tiktok.sparkling.hybridkit.HybridContext

interface SparklingUIProvider {
    fun getLoadingView(context: Context): View

    fun getErrorView(context: Context): View

    fun getToolBar(context: Context): Toolbar?
}

class SparklingContext : HybridContext() {
    var sparklingUIProvider: SparklingUIProvider? = null
}
