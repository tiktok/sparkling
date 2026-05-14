// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.router.utils

import android.content.Context
import com.tiktok.sparkling.method.registry.core.BridgePlatformType

/**
 * Desc:
 */
abstract class AbsRouteOpenHandler {
    var nextHandler: AbsRouteOpenHandler? = null
        private set
    var exceptionHandler: AbsRouteOpenHandler? = null
        private set

    fun setNextHandler(handler: AbsRouteOpenHandler?) {
        this.nextHandler = handler
    }

    fun setExceptionHandler(handler: AbsRouteOpenHandler?) {
        this.exceptionHandler = handler
    }

    abstract fun openScheme(
        scheme: String,
        extraInfo: Map<String, Any>,
        context: Context?,
    ): Boolean

    abstract fun getSupportPlatformTypeList(): List<BridgePlatformType>
}
