// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.base

class HybridKitError {
    var errorCode: Int? = null
    var errorReason: String? = null
    var originCode: Int? = null
    var originReason: String? = null

    fun printErrorMsg(): String = "errorCode = $errorCode, errorReason = $errorReason"
}

object HybridErrorConstantCode {
    const val LynxLoadError = 210
    const val ResourceLoadError = 211
}
