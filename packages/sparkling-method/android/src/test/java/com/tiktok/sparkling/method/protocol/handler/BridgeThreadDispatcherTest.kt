// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.protocol.handler

import android.os.Handler
import android.os.Looper
import com.tiktok.sparkling.method.protocol.BridgeContext
import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BridgeThreadDispatcherTest {
    @Test
    fun bridgeFactoryManagerDefaultDoesNothing() {
        val mgr = BridgeFactoryManager()
        // Default implementation should be a no-op without throwing.
        mgr.checkAndInitBridge(BridgeContext(), BridgeCall(BridgeContext()))
    }

    @Test
    fun dispatchLynxBridgeThreadPostsToProvidedHandler() {
        val handler = Handler(Looper.getMainLooper())
        val dispatcher = BridgeThreadDispatcher(handler)
        val ctx = BridgeContext()
        val call = BridgeCall(ctx)

        var invoked = false
        var inMain = false
        dispatcher.dispatchLynxBridgeThread(call) { isMain ->
            invoked = true
            inMain = isMain
            Unit
        }
        // Drain main looper to execute the posted block.
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(invoked)
        assertTrue(inMain)
    }

    @Test
    fun dispatchWebBridgeThreadPostsToProvidedHandler() {
        val handler = Handler(Looper.getMainLooper())
        val dispatcher = BridgeThreadDispatcher(handler)
        val ctx = BridgeContext()
        val call = BridgeCall(ctx)

        val received = mutableListOf<Boolean>()
        dispatcher.dispatchWebBridgeThread(call) { isMain ->
            received += isMain
            Unit
        }
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf(true), received)
    }

    @Test
    fun defaultDispatcherUsesMainLooperHandler() {
        val dispatcher = BridgeThreadDispatcher()
        val ctx = BridgeContext()
        val call = BridgeCall(ctx)
        var counter = 0
        dispatcher.dispatchLynxBridgeThread(call) {
            counter += 1
            Unit
        }
        dispatcher.dispatchWebBridgeThread(call) {
            counter += 1
            Unit
        }
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(2, counter)
    }
}
