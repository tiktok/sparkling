// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.playground.depend

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.tiktok.sparkling.method.runtime.depend.common.IHostPermissionDepend
import com.tiktok.sparkling.method.runtime.depend.common.OnPermissionGrantCallback
import com.tiktok.sparkling.method.runtime.depend.common.OnPermissionsGrantCallback
import com.tiktok.sparkling.method.runtime.depend.common.OnPermissionsGrantResult

class AppPermissionDepend : IHostPermissionDepend {
    override fun hasPermission(
        activity: Activity,
        permission: String,
    ): Boolean = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED

    override fun requestPermission(
        activity: Activity,
        callback: OnPermissionGrantCallback,
        permission: String,
    ) {
        ActivityCompat.requestPermissions(activity, arrayOf(permission), PERMISSION_REQUEST_CODE)
        // Result handled via requestPermissions to callback: best-effort immediate check
        if (hasPermission(activity, permission)) {
            callback.onAllGranted()
        } else {
            callback.onNotGranted()
        }
    }

    override fun requestPermissions(
        activity: Activity,
        callback: OnPermissionsGrantCallback,
        permissions: Array<String>,
    ) {
        ActivityCompat.requestPermissions(activity, permissions, PERMISSION_REQUEST_CODE)
        val results =
            permissions
                .map { perm ->
                    val granted = hasPermission(activity, perm)
                    OnPermissionsGrantResult(perm, if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED)
                }.toTypedArray()
        callback.onResult(results)
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 0x527
    }
}
