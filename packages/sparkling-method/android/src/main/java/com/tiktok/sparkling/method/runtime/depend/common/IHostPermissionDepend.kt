// Copyright (c) 2023 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.runtime.depend.common

import android.app.Activity
import androidx.annotation.Keep

@Keep
interface OnPermissionGrantCallback {
    fun onAllGranted()

    fun onNotGranted()
}

@Keep
interface OnPermissionsGrantCallback {
    fun onResult(onPermissionsGrantResults: Array<OnPermissionsGrantResult>)
}

@Keep
class OnPermissionsGrantResult(
    val permission: String,
    val result: Int,
)

@Keep
interface IHostPermissionDepend {
    fun hasPermission(
        activity: Activity,
        permission: String,
    ): Boolean

    fun requestPermission(
        activity: Activity,
        callback: OnPermissionGrantCallback,
        permission: String,
    )

    fun requestPermissions(
        activity: Activity,
        callback: OnPermissionsGrantCallback,
        permissions: Array<String>,
    )
}
