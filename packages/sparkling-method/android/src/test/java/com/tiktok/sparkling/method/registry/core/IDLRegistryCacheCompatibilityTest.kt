// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

import com.tiktok.sparkling.method.registry.api.util.ThreadPool
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodName
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.Runs
import io.mockk.unmockkObject
import java.lang.ref.WeakReference
import java.lang.reflect.Modifier
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Existing cache contracts, run unchanged before and after the shared-manager migration. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class IDLRegistryCacheCompatibilityTest {
    private val container = "cache-compatibility"

    @After
    fun tearDown() {
        IDLMethodRegistryCacheManager.unregisterIDLMethodRegistryCache(container)
        IDLMethodRegistryCacheManager.unregisterIDLMethodRegistryCache("")
    }

    @Test
    fun nullAndUnknownContainersShareTheLegacyGlobalCache() {
        val global = IDLRegistryCache.getRealIDLRegistryCache()
        assertSame(global, IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(null))
        assertSame(global, IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(container))
        assertSame(global, IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(""))
        assertSame(global, IDLRegistryCache.getRealIDLRegistryCache())
        assertEquals("IDLRegistryCache", IDLRegistryCache.TAG)
    }

    @Test
    fun localCacheRegistrationReplacementAndRemovalStayContainerScoped() {
        val global = IDLRegistryCache.getRealIDLRegistryCache()
        val first = IDLMethodRegistryCache()
        val replacement = IDLMethodRegistryCache()
        IDLMethodRegistryCacheManager.registerIDLMethodRegistryCache(container, first)
        assertSame(first, IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(container))
        IDLMethodRegistryCacheManager.registerIDLMethodRegistryCache(container, replacement)
        assertSame(replacement, IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(container))
        assertSame(global, IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(null))
        IDLMethodRegistryCacheManager.unregisterIDLMethodRegistryCache(container)
        IDLMethodRegistryCacheManager.unregisterIDLMethodRegistryCache(container)
        assertSame(global, IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(container))
    }

    @Test
    fun emptyContainerIdentifierRemainsAValidLocalRegistration() {
        val local = IDLMethodRegistryCache()
        IDLMethodRegistryCacheManager.registerIDLMethodRegistryCache("", local)
        assertSame(local, IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(""))
        assertSame(IDLRegistryCache.getRealIDLRegistryCache(), IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(null))
    }

    @Test
    fun collectedContainerCacheFallsBackToTheSameGlobalInstance() {
        val local = IDLMethodRegistryCache()
        IDLMethodRegistryCacheManager.registerIDLMethodRegistryCache(container, local)
        val field = IDLMethodRegistryCacheManager::class.java.getDeclaredField("map").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val entries = field.get(null) as Map<String, WeakReference<IDLMethodRegistryCache>>
        val reference = requireNotNull(entries[container])
        assertSame(local, reference.get())
        // Deterministically simulate collection without relying on System.gc().
        reference.clear()
        assertSame(IDLRegistryCache.getRealIDLRegistryCache(), IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(container))
    }

    @Test
    fun legacyMapGettersAndResultLookupUseTheManagerCache() {
        val global = requireNotNull(IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(null))
        assertSame(global.BRIDGE_ANNOTATION_MAP, IDLRegistryCache.BRIDGE_ANNOTATION_MAP)
        assertSame(global.BRIDGE_RESULT_MAP, IDLRegistryCache.BRIDGE_RESULT_MAP)
        val data = IDLAnnotationData(String::class.java, Result::class.java, IDLAnnotationModel(), IDLAnnotationModel())
        assertNull(IDLRegistryCache.getAnnotationDataByResultClass(Result::class.java))
        try {
            IDLRegistryCache.BRIDGE_RESULT_MAP[Result::class.java] = Method::class.java
            assertNull(IDLRegistryCache.getAnnotationDataByResultClass(Result::class.java))
            global.BRIDGE_ANNOTATION_MAP[Method::class.java] = data
            assertSame(data, IDLRegistryCache.getAnnotationDataByResultClass(Result::class.java))
        } finally {
            global.BRIDGE_RESULT_MAP.remove(Result::class.java)
            global.BRIDGE_ANNOTATION_MAP.remove(Method::class.java)
        }
    }

    @Test
    fun legacyFindRetainsTheStaticJvmEntryPointAndNameResolution() {
        mockkObject(ThreadPool)
        try {
            every { ThreadPool.runInBackGround(any()) } just Runs
            val find = IDLRegistryCache::class.java.getDeclaredMethod("find", Class::class.java)
            assertTrue(Modifier.isStatic(find.modifiers))
            assertEquals("cache.compatibility", find.invoke(null, Method::class.java))
            assertEquals("cache.compatibility", IDLRegistryCache.find(Method::class.java))
        } finally {
            unmockkObject(ThreadPool)
        }
    }

    abstract class Method : IDLBridgeMethod {
        @field:IDLMethodName(name = "cache.compatibility")
        val declaredName = "cache.compatibility"
    }

    class Result
}
