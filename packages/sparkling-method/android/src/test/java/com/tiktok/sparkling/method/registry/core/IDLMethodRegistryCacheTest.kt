// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

import com.tiktok.sparkling.method.registry.api.SparklingBridge
import com.tiktok.sparkling.method.registry.api.util.ThreadPool
import com.tiktok.sparkling.method.registry.core.annotation.DefaultType
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodIntEnum
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodName
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamField
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamModel
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodResultModel
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodStringEnum
import com.tiktok.sparkling.method.registry.core.annotation.MethodParamDefaultValue
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseParamModel
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseResultModel
import java.util.ArrayDeque
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/** Contracts for deferred parsing, failure hooks, and cache publication. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class IDLMethodRegistryCacheTest {
    private val executorField = ThreadPool::class.java.getDeclaredField("executor").apply { isAccessible = true }
    private var previousExecutor: Any? = null
    private lateinit var executor: QueuedExecutor

    @Before
    fun setUp() {
        previousExecutor = executorField.get(null)
        executor = QueuedExecutor()
        ThreadPool.configExecutorService(executor)
    }

    @After
    fun tearDown() {
        executor.shutdownNow()
        executorField.set(null, previousExecutor)
    }

    @Test
    fun modelCachingStaysDeferredAndPreservesAllFieldMetadata() {
        val cache = IDLMethodRegistryCache()
        cache.cacheMethodClass(ParentMethod::class.java)
        assertEquals(1, executor.pendingCount)
        assertTrue(cache.BRIDGE_ANNOTATION_MAP.isEmpty())
        assertTrue(cache.BRIDGE_RESULT_MAP.isEmpty())
        executor.runPending()

        val data = requireNotNull(cache.BRIDGE_ANNOTATION_MAP[ParentMethod::class.java])
        val choice = requireNotNull(data.methodParamModel.stringModel["choice"])
        assertTrue(choice.required)
        assertTrue(choice.isEnum)
        assertTrue(choice.isGetter)
        assertEquals(String::class, choice.primitiveClassType)
        assertEquals(String::class.java, choice.returnType)
        assertEquals(listOf("a", "b"), choice.stringEnum)
        assertEquals(emptyList<Int>(), choice.intEnum)
        assertEquals(IDLDefaultValue(DefaultType.STRING, 2.5, "a", 7, true, 9L), choice.defaultValue)
        assertEquals(listOf(1, 2), data.methodParamModel.stringModel["count"]?.intEnum)

        val setter = ParentMethod.Params::class.java.getDeclaredMethod("setMutable", String::class.java)
        assertFalse(requireNotNull(data.methodParamModel.methodModel[setter]).isGetter)
        assertTrue(requireNotNull(data.methodParamModel.stringModel["mutable"]).isGetter)
        assertFalse(data.methodParamModel.stringModel.containsKey("unannotated"))
        assertEquals(setOf(NestedModel::class.java), data.models.keys)
        assertEquals(setOf("nestedValue"), data.models[NestedModel::class.java]?.stringModel?.keys)
        assertEquals(setOf("result"), data.methodResultModel.stringModel.keys)
        assertSame(data, cache.getAnnotationDataByResultClass(ParentMethod.Result::class.java))
        assertNull(cache.getAnnotationDataByResultClass(String::class.java))
    }

    @Test
    fun superclassNameAndModelsTakePriorityAndANameHitDoesNotReschedule() {
        val cache = IDLMethodRegistryCache()
        assertEquals("parent", cache.find(ChildMethod::class.java))
        assertEquals("parent", cache.find(ChildMethod::class.java))
        assertEquals(1, executor.pendingCount)
        executor.runPending()
        val data = requireNotNull(cache.BRIDGE_ANNOTATION_MAP[ChildMethod::class.java])
        assertSame(ParentMethod.Params::class.java, data.paramClass)
        assertSame(ParentMethod.Result::class.java, data.resultClass)
    }

    @Test
    fun ownAnnotationsAreUsedWhenTheSuperclassHasNone() {
        val cache = IDLMethodRegistryCache()
        assertEquals("own", cache.find(OwnMethod::class.java))
        executor.runPending()
        val data = requireNotNull(cache.BRIDGE_ANNOTATION_MAP[OwnMethod::class.java])
        assertSame(OwnMethod.Params::class.java, data.paramClass)
        assertSame(OwnMethod.Result::class.java, data.resultClass)
    }

    @Test
    fun unresolvedNamesAreNotInstantiatedAndEmptyAnnotatedNamesStayCached() {
        val cache = IDLMethodRegistryCache()
        repeat(2) { assertEquals("", cache.find(UnnamedMethod::class.java)) }
        assertEquals(2, executor.pendingCount)
        repeat(2) { assertEquals("", cache.find(EmptyNameMethod::class.java)) }
        assertEquals(3, executor.pendingCount)
    }

    @Test
    fun aMissingModelDoesNotPublishPartialMetadata() {
        val cache = IDLMethodRegistryCache()
        cache.cacheMethodClass(MissingResultMethod::class.java)
        executor.runPending()
        assertTrue(cache.BRIDGE_ANNOTATION_MAP.isEmpty())
        assertTrue(cache.BRIDGE_RESULT_MAP.isEmpty())
    }

    @Test
    fun failedNameLookupLogsAndReturnsEmpty() {
        val cache = IDLMethodRegistryCache()
        val previousDebugEnv = SparklingBridge.isDebugEnv
        try {
            SparklingBridge.isDebugEnv = true
            // An interface has no superclass: preserve the existing lookup failure and debug log.
            assertEquals("", cache.find(IDLBridgeMethod::class.java))
            assertTrue(ShadowLog.getLogsForTag("IDLMethodRegistryCache").any {
                it.msg.startsWith("IDLMethodRegistryCache.find failed,clazz == ${IDLBridgeMethod::class.java.name}, e == ")
            })
        } finally {
            SparklingBridge.isDebugEnv = previousDebugEnv
        }
    }

    @Test
    fun subclassesReceiveFailureDetailsAndCallingSuperDoesNotReport() {
        val failure = IllegalStateException("name reader failed")
        val cache = object : IDLMethodRegistryCache() {
            override val TAG = "CustomCache"
            var reportedError: Throwable? = null
            var reportedMessage: String? = null
            var failureCount = 0

            override fun readMethodName(clazz: Class<out IDLBridgeMethod>): String? = throw failure

            override fun onFindFailed(clazz: Class<out IDLBridgeMethod>, errorMsg: String, e: Throwable) {
                reportedError = e
                reportedMessage = errorMsg
                failureCount++
                super.onFindFailed(clazz, errorMsg, e)
            }
        }
        val method = OwnMethod::class.java
        assertEquals("", cache.find(method))
        assertSame(failure, cache.reportedError)
        assertEquals(
            "CustomCache.find failed,clazz == ${method.name}, e == name reader failed",
            cache.reportedMessage,
        )
        assertEquals(1, cache.failureCount)
        assertEquals(1, executor.pendingCount)
    }

    @Test
    fun rejectedSchedulingPropagatesFromCacheAndFindReturnsEmptyWithoutReporting() {
        executor.shutdown()
        val cache = IDLMethodRegistryCache()
        val method = OwnMethod::class.java
        assertTrue(runCatching { cache.cacheMethodClass(method) }.exceptionOrNull() is RejectedExecutionException)
        assertEquals("", cache.find(method))
        assertTrue(cache.BRIDGE_ANNOTATION_MAP.isEmpty())
        assertTrue(cache.BRIDGE_RESULT_MAP.isEmpty())
    }

    @Test
    fun backgroundParsingFailureDoesNotTurnAResolvedNameIntoARegistrationFailure() {
        val failure = IllegalStateException("model reader failed")
        val cache = object : IDLMethodRegistryCache() {
            override fun findParamModelClass(clazz: Class<out IDLBridgeMethod>): Class<*>? = throw failure
        }
        assertEquals("own", cache.find(OwnMethod::class.java))
        assertEquals(1, executor.pendingCount)
        val executionFailure = runCatching { executor.runPending() }.exceptionOrNull()
        assertTrue(executionFailure is java.util.concurrent.ExecutionException)
        assertSame(failure, executionFailure?.cause)
        assertTrue(cache.BRIDGE_ANNOTATION_MAP.isEmpty())
        assertTrue(cache.BRIDGE_RESULT_MAP.isEmpty())
    }

    @Test
    fun hostOverridesCanReplaceDefaultSchedulingAndHandleFailures() {
        val failure = IllegalStateException("host name reader failed")
        val cache = object : IDLMethodRegistryCache() {
            var reportedError: Throwable? = null

            override fun cacheMethodClass(clazz: Class<out IDLBridgeMethod>) {
                addAnnotationCache(clazz)
            }

            override fun readMethodName(clazz: Class<out IDLBridgeMethod>): String? = throw failure

            override fun onFindFailed(clazz: Class<out IDLBridgeMethod>, errorMsg: String, e: Throwable) {
                reportedError = e
            }
        }
        assertEquals("", cache.find(OwnMethod::class.java))
        assertSame(failure, cache.reportedError)
        assertEquals(0, executor.pendingCount)
        assertTrue(cache.BRIDGE_ANNOTATION_MAP.containsKey(OwnMethod::class.java))
    }

    @Test
    fun globalAndContainerRegistriesRetainDeferredModelCaching() {
        val container = "cache-registry-test"
        val field = IDLMethodRegistryCacheManager::class.java.getDeclaredField("processWideCache")
            .apply { isAccessible = true }
        val previous = field.get(null)
        field.set(null, null)
        try {
            val context = BridgeContextWrapper().apply { setContainerID(container) }
            val localRegistry = IDLMethodRegistry(true, context, false)
            val globalRegistry = IDLMethodRegistry()
            val provider = IDLMethodProvider { error("Registration must not instantiate the method") }
            val method = OwnMethod::class.java
            globalRegistry.registerMethodProvider("global", provider, BridgePlatformType.LYNX, method)
            localRegistry.registerMethodProvider("local", provider, BridgePlatformType.LYNX, method)
            val global = requireNotNull(IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(null))
            val local = requireNotNull(IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(container))
            assertTrue(global !== local)
            assertEquals(2, executor.pendingCount)
            assertTrue(global.BRIDGE_ANNOTATION_MAP.isEmpty())
            assertTrue(local.BRIDGE_ANNOTATION_MAP.isEmpty())
            executor.runPending()
            assertTrue(global.BRIDGE_ANNOTATION_MAP.containsKey(method))
            assertTrue(local.BRIDGE_ANNOTATION_MAP.containsKey(method))
            assertSame(global.BRIDGE_ANNOTATION_MAP[method], global.getAnnotationDataByResultClass(OwnMethod.Result::class.java))
            assertSame(local.BRIDGE_ANNOTATION_MAP[method], local.getAnnotationDataByResultClass(OwnMethod.Result::class.java))
        } finally {
            IDLMethodRegistryCacheManager.unregisterIDLMethodRegistryCache(container)
            field.set(null, previous)
        }
    }

    abstract class ParentMethod : IDLBridgeMethod {
        @field:IDLMethodName(name = "parent")
        val declaredName = "unused"

        @IDLMethodParamModel
        interface Params : IDLMethodBaseParamModel {
            @get:IDLMethodParamField(
                keyPath = "choice", required = true, isEnum = true, primitiveClassType = String::class,
                defaultValue = MethodParamDefaultValue(DefaultType.STRING, 2.5, "a", 7, true, 9L),
            )
            @get:IDLMethodStringEnum("a", "b")
            val choice: String

            @get:IDLMethodParamField(keyPath = "count", isEnum = true)
            @get:IDLMethodIntEnum(1, 2)
            val count: Int

            @get:IDLMethodParamField(keyPath = "nested", nestedClassType = NestedModel::class)
            val nested: NestedModel

            @get:IDLMethodParamField(keyPath = "mutable")
            @set:IDLMethodParamField(keyPath = "mutable", isGetter = false)
            var mutable: String

            val unannotated: String
        }

        @IDLMethodResultModel
        interface Result : IDLMethodBaseResultModel {
            @get:IDLMethodParamField(keyPath = "result")
            val result: Boolean
        }
    }

    interface NestedModel : IDLMethodBaseModel {
        @get:IDLMethodParamField(keyPath = "nestedValue")
        val nestedValue: Long
    }

    abstract class ChildMethod : ParentMethod() {
        @field:IDLMethodName(name = "child")
        val childName = "unused"

        @IDLMethodParamModel
        interface Params : IDLMethodBaseParamModel

        @IDLMethodResultModel
        interface Result : IDLMethodBaseResultModel
    }

    abstract class OwnMethod : IDLBridgeMethod {
        @field:IDLMethodName(name = "own")
        val declaredName = "unused"

        @IDLMethodParamModel
        interface Params : IDLMethodBaseParamModel

        @IDLMethodResultModel
        interface Result : IDLMethodBaseResultModel
    }

    abstract class UnnamedMethod : IDLBridgeMethod {
        override val name: String = "instance-name-must-not-be-used"
    }

    abstract class EmptyNameMethod : IDLBridgeMethod {
        @field:IDLMethodName(name = "")
        val declaredName = "unused"
    }

    abstract class MissingResultMethod : IDLBridgeMethod {
        @IDLMethodParamModel
        interface Params : IDLMethodBaseParamModel
    }

    private class QueuedExecutor : AbstractExecutorService() {
        private val pending = ArrayDeque<Runnable>()
        private var stopped = false
        val pendingCount: Int get() = pending.size

        override fun execute(command: Runnable) {
            if (stopped) throw RejectedExecutionException()
            pending.addLast(command)
        }

        fun runPending() {
            while (pending.isNotEmpty()) {
                val task = pending.removeFirst()
                task.run()
                (task as? Future<*>)?.get()
            }
        }

        override fun shutdown() { stopped = true }
        override fun isShutdown(): Boolean = stopped
        override fun isTerminated(): Boolean = stopped && pending.isEmpty()
        override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean = isTerminated
        override fun shutdownNow(): MutableList<Runnable> {
            stopped = true
            return pending.toMutableList().also { pending.clear() }
        }
    }
}
