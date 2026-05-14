// Copyright (c) 2024 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.impl.lynx

import android.content.Context
import com.lynx.jsbridge.LynxMethod
import com.lynx.jsbridge.LynxModule
import com.lynx.react.bridge.Callback
import com.lynx.react.bridge.ReadableMap

/**
 */
class LynxRuntimeBridgeDelegateModule(
    val context: Context,
    val obj: Any?,
) : LynxModule(context, obj) {
    private val TAG = "LynxRuntimeBridgeDelegateModule"
    private val realLynxBridgeDelegate: RealLynxBridgeDelegate = RealLynxBridgeDelegate(obj)

    companion object {
        // here is the protocol of the front end of lynx, don't change it casually
        const val NAME = "runtimeBridge"
    }

    @LynxMethod
    fun call(
        bridgeName: String,
        params: ReadableMap? = null,
        callback: Callback? = null,
    ) {
        realLynxBridgeDelegate.call(bridgeName, params, callback, "LynxRuntime")
    }
}
