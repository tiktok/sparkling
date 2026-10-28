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
 * Lynx module that receives JSB calls from a Lynx background runtime and hands them to
 * [RealLynxBridgeDelegate].
 *
 * Lynx only collects `@LynxMethod` methods declared on the registered class itself, so a subclass
 * registered with Lynx must override [call] and annotate it again.
 */
open class LynxRuntimeBridgeDelegateModule protected constructor(
    val context: Context,
    val obj: Any?,
    private val realLynxBridgeDelegate: RealLynxBridgeDelegate,
) : LynxModule(context, obj) {
    constructor(context: Context, obj: Any?) : this(context, obj, RealLynxBridgeDelegate(obj))

    private val TAG = "LynxRuntimeBridgeDelegateModule"

    companion object {
        // here is the protocol of the front end of lynx, don't change it casually
        const val NAME = "runtimeBridge"
    }

    @LynxMethod
    open fun call(bridgeName: String, params: ReadableMap? = null, callback: Callback? = null) {
        realLynxBridgeDelegate.call(bridgeName, params, callback, "LynxRuntime")
    }
}
