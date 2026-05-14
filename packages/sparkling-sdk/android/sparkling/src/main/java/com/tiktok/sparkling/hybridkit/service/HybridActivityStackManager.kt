// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.hybridkit.service

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.tiktok.sparkling.hybridkit.utils.HybridActivityDelegate

object HybridActivityStackManager {
    private var resumeCount = 0

    fun getTopActivity(): Activity? = HybridActivityDelegate.getTopActivity()

    fun isBackground(): Boolean = resumeCount == 0

    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(
                    activity: Activity,
                    savedInstanceState: Bundle?,
                ) {
                }

                override fun onActivityStarted(activity: Activity) {
                }

                override fun onActivityResumed(activity: Activity) {
                    HybridActivityDelegate.setTopActivity(activity)
                    resumeCount++
//                ViewEventUtils.onForeground()
                }

                override fun onActivityPaused(activity: Activity) {
                    resumeCount--
//                if (resumeCount == 0)
//                    ViewEventUtils.onBackground()
                }

                override fun onActivityStopped(activity: Activity) {
                }

                override fun onActivitySaveInstanceState(
                    activity: Activity,
                    outState: Bundle,
                ) {
                }

                override fun onActivityDestroyed(activity: Activity) {
                    // detect memory leak
                }
            },
        )
    }
}
