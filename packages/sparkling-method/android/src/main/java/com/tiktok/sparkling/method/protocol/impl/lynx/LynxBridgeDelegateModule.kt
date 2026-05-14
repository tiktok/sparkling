// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.impl.lynx

import com.lynx.jsbridge.LynxContextModule
import com.lynx.jsbridge.LynxMethod
import com.lynx.react.bridge.Callback
import com.lynx.react.bridge.ReadableMap
import com.lynx.tasm.behavior.LynxContext

class LynxBridgeDelegateModule(
    val context: LynxContext,
    val obj: Any?,
) : LynxContextModule(context, obj) {
    constructor(context: LynxContext) : this(context, null)

    private val TAG = "LynxBridgeDelegateModule"

    private val realLynxBridgeDelegate: RealLynxBridgeDelegate = RealLynxBridgeDelegate(obj)

    companion object {
        // here is the protocol of the front end of lynx, don't change it casually
        const val NAME = "spkPipe"
    }

    @LynxMethod
    fun call(
        bridgeName: String,
        params: ReadableMap? = null,
        callback: Callback? = null,
    ) {
        realLynxBridgeDelegate.call(bridgeName, params, callback, "Lynx")
    }
}
