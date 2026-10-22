// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.api

import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IBridgeContext
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.IDLMethodProvider
import com.tiktok.sparkling.method.registry.core.IDLMethodRegistry
import com.tiktok.sparkling.method.registry.core.LocalBridge
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.ConcurrentHashMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class BridgeLocalPoolExtensionTest {
    @Test
    fun customHolderReceivesContextAndBothClassAndFactoryRegistrations() {
        val holder = HostHolder().apply { setIsLocalBridge(true) }
        val pool = object : BridgeLocalPool() {
            override fun localBridge(): LocalBridge = holder
            fun currentContext(): IBridgeContext? = bridgeContext
        }
        val context = mockk<IBridgeContext>()
        every { context.containerID } returns null
        val fromFactory = HostClassMethod()

        pool.setBridgeContext(context)
        pool.registerLocalIDLMethod(HostClassMethod::class.java, BridgePlatformType.WEB)
        pool.registerLocalIDLMethod("factory.method", BridgePlatformType.WEB) { fromFactory }

        assertSame(context, pool.currentContext())
        assertSame(context, holder.currentContext())
        assertSame(context, holder.registryContext)
        assertTrue(holder.registryIsLocal)
        assertEquals(1, holder.createCalls)
        assertSame(HostClassMethod::class.java, holder.findIDLMethodClass(BridgePlatformType.WEB, "host.class"))
        assertTrue(pool.getBridge("host.class", BridgePlatformType.WEB) is HostClassMethod)
        assertSame(fromFactory, holder.findIDLMethodProvider(BridgePlatformType.WEB, "factory.method")?.provideMethod())
        assertSame(fromFactory, pool.getBridge("factory.method", BridgePlatformType.WEB))

        pool.setBridgeContext(null)
        assertNull(pool.currentContext())
        assertNull(holder.currentContext())
    }

    @Test
    fun providerHooksRunOnlyOnCacheMissAndGlobalLookupFollowsALocalMiss() {
        val local = HostClassMethod()
        val global = HostClassMethod()
        val localLookups = mutableListOf<Pair<BridgePlatformType, String>>()
        val globalLookups = mutableListOf<Pair<BridgePlatformType, String>>()
        val pool = object : BridgeLocalPool() {
            override fun findLocalMethodProvider(platformType: BridgePlatformType, bridgeName: String): IDLMethodProvider? {
                localLookups.add(platformType to bridgeName)
                return if (bridgeName == "local") IDLMethodProvider { local } else null
            }

            override fun findGlobalMethodProvider(platformType: BridgePlatformType, bridgeName: String): IDLMethodProvider? {
                globalLookups.add(platformType to bridgeName)
                return if (bridgeName == "global") IDLMethodProvider { global } else null
            }
        }

        repeat(2) { assertSame(local, pool.getBridge("local", BridgePlatformType.WEB)) }
        repeat(2) { assertSame(global, pool.getBridge("global", BridgePlatformType.LYNX)) }

        assertEquals(listOf(BridgePlatformType.WEB to "local", BridgePlatformType.LYNX to "global"), localLookups)
        assertEquals(listOf(BridgePlatformType.LYNX to "global"), globalLookups)
    }

    @Test
    fun exposedTableAndBucketsAreUsedByLookupAndRelease() {
        val pool = InspectablePool()
        val table = pool.table()
        val webBucket = pool.bucket(BridgePlatformType.WEB)
        val web = HostClassMethod()
        val lynx = HostClassMethod()
        assertSame(webBucket, table[BridgePlatformType.WEB])
        webBucket["direct.web"] = web
        table[BridgePlatformType.LYNX] = ConcurrentHashMap<String, IDLBridgeMethod>().apply {
            put("direct.lynx", lynx)
        }

        assertSame(web, pool.getBridge("direct.web", BridgePlatformType.WEB))
        assertSame(lynx, pool.getBridge("direct.lynx", BridgePlatformType.LYNX))
        pool.release()

        assertEquals(1, web.released)
        assertEquals(1, lynx.released)
        assertTrue(table.isEmpty())
        assertNull(pool.getBridge("direct.web", BridgePlatformType.WEB))
    }

    @Test
    fun overriddenAllPlatformSelectsTheFallbackBucket() {
        val pool = object : InspectablePool() {
            override fun allPlatform(): BridgePlatformType = BridgePlatformType.LYNX
        }
        val defaultAll = HostClassMethod()
        val hostAll = HostClassMethod()
        pool.bucket(BridgePlatformType.ALL)["fallback"] = defaultAll
        pool.bucket(BridgePlatformType.LYNX)["fallback"] = hostAll

        assertSame(hostAll, pool.getBridge("fallback", BridgePlatformType.WEB))
        assertSame(hostAll, pool.getBridge("fallback", BridgePlatformType.NONE))
        assertSame(defaultAll, pool.getBridge("fallback", BridgePlatformType.ALL))
    }

    class HostClassMethod : BridgeLocalPoolTest.FakeBridgeMethod()

    private class HostHolder : LocalBridge() {
        var createCalls = 0
        var registryContext: IBridgeContext? = null
        var registryIsLocal = false

        fun currentContext(): IBridgeContext? = bridgeContext

        override fun createRegistry(): IDLMethodRegistry {
            createCalls++
            registryContext = bridgeContext
            registryIsLocal = isLocalBridge
            return object : IDLMethodRegistry(isLocalBridge, bridgeContext) {
                override fun resolveMethodName(clazz: Class<out IDLBridgeMethod>): String = "host.class"
            }
        }
    }

    private open class InspectablePool : BridgeLocalPool() {
        fun table(): ConcurrentHashMap<BridgePlatformType, ConcurrentHashMap<String, IDLBridgeMethod>> = methodTable()

        fun bucket(platformType: BridgePlatformType): ConcurrentHashMap<String, IDLBridgeMethod> =
            getPlatformTypeCache(platformType)

        override fun findGlobalMethodProvider(platformType: BridgePlatformType, bridgeName: String): IDLMethodProvider? = null
    }
}
