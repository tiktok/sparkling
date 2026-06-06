// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.registry.core.model.context

import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [ContextProviderFactory], its supporting holders ([ContextHolder],
 * [WeakContextHolder], [WeakHostContextHolder]), the [BridgeCallThreadTypeConfig]
 * default behavior, and the [threadStringToThreadTypeEnum] free function.
 */
class ContextProviderFactoryTest {
    private interface SampleApi {
        fun greet(): String
    }

    private class SampleImpl(
        private val msg: String,
    ) : SampleApi {
        override fun greet(): String = msg
    }

    @Test
    fun contextHolderProvidesAndReleasesInstance() {
        val target = SampleImpl("hi")
        val holder = ContextHolder<SampleApi>(target)
        assertSame(target, holder.provideInstance())
        holder.release()
        assertNull(holder.provideInstance())
    }

    @Test
    fun contextHolderHandlesNullInitialValue() {
        val holder = ContextHolder<SampleApi>(null)
        assertNull(holder.provideInstance())
        holder.release()
        assertNull(holder.provideInstance())
    }

    @Test
    fun weakContextHolderProvidesAndReleasesInstance() {
        val target = SampleImpl("hi")
        val holder = WeakContextHolder<SampleApi>(target)
        assertSame(target, holder.provideInstance())
        holder.release()
        assertNull(holder.provideInstance())
    }

    @Test
    fun weakContextHolderWithNullDoesNotAllocateReference() {
        val holder = WeakContextHolder<SampleApi>(null)
        assertNull(holder.provideInstance())
        holder.release()
        assertNull(holder.provideInstance())
    }

    @Test
    fun weakHostContextHolderInvokesProviderOnHost() {
        val host = SampleImpl("hello")
        val holder = WeakHostContextHolder(host) { greet() }
        assertEquals("hello", holder.provideInstance())
        holder.release()
        assertNull(holder.provideInstance())
    }

    @Test
    fun registerProviderViaLambdaProvidesInstance() {
        val factory = ContextProviderFactory()
        val target = SampleImpl("a")
        factory.registerProvider(SampleApi::class.java) { target }
        assertTrue(factory.has(SampleApi::class.java))
        assertSame(target, factory.provideInstance(SampleApi::class.java))
    }

    @Test
    fun registerProviderReplacesExistingAndReleasesPrevious() {
        val factory = ContextProviderFactory()
        val first = mockk<IContextProvider<SampleApi>>(relaxed = true)
        val second = mockk<IContextProvider<SampleApi>>(relaxed = true)
        factory.registerProvider(SampleApi::class.java, first)
        factory.registerProvider(SampleApi::class.java, second)
        verify(exactly = 1) { first.release() }
        assertSame(second, factory.getProvider(SampleApi::class.java))
    }

    @Test
    fun registerProviderTwiceWithSameInstanceDoesNotRelease() {
        val factory = ContextProviderFactory()
        val provider = mockk<IContextProvider<SampleApi>>(relaxed = true)
        factory.registerProvider(SampleApi::class.java, provider)
        factory.registerProvider(SampleApi::class.java, provider)
        verify(exactly = 0) { provider.release() }
    }

    @Test
    fun registerHolderAndRegisterWeakHolderHelpers() {
        val factory = ContextProviderFactory()
        val target = SampleImpl("x")
        factory.registerHolder(SampleApi::class.java, target)
        assertSame(target, factory.provideInstance(SampleApi::class.java))

        factory.registerWeakHolder(SampleApi::class.java, target)
        assertSame(target, factory.provideInstance(SampleApi::class.java))
    }

    @Test
    fun removeProviderReleasesAndDeletes() {
        val factory = ContextProviderFactory()
        val provider = mockk<IContextProvider<SampleApi>>(relaxed = true)
        factory.registerProvider(SampleApi::class.java, provider)
        factory.removeProvider(SampleApi::class.java)
        verify(exactly = 1) { provider.release() }
        assertFalse(factory.has(SampleApi::class.java))
        assertNull(factory.provideInstance(SampleApi::class.java))
    }

    @Test
    fun removeAllClearsState() {
        val factory = ContextProviderFactory()
        factory.registerHolder(SampleApi::class.java, SampleImpl("x"))
        assertTrue(factory.has(SampleApi::class.java))
        factory.removeAll()
        assertFalse(factory.has(SampleApi::class.java))
    }

    @Test
    fun keysReflectsRegisteredClasses() {
        val factory = ContextProviderFactory()
        factory.registerHolder(SampleApi::class.java, SampleImpl("x"))
        val keys = factory.keys().toList()
        assertEquals(1, keys.size)
        assertEquals(SampleApi::class.java, keys[0])
    }

    @Test
    fun mergeAndCopyTransferProviders() {
        val src = ContextProviderFactory()
        val target = SampleImpl("merged")
        src.registerHolder(SampleApi::class.java, target)

        val dst = ContextProviderFactory()
        dst.merge(src)
        assertSame(target, dst.provideInstance(SampleApi::class.java))

        val copy = src.copy()
        assertSame(target, copy.provideInstance(SampleApi::class.java))
        assertNotNull(copy.getProvider(SampleApi::class.java))
    }

    @Test
    fun provideInstanceReturnsNullWhenProviderReturnsNull() {
        val factory = ContextProviderFactory()
        factory.registerProvider(SampleApi::class.java) { null }
        assertNull(factory.provideInstance(SampleApi::class.java))
    }

    @Test
    fun bridgeCallThreadTypeConfigDefaultReturnsNull() {
        val cfg = BridgeCallThreadTypeConfig()
        val call = mockk<BridgeCall>(relaxed = true)
        assertNull(cfg.getBridgeCallThreadType("any", call))
    }

    @Test
    fun threadStringToThreadTypeEnumValidValues() {
        for (value in BridgeCallThreadTypeEnum.values()) {
            assertEquals(value, threadStringToThreadTypeEnum(value.name))
        }
    }

    @Test
    fun threadStringToThreadTypeEnumFallsBackToMainOnUnknown() {
        assertEquals(BridgeCallThreadTypeEnum.MAIN_THREAD, threadStringToThreadTypeEnum("foo"))
        assertEquals(BridgeCallThreadTypeEnum.MAIN_THREAD, threadStringToThreadTypeEnum(""))
    }
}
