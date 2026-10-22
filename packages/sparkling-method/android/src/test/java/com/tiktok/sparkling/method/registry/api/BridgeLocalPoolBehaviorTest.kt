// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.api

import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IBridgeContext
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.IDLMethodProvider
import com.tiktok.sparkling.method.registry.core.SparklingBridgeManager
import com.tiktok.sparkling.method.registry.core.model.context.ContextProviderFactory
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Behavior contracts that run against both the original and extensible pool. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class BridgeLocalPoolBehaviorTest {
    @Before
    fun setUp() {
        mockkStatic(SparklingBridgeManager::class)
        every { SparklingBridgeManager.findIDLMethodProvider(any(), any(), any()) } returns null
    }

    @After
    fun tearDown() {
        unmockkStatic(SparklingBridgeManager::class)
    }

    @Test
    fun factoryCreatesMethodsWithRequiredConstructorArgumentsAndCachesPerPlatform() {
        val pool = BridgeLocalPool()
        var factoryCalls = 0
        pool.registerLocalIDLMethod("factory.method") {
            factoryCalls++
            ConstructedMethod("constructor-argument-$factoryCalls")
        }

        val web = requireNotNull(pool.getBridge("factory.method", BridgePlatformType.WEB))
        assertEquals("constructor-argument-1", web.name)
        assertSame(web, pool.getBridge("factory.method", BridgePlatformType.WEB))
        val lynx = requireNotNull(pool.getBridge("factory.method", BridgePlatformType.LYNX))
        assertEquals("constructor-argument-2", lynx.name)
        assertNotSame(web, lynx)
        assertEquals(2, factoryCalls)
    }

    @Test
    fun localProvidersWinAndMissingLocalMethodsFallBackToGlobalProviders() {
        val pool = BridgeLocalPool()
        val local = ConstructedMethod("local")
        val global = ConstructedMethod("global")
        var globalCalls = 0
        val globalProvider = IDLMethodProvider {
            globalCalls++
            global
        }
        every {
            SparklingBridgeManager.findIDLMethodProvider(BridgePlatformType.WEB, any(), any())
        } returns globalProvider
        pool.registerLocalIDLMethod("local.method") { local }

        assertSame(local, pool.getBridge("local.method", BridgePlatformType.WEB))
        verify(exactly = 0) {
            SparklingBridgeManager.findIDLMethodProvider(BridgePlatformType.WEB, "local.method", any())
        }
        assertSame(global, pool.getBridge("global.method", BridgePlatformType.WEB))
        assertSame(global, pool.getBridge("global.method", BridgePlatformType.WEB))
        assertEquals(1, globalCalls)
        verify(exactly = 1) {
            SparklingBridgeManager.findIDLMethodProvider(BridgePlatformType.WEB, "global.method", any())
        }
    }

    @Test
    fun platformCachePrecedesAllCacheAndAllCacheAlsoServesNone() {
        val pool = BridgeLocalPool()
        var factoryCalls = 0
        pool.registerLocalIDLMethod("cached.method") {
            factoryCalls++
            ConstructedMethod("instance-$factoryCalls")
        }

        assertNull(pool.getBridge("cached.method", BridgePlatformType.NONE))
        val web = requireNotNull(pool.getBridge("cached.method", BridgePlatformType.WEB))
        val all = requireNotNull(pool.getBridge("cached.method", BridgePlatformType.ALL))
        assertNotSame(web, all)
        assertSame(web, pool.getBridge("cached.method", BridgePlatformType.WEB))
        assertSame(all, pool.getBridge("cached.method", BridgePlatformType.LYNX))
        // The existing pool consults ALL instances before registry-level NONE rejection.
        assertSame(all, pool.getBridge("cached.method", BridgePlatformType.NONE))
        assertEquals(2, factoryCalls)
    }

    @Test
    fun factoryFailuresArePropagatedAndRetriedUntilAnInstanceIsCached() {
        val pool = BridgeLocalPool()
        val failure = IllegalStateException("factory failed")
        val method = ConstructedMethod("recovered")
        var factoryCalls = 0
        pool.registerLocalIDLMethod("retry.method") {
            factoryCalls++
            if (factoryCalls == 1) throw failure
            method
        }

        assertSame(failure, runCatching { pool.getBridge("retry.method", BridgePlatformType.WEB) }.exceptionOrNull())
        assertSame(method, pool.getBridge("retry.method", BridgePlatformType.WEB))
        assertSame(method, pool.getBridge("retry.method", BridgePlatformType.WEB))
        assertEquals(2, factoryCalls)
    }

    @Test
    fun releaseReleasesCreatedInstancesAndKeepsRegistrationsForReuse() {
        val pool = BridgeLocalPool()
        var factoryCalls = 0
        pool.registerLocalIDLMethod("release.method") {
            factoryCalls++
            ConstructedMethod("instance-$factoryCalls")
        }
        val web = pool.getBridge("release.method", BridgePlatformType.WEB) as ConstructedMethod
        val lynx = pool.getBridge("release.method", BridgePlatformType.LYNX) as ConstructedMethod

        pool.release()

        assertEquals(1, web.releaseCalls)
        assertEquals(1, lynx.releaseCalls)
        val replacement = pool.getBridge("release.method", BridgePlatformType.WEB) as ConstructedMethod
        assertNotSame(web, replacement)
        assertEquals(0, replacement.releaseCalls)
        assertEquals(3, factoryCalls)
    }

    @Test
    fun releaseFailurePropagatesAndLeavesTheCachedInstanceAvailable() {
        val pool = BridgeLocalPool()
        val failure = IllegalStateException("release failed")
        var factoryCalls = 0
        pool.registerLocalIDLMethod("release.failure") {
            factoryCalls++
            ConstructedMethod("instance-$factoryCalls")
        }
        val method = pool.getBridge("release.failure", BridgePlatformType.WEB) as ConstructedMethod
        method.releaseFailure = failure

        assertSame(failure, runCatching { pool.release() }.exceptionOrNull())
        assertEquals(1, method.releaseCalls)
        assertSame(method, pool.getBridge("release.failure", BridgePlatformType.WEB))
        assertEquals(1, factoryCalls)

        method.releaseFailure = null
        pool.release()
        assertEquals(2, method.releaseCalls)
        assertNotSame(method, pool.getBridge("release.failure", BridgePlatformType.WEB))
        assertEquals(2, factoryCalls)
    }

    @Test
    fun reRegistrationDoesNotReplaceAnAlreadyCachedInstance() {
        val pool = BridgeLocalPool()
        val original = ConstructedMethod("original")
        val replacement = ConstructedMethod("replacement")
        pool.registerLocalIDLMethod("replace.method") { original }
        assertSame(original, pool.getBridge("replace.method", BridgePlatformType.WEB))

        pool.registerLocalIDLMethod("replace.method") { replacement }

        assertSame(original, pool.getBridge("replace.method", BridgePlatformType.WEB))
        assertSame(replacement, pool.getBridge("replace.method", BridgePlatformType.LYNX))
    }

    private class ConstructedMethod(override val name: String) : IDLBridgeMethod {
        var releaseCalls = 0
        var releaseFailure: Throwable? = null

        override fun realHandle(
            params: Map<String, Any?>,
            callback: IDLBridgeMethod.Callback,
            type: BridgePlatformType,
        ) = Unit

        override fun setProviderFactory(contextProviderFactory: ContextProviderFactory?) = Unit

        override fun setBridgeContext(bridgeContext: IBridgeContext) = Unit

        override fun release() {
            releaseCalls++
            releaseFailure?.let { throw it }
        }
    }
}
