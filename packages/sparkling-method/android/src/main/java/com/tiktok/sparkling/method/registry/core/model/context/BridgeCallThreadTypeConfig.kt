// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core.model.context

import com.tiktok.sparkling.method.protocol.entity.BridgeCall

/** Configures the thread used to handle each bridge call. */
open class BridgeCallThreadTypeConfig {
    /** Returns the thread type used to handle the given bridge call. */
    open fun getBridgeCallThreadType(
        name: String,
        call: BridgeCall,
    ): BridgeCallThreadTypeEnum? = null
}

/** Threads available for bridge-call dispatch. */
enum class BridgeCallThreadTypeEnum {
    CURRENT_THREAD,
    MAIN_THREAD,
    MAIN_THREAD_IMMEDIATE,
    BACKGROUND_THREAD,
    IO_THREAD,
    NORMAL_THREAD,
    CPU_THREAD,
    SERIAL_THREAD,
}

/** Converts a thread name to its enum value and falls back to the main thread. */
fun threadStringToThreadTypeEnum(input: String): BridgeCallThreadTypeEnum =
    try {
        enumValueOf<BridgeCallThreadTypeEnum>(input)
    } catch (e: IllegalArgumentException) {
        BridgeCallThreadTypeEnum.MAIN_THREAD
    }
