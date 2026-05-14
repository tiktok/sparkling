// Copyright (c) 2024 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core.model.context

import com.tiktok.sparkling.method.protocol.entity.BridgeCall

/**
 */
open class BridgeCallThreadTypeConfig {
    /**
     * @param name String
     * @param call BridgeCall
     * @return BridgeCallThreadTypeEnum? corresponding BridgeCallThreadTypeEnum of given jsb name
     */
    open fun getBridgeCallThreadType(
        name: String,
        call: BridgeCall,
    ): BridgeCallThreadTypeEnum? = null
}

enum class BridgeCallThreadTypeEnum {
    CURRENT_THREAD, // current thread in lynx ,it means lynx js thread, in web, it means java_bridge thread
    MAIN_THREAD, // default only support MAIN_THREAD, if you want to dispatcher to other thread, you need to implement SparklingBridge.bridgeThreadDispatcher
    BACKGROUND_THREAD,
    IO_THREAD,
    NORMAL_THREAD,
    CPU_THREAD,
    SERIAL_THREAD,
}

/**
 * Convert input string to BridgeCallThreadTypeEnum
 */
fun threadStringToThreadTypeEnum(input: String): BridgeCallThreadTypeEnum =
    try {
        enumValueOf<BridgeCallThreadTypeEnum>(input)
    } catch (e: IllegalArgumentException) {
        BridgeCallThreadTypeEnum.MAIN_THREAD // Handle invalid input
    }
