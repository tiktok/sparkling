// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.impl.lynx

import com.tiktok.sparkling.method.protocol.BridgeContext
import com.lynx.tasm.LynxBackgroundRuntimeOptions
import com.lynx.tasm.LynxViewBuilder

class LynxViewImpl(
    context: BridgeContext,
) {
    private val TAG = "LynxViewImpl"
    private val BRIDGE_NAME = "spkPipe"
    private val mContext = context

    /**
     * The correct LynxViewBuilder needs to be registered here
     */
    fun init(builder: LynxViewBuilder): LynxViewBuilder {
        builder.registerModule(BRIDGE_NAME, LynxBridgeDelegateModule::class.java, mContext)
        return builder
    }

    fun initJSRuntime(options: LynxBackgroundRuntimeOptions): LynxBackgroundRuntimeOptions {
        options.registerModule(LynxRuntimeBridgeDelegateModule.NAME, LynxRuntimeBridgeDelegateModule::class.java, mContext)
        return options
    }
}
