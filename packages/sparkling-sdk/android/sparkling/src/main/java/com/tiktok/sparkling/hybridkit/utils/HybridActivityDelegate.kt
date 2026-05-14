// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.utils

import android.app.Activity
import java.lang.ref.WeakReference

object HybridActivityDelegate {
    private var topActivityRef: WeakReference<Activity>? = null

    fun getTopActivity(): Activity? = topActivityRef?.get()

    fun setTopActivity(activity: Activity) {
        topActivityRef = WeakReference(activity)
    }
}
