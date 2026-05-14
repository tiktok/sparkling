// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.base

import android.net.Uri
import com.tiktok.sparkling.hybridkit.scheme.HybridSchemeParam

interface IKitInitParam {
    var type: HybridKitType
    var loadUri: Uri?

    fun setGlobalProps(_globalProps: Map<String, Any>?) {}

    fun removeGlobalProps(_globalPropsKeys: List<String>?) {}

    fun obtainGlobalProps(): Map<String, Any>? = null

    fun getHybridSchemeParam(): HybridSchemeParam? = null

    fun applyHybridSchemeParam(hybridSchemeParams: HybridSchemeParam?)
}
