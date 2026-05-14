// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core.interfaces

import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.BridgePlatformType

interface IPlatformDataProcessor<Data> {
    fun matchPlatformType(platformType: BridgePlatformType): Boolean

    //    @Throws(IllegalInputParamException::class)
    fun transformPlatformDataToMap(
        params: Data,
        clazz: Class<out IDLBridgeMethod>,
    ): Map<String, Any?>?

    fun transformMapToPlatformData(
        params: Map<String, Any?>,
        clazz: Class<out IDLBridgeMethod>,
    ): Data
}
