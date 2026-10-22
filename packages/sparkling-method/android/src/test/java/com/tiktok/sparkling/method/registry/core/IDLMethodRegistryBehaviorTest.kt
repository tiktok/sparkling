// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

import com.tiktok.sparkling.method.registry.api.BridgeSettings
import com.tiktok.sparkling.method.registry.api.util.ThreadPool
import com.tiktok.sparkling.method.registry.core.model.context.ContextProviderFactory
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.Runs
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Contracts exercised against both the original registry and its extensible replacement. */
class IDLMethodRegistryBehaviorTest {
    @Before
    fun setUp() {
        mockkObject(BridgeSettings, IDLMethodRegistryCacheManager, ThreadPool)
        every { BridgeSettings.bridgeRegistryOptimize } returns false
        every { IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(any()) } returns null
        every { IDLMethodRegistryCacheManager.registerIDLMethodRegistryCache(any(), any()) } just Runs
        // Cache execution is covered separately; these tests isolate name fallback and registry state.
        every { ThreadPool.runInBackGround(any()) } just Runs
        CountingMethod.constructions = 0
    }

    @After
    fun tearDown() {
        unmockkObject(ThreadPool, IDLMethodRegistryCacheManager, BridgeSettings)
    }

    @Test
    fun providerOnlyRegistrationIsLazyAndCoversAllPlatforms() {
        val registry = IDLMethodRegistry()
        val provider = IDLMethodProvider { CountingMethod() }
        registry.registerMethodProvider("factory", provider)

        for (platform in listOf(BridgePlatformType.ALL, BridgePlatformType.WEB, BridgePlatformType.LYNX)) {
            assertTrue(registry.isMethodExists("factory", platform))
            assertSame(provider, registry.findMethodProvider(platform, "factory"))
            assertNull(registry.findMethodClass(platform, "factory"))
        }
        assertEquals(0, CountingMethod.constructions)
        requireNotNull(registry.findMethodProvider(BridgePlatformType.WEB, "factory")).provideMethod()
        assertEquals(1, CountingMethod.constructions)

        registry.registerMethodProvider("", provider)
        assertFalse(registry.isMethodExists(""))
    }

    @Test
    fun copyWithCopiesBothTablesWithoutSharingTheirMutableBuckets() {
        val original = IDLMethodRegistry().apply { namespace = "custom" }
        val typedProvider = IDLMethodProvider { CountingMethod() }
        val onlyProvider = IDLMethodProvider { CountingMethod() }
        val replacement = IDLMethodProvider { ReplacementMethod() }
        original.registerMethodProvider("typed", typedProvider, BridgePlatformType.WEB, CountingMethod::class.java)
        original.registerMethodProvider("only", onlyProvider, BridgePlatformType.LYNX)

        val copy = IDLMethodRegistry.copyWith(original)
        assertEquals("custom", copy.namespace)
        assertSame(typedProvider, copy.findMethodProvider(BridgePlatformType.WEB, "typed"))
        assertSame(CountingMethod::class.java, copy.findMethodClass(BridgePlatformType.WEB, "typed"))
        assertSame(onlyProvider, copy.findMethodProvider(BridgePlatformType.LYNX, "only"))
        assertNull(copy.findMethodClass(BridgePlatformType.LYNX, "only"))
        assertNotSame(original.getMethodList(BridgePlatformType.WEB), copy.getMethodList(BridgePlatformType.WEB))

        original.registerMethodProvider("typed", replacement, BridgePlatformType.WEB, ReplacementMethod::class.java)
        assertSame(typedProvider, copy.findMethodProvider(BridgePlatformType.WEB, "typed"))
        assertSame(CountingMethod::class.java, copy.findMethodClass(BridgePlatformType.WEB, "typed"))
        copy.registerMethodProvider("only", replacement, BridgePlatformType.LYNX)
        assertSame(onlyProvider, original.findMethodProvider(BridgePlatformType.LYNX, "only"))
        requireNotNull(copy.getMethodList(BridgePlatformType.WEB)).remove("typed")
        assertSame(ReplacementMethod::class.java, original.findMethodClass(BridgePlatformType.WEB, "typed"))
    }

    @Test
    fun noneLookupsStayEmptyEvenAfterExplicitRegistration() {
        val registry = IDLMethodRegistry()
        registry.registerMethodProvider("hidden", IDLMethodProvider { CountingMethod() }, BridgePlatformType.NONE, CountingMethod::class.java)

        assertNull(registry.findMethodClass(BridgePlatformType.NONE, "hidden"))
        assertNull(registry.findMethodProvider(BridgePlatformType.NONE, "hidden"))
        assertNull(registry.getMethodList(BridgePlatformType.NONE))
        assertFalse(registry.isMethodExists("hidden", BridgePlatformType.NONE))
        assertFalse(registry.isMethodExists("hidden", BridgePlatformType.ALL))
    }

    @Test
    fun optimizationRetainsTheExistingNumberOfNameFallbackConstructions() {
        for (optimized in listOf(false, true)) {
            every { BridgeSettings.bridgeRegistryOptimize } returns optimized
            CountingMethod.constructions = 0
            val registry = IDLMethodRegistry(inferMethodNameFromInstance = true)
            registry.registerMethod(CountingMethod::class.java)

            assertEquals(if (optimized) 1 else 3, CountingMethod.constructions)
            for (platform in listOf(BridgePlatformType.ALL, BridgePlatformType.WEB, BridgePlatformType.LYNX)) {
                assertSame(CountingMethod::class.java, registry.findMethodClass(platform, "counted"))
                assertNotNull(registry.findMethodProvider(platform, "counted"))
            }
        }
    }

    @Test
    fun nameFallbackIsOptInForGlobalAndOptOutForLocalRegistries() {
        val context = mockk<IBridgeContext>()
        every { context.containerID } returns "registry-behavior-test"

        val global = IDLMethodRegistry()
        global.registerMethod(CountingMethod::class.java, BridgePlatformType.WEB)
        assertFalse(global.isMethodExists("counted", BridgePlatformType.WEB))
        assertEquals(0, CountingMethod.constructions)

        val optedIn = IDLMethodRegistry(inferMethodNameFromInstance = true)
        optedIn.registerMethod(CountingMethod::class.java, BridgePlatformType.WEB)
        assertTrue(optedIn.isMethodExists("counted", BridgePlatformType.WEB))
        assertEquals(1, CountingMethod.constructions)

        val local = IDLMethodRegistry(isLocalBridgeRegistry = true, bridgeContext = context)
        local.registerMethod(CountingMethod::class.java, BridgePlatformType.WEB)
        assertTrue(local.isMethodExists("counted", BridgePlatformType.WEB))
        assertEquals(2, CountingMethod.constructions)

        val optedOut = IDLMethodRegistry(isLocalBridgeRegistry = true, bridgeContext = context, inferMethodNameFromInstance = false)
        optedOut.registerMethod(CountingMethod::class.java, BridgePlatformType.WEB)
        assertFalse(optedOut.isMethodExists("counted", BridgePlatformType.WEB))
        assertEquals(2, CountingMethod.constructions)
    }

    @Test
    fun failedConstructionAndEmptyInstanceNamesDoNotCreateRegistrations() {
        val registry = IDLMethodRegistry(inferMethodNameFromInstance = true)
        registry.registerMethod(ThrowingMethod::class.java, BridgePlatformType.WEB)
        registry.registerMethod(EmptyNameMethod::class.java, BridgePlatformType.WEB)

        assertNull(registry.getMethodList(BridgePlatformType.WEB))
        assertFalse(registry.isMethodExists("throwing", BridgePlatformType.WEB))
        assertFalse(registry.isMethodExists("", BridgePlatformType.WEB))
    }

    @Test
    fun localBridgeReplacesOnlyTheRegistryWithTheSameNamespace() {
        val bridge = LocalBridge()
        val first = IDLMethodProvider { CountingMethod() }
        val second = IDLMethodProvider { ReplacementMethod() }
        fun registry(namespace: String, provider: IDLMethodProvider): IDLMethodRegistry =
            IDLMethodRegistry().apply {
                this.namespace = namespace
                registerMethodProvider("same-name", provider, BridgePlatformType.WEB)
            }

        bridge.registerRegistry(registry("main", first))
        bridge.registerRegistry(registry("other", first))
        bridge.registerRegistry(registry("main", second))

        assertSame(second, bridge.findIDLMethodProvider(BridgePlatformType.WEB, "same-name", "main"))
        assertSame(first, bridge.findIDLMethodProvider(BridgePlatformType.WEB, "same-name", "other"))
        assertNull(bridge.findIDLMethodProvider(BridgePlatformType.WEB, "same-name", "missing"))
        assertNull(bridge.findIDLMethodProvider(BridgePlatformType.LYNX, "same-name", "main"))
    }

    open class NamedMethod(override val name: String) : IDLBridgeMethod {
        override fun realHandle(params: Map<String, Any?>, callback: IDLBridgeMethod.Callback, type: BridgePlatformType) = Unit
        override fun setProviderFactory(contextProviderFactory: ContextProviderFactory?) = Unit
        override fun setBridgeContext(bridgeContext: IBridgeContext) = Unit
    }

    class CountingMethod : NamedMethod("counted") {
        init { constructions++ }

        companion object {
            var constructions = 0
        }
    }

    class ReplacementMethod : NamedMethod("replacement")

    class EmptyNameMethod : NamedMethod("")

    class ThrowingMethod : NamedMethod("throwing") {
        init { throw IllegalStateException("constructor failure") }
    }
}
