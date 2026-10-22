// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamField
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseParamModel
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseResultModel
import java.lang.reflect.Method
import kotlin.reflect.KClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class IDLMethodRegistryCacheExtensionTest {
    @Test
    fun hostAnnotationsShareTraversalAndRemainVisibleToExistingCacheReaders() {
        val cache = HostCache()
        val containerID = "registry-cache-extension-test"
        IDLMethodRegistryCacheManager.registerIDLMethodRegistryCache(containerID, cache)
        try {
            assertEquals("host-parent", cache.find(HostChild::class.java))
            val data = requireNotNull(DefaultIDLParamModelReader.findAnnotationData(containerID, HostChild::class.java))
            assertSame(HostParent.Params::class.java, data.paramClass)
            assertSame(HostParent.Result::class.java, data.resultClass)
            assertEquals(setOf("custom", "standard", "nested"), data.methodParamModel.stringModel.keys)
            val custom = requireNotNull(data.methodParamModel.stringModel["custom"])
            assertTrue(custom.required)
            assertEquals(String::class.java, custom.returnType)
            assertEquals(HostSentinel::class, custom.nestedClassType)
            assertEquals(setOf(HostNested::class.java), data.models.keys)
            assertEquals(setOf("value"), data.models[HostNested::class.java]?.stringModel?.keys)
            assertEquals(setOf("result"), data.methodResultModel.stringModel.keys)
            assertSame(data, cache.getAnnotationDataByResultClass(HostParent.Result::class.java))
        } finally {
            IDLMethodRegistryCacheManager.unregisterIDLMethodRegistryCache(containerID)
        }
    }

    @Test
    fun aHostCacheAlsoReadsUnmodifiedSparklingMethods() {
        val cache = HostCache()
        val method = IDLMethodRegistryCacheTest.ParentMethod::class.java
        assertEquals("parent", cache.find(method))
        val data = requireNotNull(cache.BRIDGE_ANNOTATION_MAP[method])
        assertSame(IDLMethodRegistryCacheTest.ParentMethod.Params::class.java, data.paramClass)
        assertSame(IDLMethodRegistryCacheTest.ParentMethod.Result::class.java, data.resultClass)
        assertEquals(listOf("a", "b"), data.methodParamModel.stringModel["choice"]?.stringEnum)
        assertEquals(setOf(IDLMethodRegistryCacheTest.NestedModel::class.java), data.models.keys)
    }

    @Test
    fun successfulHostNameFallbackIsCachedWithoutRepeatingModelWork() {
        val cache = object : HostCache() {
            var fallbackCount = 0
            var cacheCount = 0

            override fun cacheMethodClass(clazz: Class<out IDLBridgeMethod>) {
                cacheCount++
                super.cacheMethodClass(clazz)
            }

            override fun onNameUnresolved(clazz: Class<out IDLBridgeMethod>): String {
                fallbackCount++
                return "host-fallback"
            }
        }
        val method = IDLMethodRegistryCacheTest.UnnamedMethod::class.java
        repeat(2) { assertEquals("host-fallback", cache.find(method)) }
        assertEquals(1, cache.fallbackCount)
        assertEquals(1, cache.cacheCount)
    }

    @Test
    fun hostLookupErrorsReachTheReportingHookAndReturnAnEmptyName() {
        val failure = IllegalStateException("host lookup failed")
        val method = IDLMethodRegistryCacheTest.OwnMethod::class.java
        val cache = object : HostCache() {
            override val TAG = "HostCache"
            var reportedClass: Class<out IDLBridgeMethod>? = null
            var reportedMessage: String? = null
            var reportedError: Throwable? = null

            override fun readMethodName(clazz: Class<out IDLBridgeMethod>): String? = throw failure

            override fun onFindFailed(clazz: Class<out IDLBridgeMethod>, errorMsg: String, e: Throwable) {
                reportedClass = clazz
                reportedMessage = errorMsg
                reportedError = e
            }
        }
        assertEquals("", cache.find(method))
        assertSame(method, cache.reportedClass)
        assertSame(failure, cache.reportedError)
        assertEquals("HostCache.find failed,clazz == ${method.name}, e == host lookup failed", cache.reportedMessage)
    }

    /** Mimics a host adapting its generated annotations while reusing the cache algorithm. */
    private open class HostCache : IDLMethodRegistryCache() {
        override fun cacheMethodClass(clazz: Class<out IDLBridgeMethod>) {
            addAnnotationCache(clazz)
        }

        override fun readMethodName(clazz: Class<out IDLBridgeMethod>): String? =
            readNameFrom(clazz, HostName::class.java) { it.name } ?: super.readMethodName(clazz)

        override fun findParamModelClass(clazz: Class<out IDLBridgeMethod>): Class<*>? =
            findModelClassByAnnotation(clazz, HostParams::class.java) ?: super.findParamModelClass(clazz)

        override fun findResultModelClass(clazz: Class<out IDLBridgeMethod>): Class<*>? =
            findModelClassByAnnotation(clazz, HostResult::class.java) ?: super.findResultModelClass(clazz)

        override fun isDefaultNestedType(type: KClass<out IDLMethodBaseModel>): Boolean =
            type == HostSentinel::class || super.isDefaultNestedType(type)

        override fun readParamField(method: Method): IDLParamField? {
            val field = method.getAnnotation(HostField::class.java) ?: return super.readParamField(method)
            return IDLParamField(
                required = field.required,
                keyPath = field.keyPath,
                nestedClassType = field.nestedClassType,
                returnType = method.returnType,
            )
        }
    }

    @Target(AnnotationTarget.FIELD)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class HostName(val name: String)

    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class HostParams

    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class HostResult

    @Target(AnnotationTarget.PROPERTY_GETTER)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class HostField(
        val keyPath: String,
        val required: Boolean = false,
        val nestedClassType: KClass<out IDLMethodBaseModel> = HostSentinel::class,
    )

    interface HostSentinel : IDLMethodBaseModel

    interface HostNested : IDLMethodBaseModel {
        @get:HostField(keyPath = "value")
        val value: Int
    }

    abstract class HostParent : IDLBridgeMethod {
        @field:HostName("host-parent")
        val declaredName = "unused"

        @HostParams
        interface Params : IDLMethodBaseParamModel {
            @get:HostField(keyPath = "custom", required = true)
            val custom: String

            @get:IDLMethodParamField(keyPath = "standard")
            val standard: Boolean

            @get:HostField(keyPath = "nested", nestedClassType = HostNested::class)
            val nested: HostNested
        }

        @HostResult
        interface Result : IDLMethodBaseResultModel {
            @get:HostField(keyPath = "result")
            val result: String
        }
    }

    abstract class HostChild : HostParent() {
        @field:HostName("host-child")
        val childName = "unused"

        @HostParams
        interface Params : IDLMethodBaseParamModel

        @HostResult
        interface Result : IDLMethodBaseResultModel
    }
}
