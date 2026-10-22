// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

import com.tiktok.sparkling.method.registry.api.BridgeSettings
import com.tiktok.sparkling.method.registry.api.util.ThreadPool
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.Runs
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IDLMethodRegistryExtensionTest {
    @Before
    fun setUp() {
        mockkObject(BridgeSettings, IDLMethodRegistryCacheManager, ThreadPool)
        every { BridgeSettings.bridgeRegistryOptimize } returns true
        every { IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(any()) } returns null
        every { IDLMethodRegistryCacheManager.registerIDLMethodRegistryCache(any(), any()) } just Runs
        every { ThreadPool.runInBackGround(any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkObject(ThreadPool, IDLMethodRegistryCacheManager, BridgeSettings)
    }

    @Test
    fun bothRegistrationEntrypointsUseTheHostRegistryAndCurrentContext() {
        val bridge = HostBridge()
        val context = mockk<IBridgeContext>()
        every { context.containerID } returns "host-registry-test"
        bridge.setIsLocalBridge(true)
        bridge.setContext(context)
        val method = IDLMethodRegistryBehaviorTest.CountingMethod::class.java
        val factory = IDLMethodProvider { IDLMethodRegistryBehaviorTest.CountingMethod() }

        bridge.registerIDLMethod(method, BridgePlatformType.LYNX, "classes")
        bridge.registerIDLMethod("factory-name", factory, BridgePlatformType.WEB, "factories", method)

        assertEquals(2, bridge.created.size)
        bridge.created.forEach {
            assertTrue(it.observedLocal)
            assertSame(context, it.observedContext)
        }
        assertEquals(1, bridge.created[0].resolutionCount)
        assertEquals(0, bridge.created[1].resolutionCount)
        assertSame(method, bridge.findIDLMethodClass(BridgePlatformType.LYNX, "host-name", "classes"))
        assertEquals("counted", requireNotNull(bridge.findIDLMethodProvider(BridgePlatformType.LYNX, "host-name", "classes")).provideMethod().name)
        assertSame(factory, bridge.findIDLMethodProvider(BridgePlatformType.WEB, "factory-name", "factories"))
        assertSame(method, bridge.findIDLMethodClass(BridgePlatformType.WEB, "factory-name", "factories"))

        bridge.registerIDLMethod(method, BridgePlatformType.LYNX, "classes")
        assertEquals(2, bridge.created.size)
        assertEquals(2, bridge.created[0].resolutionCount)
    }

    @Test
    fun unresolvedHostNamesDoNotPopulateEitherRegistryTable() {
        for (name in listOf(null, "")) {
            val registry = HostRegistry(false, null).apply { resolvedName = name }
            registry.registerMethod(IDLMethodRegistryBehaviorTest.CountingMethod::class.java)

            assertEquals(1, registry.resolutionCount)
            for (platform in listOf(BridgePlatformType.ALL, BridgePlatformType.WEB, BridgePlatformType.LYNX)) {
                assertNull(registry.getMethodList(platform))
                assertFalse(registry.isMethodExists("counted", platform))
                assertFalse(registry.isMethodExists("", platform))
            }
        }
    }

    @Test
    fun copyFromReplacesSourcePlatformsAndKeepsOtherPlatformsIndependent() {
        val source = IDLMethodRegistry().apply { namespace = "source" }
        val target = HostRegistry(false, null).apply { namespace = "target" }
        val sourceProvider = IDLMethodProvider { IDLMethodRegistryBehaviorTest.CountingMethod() }
        val oldProvider = IDLMethodProvider { IDLMethodRegistryBehaviorTest.ReplacementMethod() }
        val sourceClass = IDLMethodRegistryBehaviorTest.CountingMethod::class.java
        val oldClass = IDLMethodRegistryBehaviorTest.ReplacementMethod::class.java
        source.registerMethodProvider("source-entry", sourceProvider, BridgePlatformType.WEB, sourceClass)
        target.registerMethodProvider("old-entry", oldProvider, BridgePlatformType.WEB, oldClass)
        target.registerMethodProvider("retained", oldProvider, BridgePlatformType.LYNX, oldClass)

        target.copyFrom(source)

        assertEquals("source", target.namespace)
        assertSame(sourceProvider, target.findMethodProvider(BridgePlatformType.WEB, "source-entry"))
        assertSame(sourceClass, target.findMethodClass(BridgePlatformType.WEB, "source-entry"))
        assertFalse(target.isMethodExists("old-entry", BridgePlatformType.WEB))
        assertSame(oldProvider, target.findMethodProvider(BridgePlatformType.LYNX, "retained"))
        assertSame(oldClass, target.findMethodClass(BridgePlatformType.LYNX, "retained"))

        source.registerMethodProvider("source-entry", oldProvider, BridgePlatformType.WEB, oldClass)
        assertSame(sourceProvider, target.findMethodProvider(BridgePlatformType.WEB, "source-entry"))
        assertSame(sourceClass, target.findMethodClass(BridgePlatformType.WEB, "source-entry"))
        requireNotNull(target.getMethodList(BridgePlatformType.WEB)).clear()
        target.registerMethodProvider("target-only", oldProvider, BridgePlatformType.WEB)
        assertSame(oldClass, source.findMethodClass(BridgePlatformType.WEB, "source-entry"))
        assertFalse(source.isMethodExists("target-only", BridgePlatformType.WEB))
    }

    @Test
    fun copyingAProviderOnlyPlatformRemovesOldClassAndProviderEntries() {
        val source = IDLMethodRegistry()
        val target = IDLMethodRegistry()
        val freshProvider = IDLMethodProvider { IDLMethodRegistryBehaviorTest.CountingMethod() }
        source.registerMethodProvider("fresh", freshProvider, BridgePlatformType.WEB)
        target.registerMethodProvider(
            "stale",
            IDLMethodProvider { IDLMethodRegistryBehaviorTest.ReplacementMethod() },
            BridgePlatformType.WEB,
            IDLMethodRegistryBehaviorTest.ReplacementMethod::class.java,
        )

        target.copyFrom(source)

        assertSame(freshProvider, target.findMethodProvider(BridgePlatformType.WEB, "fresh"))
        assertNull(target.getMethodList(BridgePlatformType.WEB))
        assertNull(target.findMethodClass(BridgePlatformType.WEB, "stale"))
        assertNull(target.findMethodProvider(BridgePlatformType.WEB, "stale"))
        assertFalse(target.isMethodExists("stale", BridgePlatformType.WEB))
    }

    @Test
    fun registryTableExposesTheSameTableUsedForReplacementAndLookup() {
        val bridge = HostBridge()
        val table = bridge.exposedTable()
        val original = IDLMethodRegistry().apply { namespace = "shared" }
        val replacement = IDLMethodRegistry().apply {
            namespace = "shared"
            registerMethodProvider("replacement", IDLMethodProvider { IDLMethodRegistryBehaviorTest.ReplacementMethod() })
        }

        bridge.registerRegistry(original)
        assertSame(original, table["shared"])
        bridge.registerRegistry(replacement)
        assertSame(table, bridge.exposedTable())
        assertSame(replacement, table["shared"])
        assertEquals("replacement", requireNotNull(bridge.findIDLMethodProvider(BridgePlatformType.WEB, "replacement", "shared")).provideMethod().name)

        table.remove("shared")
        assertNull(bridge.findIDLMethodProvider(BridgePlatformType.WEB, "replacement", "shared"))
    }

    private class HostBridge : LocalBridge() {
        val created = mutableListOf<HostRegistry>()

        override fun createRegistry(): IDLMethodRegistry =
            HostRegistry(isLocalBridge, bridgeContext).also { created.add(it) }

        fun exposedTable() = registryTable()
    }

    private class HostRegistry(local: Boolean, context: IBridgeContext?) : IDLMethodRegistry(local, context) {
        val observedLocal: Boolean get() = isLocalBridgeRegistry
        val observedContext: IBridgeContext? get() = bridgeContext
        var resolutionCount = 0
        var resolvedName: String? = "host-name"

        override fun resolveMethodName(clazz: Class<out IDLBridgeMethod>): String? {
            resolutionCount++
            return resolvedName
        }
    }
}
