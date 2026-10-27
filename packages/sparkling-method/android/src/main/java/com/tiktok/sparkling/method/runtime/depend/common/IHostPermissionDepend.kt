// Copyright (c) 2023 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.runtime.depend.common

import android.app.Activity

interface OnPermissionGrantCallback {
    fun onAllGranted()

    fun onNotGranted()
}

interface OnPermissionsGrantCallback {
    fun onResult(onPermissionsGrantResults: Array<OnPermissionsGrantResult>)
}

class OnPermissionsGrantResult(
    val permission: String,
    val result: Int,
)

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
