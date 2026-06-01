// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.service

import android.app.Activity
import android.content.Context
import com.tiktok.sparkling.hybridkit.base.IKitView
import io.mockk.mockk
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class IBridgeServiceTest {
    private class StubBridgeService : IBridgeService {
        override fun createBridgeService(): IKitBridgeService? = null

        override fun onRegister(bid: String) = Unit

        override fun onUnRegister() = Unit

        override fun getBid(): String = "bid"
    }

    private class StubBridgeRegistry : IBridgeRegistry {
        var registered = 0
        var unregistered = 0

        override fun onRegisterBridge(
            kitView: IKitView,
            context: Context,
            sessionInfo: android.content.pm.PackageInstaller.SessionInfo?,
        ) {
            registered += 1
        }
    }

    private class StubStatusObserver : IBridgeStatusObserver {
        var created = 0
        var preInit = 0

        override fun onKitViewCreated(
            context: Context,
            kitView: IKitView,
            sessionInfo: android.content.pm.PackageInstaller.SessionInfo?,
        ) {
            created += 1
        }

        override fun onLynxViewPreInit(
            context: Context,
            builder: Any?,
            containerId: String?,
        ) {
            preInit += 1
        }
    }

    private class StubBridgeRefresher : IBridgeRefresher

    @Test
    fun bridgeServiceCreateBridgeServiceCanReturnNull() {
        val service: IBridgeService = StubBridgeService()
        assertNull(service.createBridgeService())
    }

    @Test
    fun bridgeRegistryDefaultUnregisterIsNoop() {
        val registry = StubBridgeRegistry()
        val kitView = mockk<IKitView>(relaxed = true)
        val context = Robolectric.buildActivity(Activity::class.java).create().get()

        registry.onRegisterBridge(kitView, context, null)
        // Use the default implementation of onUnRegisterBridge.
        registry.onUnRegisterBridge(kitView)
        assert(registry.registered == 1)
    }

    @Test
    fun statusObserverDefaultProvidedIsNoop() {
        val observer = StubStatusObserver()
        val kitView = mockk<IKitView>(relaxed = true)
        val context = Robolectric.buildActivity(Activity::class.java).create().get()

        observer.onKitViewCreated(context, kitView, null)
        // Default implementation should run without error.
        observer.onKitViewProvided(context, kitView, null)
        observer.onLynxViewPreInit(context, null, "containerId")
        assert(observer.created == 1)
        assert(observer.preInit == 1)
    }

    @Test
    fun bridgeRefresherDefaultLogsContext() {
        val refresher: IBridgeRefresher = StubBridgeRefresher()
        val context = Robolectric.buildActivity(Activity::class.java).create().get()
        // Default implementation logs and returns Unit.
        refresher.onContextRefreshed(context)
    }
}
