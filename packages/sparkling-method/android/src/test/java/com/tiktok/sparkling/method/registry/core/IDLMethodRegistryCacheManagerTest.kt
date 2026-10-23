// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Host installation and concurrent lookup contracts of the migrated internal manager. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class IDLMethodRegistryCacheManagerTest {
    private val cacheField = IDLMethodRegistryCacheManager::class.java.getDeclaredField("processWideCache").apply { isAccessible = true }
    private var previousCache: Any? = null
    private val container = "installed-cache-test"

    @Before
    fun setUp() {
        previousCache = cacheField.get(null)
        cacheField.set(null, null)
    }

    @After
    fun tearDown() {
        IDLMethodRegistryCacheManager.unregisterIDLMethodRegistryCache(container)
        cacheField.set(null, previousCache)
    }

    @Test
    fun installingBeforeTheFirstLookupIsVisibleThroughBothEntryPoints() {
        val installed = HostCache()
        IDLMethodRegistryCacheManager.installProcessWideCache(installed)
        assertSame(installed, IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(null))
        assertSame(installed, IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(container))
        assertSame(installed, IDLRegistryCache.getRealIDLRegistryCache())
        assertEquals("installed.method", IDLRegistryCache.find(Method::class.java))
        assertEquals(listOf(Method::class.java), installed.cachedMethods)
    }

    @Test
    fun replacingAfterLegacyLookupUpdatesTheFacadeWithoutReplacingLocalCaches() {
        val original = IDLRegistryCache.getRealIDLRegistryCache()
        val originalAnnotations = IDLRegistryCache.BRIDGE_ANNOTATION_MAP
        val local = IDLMethodRegistryCache()
        IDLMethodRegistryCacheManager.registerIDLMethodRegistryCache(container, local)
        val installed = HostCache()
        val data = IDLAnnotationData(String::class.java, Result::class.java, IDLAnnotationModel(), IDLAnnotationModel())
        installed.BRIDGE_RESULT_MAP[Result::class.java] = Method::class.java
        installed.BRIDGE_ANNOTATION_MAP[Method::class.java] = data
        IDLMethodRegistryCacheManager.installProcessWideCache(installed)
        assertSame(installed, IDLRegistryCache.getRealIDLRegistryCache())
        assertSame(installed.BRIDGE_ANNOTATION_MAP, IDLRegistryCache.BRIDGE_ANNOTATION_MAP)
        assertSame(installed.BRIDGE_RESULT_MAP, IDLRegistryCache.BRIDGE_RESULT_MAP)
        assertSame(data, IDLRegistryCache.getAnnotationDataByResultClass(Result::class.java))
        assertSame(originalAnnotations, original.BRIDGE_ANNOTATION_MAP)
        assertSame(local, IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(container))
        IDLMethodRegistryCacheManager.unregisterIDLMethodRegistryCache(container)
        assertSame(installed, IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(container))
    }

    @Test
    fun aLaterInstallationReplacesTheEarlierHostCache() {
        IDLMethodRegistryCacheManager.installProcessWideCache(HostCache())
        val latest = HostCache()
        IDLMethodRegistryCacheManager.installProcessWideCache(latest)
        assertSame(latest, IDLRegistryCache.getRealIDLRegistryCache())
        assertSame(latest, IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(null))
    }

    @Test
    fun concurrentFirstLookupsShareOneFallbackInstance() {
        val executor = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)
        try {
            val calls = (1..8).map {
                executor.submit<IDLMethodRegistryCache> {
                    assertTrue(start.await(10, TimeUnit.SECONDS))
                    IDLRegistryCache.getRealIDLRegistryCache()
                }
            }
            start.countDown()
            val results = calls.map { it.get(10, TimeUnit.SECONDS) }
            results.forEach { assertSame(results.first(), it) }
            assertSame(results.first(), IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(null))
        } finally {
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))
        }
    }

    @Test
    fun anInstallationRacingWithFallbackCreationIsNeverOverwritten() {
        val executor = Executors.newFixedThreadPool(2)
        try {
            repeat(50) {
                cacheField.set(null, null)
                val installed = HostCache()
                val start = CountDownLatch(1)
                val lookup = executor.submit<IDLMethodRegistryCache> {
                    assertTrue(start.await(10, TimeUnit.SECONDS))
                    IDLRegistryCache.getRealIDLRegistryCache()
                }
                val install = executor.submit {
                    assertTrue(start.await(10, TimeUnit.SECONDS))
                    IDLMethodRegistryCacheManager.installProcessWideCache(installed)
                }
                start.countDown()
                lookup.get(10, TimeUnit.SECONDS)
                install.get(10, TimeUnit.SECONDS)
                assertSame(installed, IDLRegistryCache.getRealIDLRegistryCache())
            }
        } finally {
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))
        }
    }

    private class HostCache : IDLMethodRegistryCache() {
        val cachedMethods = mutableListOf<Class<out IDLBridgeMethod>>()

        override fun cacheMethodClass(clazz: Class<out IDLBridgeMethod>) {
            cachedMethods.add(clazz)
        }

        override fun readMethodName(clazz: Class<out IDLBridgeMethod>): String = "installed.method"
    }

    abstract class Method : IDLBridgeMethod

    class Result
}
