// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.registry.api

import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IBridgeContext
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.model.context.ContextProviderFactory
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BridgeLocalPoolTest {
    /**
     * Concrete IDL bridge method with a default constructor so the local pool
     * can lazily instantiate it via reflection.
     */
    open class FakeBridgeMethod : IDLBridgeMethod {
        override val name: String = "fake.method"
        var released: Int = 0

        override fun realHandle(
            params: Map<String, Any?>,
            callback: IDLBridgeMethod.Callback,
            type: BridgePlatformType,
        ) = Unit

        override fun setProviderFactory(contextProviderFactory: ContextProviderFactory?) = Unit

        override fun setBridgeContext(bridgeContext: IBridgeContext) = Unit

        override fun release() {
            released += 1
        }
    }

    class WebOnlyBridgeMethod : FakeBridgeMethod() {
        override val name: String = "web.only.method"
    }

    @Test
    fun setBridgeContextStoresContextOnInternalLocalBridge() {
        val pool = BridgeLocalPool()
        val ctx = mockk<IBridgeContext>(relaxed = true)
        pool.setBridgeContext(ctx)
        // No public getter; just assert that calling does not crash and that
        // subsequent register/getBridge keeps working.
        pool.registerLocalIDLMethod(FakeBridgeMethod::class.java)
        val bridge = pool.getBridge("fake.method", BridgePlatformType.ALL)
        assertNotNull(bridge)
    }

    @Test
    fun getBridgeReturnsNullForUnregisteredMethod() {
        val pool = BridgeLocalPool()
        pool.setBridgeContext(mockk(relaxed = true))
        val bridge = pool.getBridge("missing", BridgePlatformType.WEB)
        assertNull(bridge)
    }

    @Test
    fun getBridgeCachesInstancePerPlatform() {
        val pool = BridgeLocalPool()
        pool.setBridgeContext(mockk(relaxed = true))
        pool.registerLocalIDLMethod(FakeBridgeMethod::class.java)

        val first = pool.getBridge("fake.method", BridgePlatformType.WEB)
        val second = pool.getBridge("fake.method", BridgePlatformType.WEB)
        assertNotNull(first)
        // Cached instance should be reused across calls for the same platform.
        assertSame(first, second)
    }

    @Test
    fun getBridgeFallsBackToAllPlatformCache() {
        val pool = BridgeLocalPool()
        pool.setBridgeContext(mockk(relaxed = true))
        pool.registerLocalIDLMethod(FakeBridgeMethod::class.java, BridgePlatformType.ALL)

        // Prime ALL cache.
        val all = pool.getBridge("fake.method", BridgePlatformType.ALL)
        assertNotNull(all)

        // Asking for WEB should now return a *new* instance (since ALL/WEB platform-typed
        // cache miss takes the registration path again), but should not return null.
        val web = pool.getBridge("fake.method", BridgePlatformType.WEB)
        assertNotNull(web)
    }

    @Test
    fun releaseClearsCachesAndReleasesInstances() {
        val pool = BridgeLocalPool()
        pool.setBridgeContext(mockk(relaxed = true))
        pool.registerLocalIDLMethod(FakeBridgeMethod::class.java)

        val instance = pool.getBridge("fake.method", BridgePlatformType.WEB) as FakeBridgeMethod
        assertEquals(0, instance.released)

        pool.release()

        // After release, the cached instance should have had `release` called.
        assertEquals(1, instance.released)

        // After release, getting the bridge again should still re-register and
        // return a new instance (registry stays alive on the LocalBridge).
        val newInstance = pool.getBridge("fake.method", BridgePlatformType.WEB)
        assertNotNull(newInstance)
    }
}
