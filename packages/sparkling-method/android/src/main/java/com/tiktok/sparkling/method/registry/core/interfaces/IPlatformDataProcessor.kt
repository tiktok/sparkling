// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core.interfaces

import com.tiktok.sparkling.method.registry.core.IBridgePlatformType
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod

/**
 * Translates between the carrier a container hands parameters over in and the map every bridge
 * method is called with.
 *
 * [Data] is that carrier, and it is what differs between containers: a Lynx `ReadableMap`, a web
 * `JSONObject`. There is one implementation per container and [matchPlatformType] is how the right
 * one is picked, which is why the type is a parameter rather than fixed here.
 *
 * Deviation from open source: the platform is [IBridgePlatformType] rather than the concrete
 * `BridgePlatformType`, so a host whose platforms are an enum of its own can be matched without
 * converting first. The registry takes the interface for the same reason.
 */
interface IPlatformDataProcessor<Data> {

    fun matchPlatformType(platformType: IBridgePlatformType): Boolean

    //    @Throws(IllegalInputParamException::class)
    fun transformPlatformDataToMap(params: Data, clazz: Class<out IDLBridgeMethod>): Map<String, Any?>?

    fun transformMapToPlatformData(params: Map<String, Any?>, clazz: Class<out IDLBridgeMethod>): Data
}
