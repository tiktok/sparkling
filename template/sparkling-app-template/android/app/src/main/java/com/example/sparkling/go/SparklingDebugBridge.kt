// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.example.sparkling.go

import android.app.Activity
import android.app.Application
import android.content.Context
import kotlin.jvm.functions.Function1

object SparklingDebugBridge {
    private const val DEBUG_TOOL_CLASS = "com.tiktok.sparkling.debugtool.SparklingDebugTool"

    fun init(application: Application) {
        runCatching {
            val debugToolClass = Class.forName(DEBUG_TOOL_CLASS)
            val initMethod = debugToolClass.getMethod("init", Application::class.java)
            initMethod.invoke(null, application)
        }
    }

    fun getDevUrl(context: Context, fallback: String): String {
        return runCatching {
            val debugToolClass = Class.forName(DEBUG_TOOL_CLASS)
            val getDevUrlMethod = debugToolClass.getMethod(
                "getDevUrl",
                Context::class.java,
                String::class.java,
            )
            getDevUrlMethod.invoke(null, context, fallback) as? String ?: fallback
        }.getOrDefault(fallback)
    }

    fun showDevUrlDialog(
        activity: Activity,
        initialUrl: String?,
        onSaved: (String) -> Unit,
    ) {
        runCatching {
            val debugToolClass = Class.forName(DEBUG_TOOL_CLASS)
            val showDevUrlDialogMethod = debugToolClass.getMethod(
                "showDevUrlDialog",
                Activity::class.java,
                String::class.java,
                Function1::class.java,
            )
            val callback = object : Function1<String, Unit> {
                override fun invoke(updatedUrl: String) {
                    onSaved(updatedUrl)
                }
            }
            showDevUrlDialogMethod.invoke(null, activity, initialUrl, callback)
        }
    }
}
