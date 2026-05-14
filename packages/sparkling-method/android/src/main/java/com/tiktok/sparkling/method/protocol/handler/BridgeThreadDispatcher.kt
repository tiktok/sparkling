// Copyright (c) 2023 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.handler

import android.os.Handler
import android.os.Looper
import com.tiktok.sparkling.method.protocol.entity.BridgeCall

/**
 * if you want to dispatch jsb to other thread, you need to implement SparklingBridge.bridgeThreadDispatcher
 * you can just implement one method when you just want to dispatch noe engine's jsb thread.
 */
open class BridgeThreadDispatcher(
    private val handler: Handler? = null,
) {
    // Lazy initialize the Handler to avoid issues in unit tests
    private val mHandler: Handler? by lazy {
        handler ?: try {
            Handler(Looper.getMainLooper())
        } catch (e: RuntimeException) {
            // In unit tests, return null handler and execute synchronously
            null
        }
    }

    /**
     * if you want to dispatch lynx's jsb, you can use this method
     */
    open fun dispatchLynxBridgeThread(
        call: BridgeCall,
        jsbRealCallBlock: (isRunInMainThread: Boolean) -> Unit?,
    ) {
        val handler = mHandler
        if (handler != null) {
            handler.post {
                jsbRealCallBlock(true)
            }
        } else {
            // Execute synchronously in unit tests
            jsbRealCallBlock(true)
        }
    }

    /**
     * if you want to dispatch web's jsb, you can use this method
     */
    open fun dispatchWebBridgeThread(
        call: BridgeCall,
        jsbRealCallBlock: (isRunInMainThread: Boolean) -> Unit?,
    ) {
        val handler = mHandler
        if (handler != null) {
            handler.post {
                jsbRealCallBlock(true)
            }
        } else {
            // Execute synchronously in unit tests
            jsbRealCallBlock(true)
        }
    }
}
