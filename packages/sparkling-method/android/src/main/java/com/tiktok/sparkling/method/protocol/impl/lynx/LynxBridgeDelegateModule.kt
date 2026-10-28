// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.impl.lynx

import com.lynx.jsbridge.LynxContextModule
import com.lynx.jsbridge.LynxMethod
import com.lynx.react.bridge.Callback
import com.lynx.react.bridge.ReadableMap
import com.lynx.tasm.behavior.LynxContext
import com.tiktok.sparkling.method.protocol.BridgeContext

/**
 * Lynx module that receives JSB calls from a LynxView and hands them to [RealLynxBridgeDelegate].
 *
 * Lynx only collects `@LynxMethod` methods declared on the registered class itself, so a subclass
 * registered with Lynx must override [call] and annotate it again.
 */
open class LynxBridgeDelegateModule protected constructor(
    val context: LynxContext,
    val obj: Any?,
    private val realLynxBridgeDelegate: RealLynxBridgeDelegate,
) : LynxContextModule(context, obj) {
    constructor(context: LynxContext, obj: Any?) : this(context, obj, RealLynxBridgeDelegate(obj))
    constructor(context: LynxContext) : this(context, null)

    private val TAG = "LynxBridgeDelegateModule"

    companion object {
        // here is the protocol of the front end of lynx, don't change it casually
        const val NAME = "spkPipe"
    }

    @LynxMethod
    open fun call(bridgeName: String, params: ReadableMap? = null, callback: Callback? = null) {
        if (context.isEmbeddedModeOn) {
            resolveEmbeddedBridgeContext()?.let {
                realLynxBridgeDelegate.setBridgeContext(it)
            }
        }
        realLynxBridgeDelegate.call(bridgeName, params, callback, "Lynx")
    }

    /** Context of the container that owns the current LynxView in embedded mode, if any. */
    protected open fun resolveEmbeddedBridgeContext(): BridgeContext? = null
}
