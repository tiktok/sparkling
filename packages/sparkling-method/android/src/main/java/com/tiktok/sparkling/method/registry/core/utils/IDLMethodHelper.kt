// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log

/**
 * Desc:
 */
object IDLMethodHelper {
    /**
     * try to get Activity by bubble up through base context.
     *
     * @param c
     * @return
     */
    fun getActivity(c: Context?): Activity? {
        var c = c
        while (c != null) {
            c =
                if (c is Activity) {
                    return c
                } else if (c is ContextWrapper) {
                    c.baseContext
                } else {
                    Log.w(
                        "ViewUtils",
                        "find non-ContextWrapper in view: $c",
                    )
                    return null
                }
        }
        return null
    }

    fun getActContext(context: Context?): Context? = getActivity(context)
}
