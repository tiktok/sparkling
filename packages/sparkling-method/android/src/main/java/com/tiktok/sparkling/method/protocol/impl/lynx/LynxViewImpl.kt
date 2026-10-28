// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.impl.lynx

import com.lynx.jsbridge.LynxModule
import com.lynx.tasm.LynxBackgroundRuntimeOptions
import com.lynx.tasm.LynxViewBuilder
import com.lynx.tasm.group.LynxViewGroupBuilder
import com.tiktok.sparkling.method.protocol.BridgeContext

/**
 * Registers the JSB modules of a container on Lynx builders and background runtime options.
 */
open class LynxViewImpl(context: BridgeContext) {
    private val TAG = "LynxViewImpl"
    protected val mContext = context

    /** Module name the front end calls the bridge by. */
    protected open val bridgeModuleName: String
        get() = LynxBridgeDelegateModule.NAME

    protected open val bridgeModuleClass: Class<out LynxModule>
        get() = LynxBridgeDelegateModule::class.java

    protected open val runtimeBridgeModuleClass: Class<out LynxModule>
        get() = LynxRuntimeBridgeDelegateModule::class.java

    /**
     * The correct LynxViewBuilder needs to be registered here
     */
    fun init(builder: LynxViewBuilder): LynxViewBuilder {
        builder.registerModule(bridgeModuleName, bridgeModuleClass, mContext)
        return builder
    }

    /**
     * The correct module for LynxViewGroup needs to be registered here
     */
    fun initForLynxViewGroupBuilder(groupBuilder: LynxViewGroupBuilder): LynxViewGroupBuilder {
        groupBuilder.registerModule(bridgeModuleName, bridgeModuleClass, mContext)
        return groupBuilder
    }

    open fun initJSRuntime(options: LynxBackgroundRuntimeOptions): LynxBackgroundRuntimeOptions {
        options.registerModule(LynxRuntimeBridgeDelegateModule.NAME, runtimeBridgeModuleClass, mContext)
        return options
    }
}
