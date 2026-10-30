// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.api

import com.tiktok.sparkling.method.protocol.entity.Platform
import com.lynx.react.bridge.JavaOnlyMap
import com.lynx.react.bridge.ReadableMap
import com.tiktok.sparkling.method.protocol.BridgeContext
import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeMethodCallback
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IBridgeContext
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.IDLMethodProvider
import com.tiktok.sparkling.method.registry.core.IDLMethodRegistry
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamModel
import com.tiktok.sparkling.method.registry.core.model.context.ContextProviderFactory
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseParamModel
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Host registry injection and instance-table access without changing default dispatch. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class BusinessCallHandlerExtensionTest {
    @Test
    fun injectedRegistryKeepsPreloadedProvidersAndReceivesBothRegistrationForms() {
        val registry = HostRegistry().apply { namespace = "old" }
        val preloaded = TrackingMethod()
        registry.registerMethodProvider("preloaded", IDLMethodProvider { preloaded }, BridgePlatformType.LYNX)
        val handler = HostHandler("business", registry)
        assertEquals("business", handler.nameSpace)
        assertEquals("business", registry.namespace)
        assertEquals(0, registry.nameLookups)

        handler.registerMethod(TrackingMethod::class.java, BridgePlatformType.WEB)
        val factory = TrackingMethod()
        handler.registerMethod("factory", BridgePlatformType.WEB) { factory }

        assertTrue(registry.nameLookups > 0)
        assertTrue(handler.isMethodExists("host.method", BridgePlatformType.WEB))
        assertFalse(handler.isMethodExists("tracking", BridgePlatformType.WEB))
        assertSame(TrackingMethod::class.java, registry.findMethodClass(BridgePlatformType.WEB, "host.method"))
        assertSame(factory, requireNotNull(registry.findMethodProvider(BridgePlatformType.WEB, "factory")).provideMethod())
        assertSame(factory, handler.getBridge(context(BridgePlatformType.WEB), "factory"))
        assertSame(preloaded, handler.getBridge(context(BridgePlatformType.LYNX), "preloaded"))

        val later = TrackingMethod()
        registry.registerMethodProvider("later", IDLMethodProvider { later })
        assertSame(later, handler.getBridge(context(BridgePlatformType.LYNX), "later"))
    }

    @Test
    fun handlingUsesTheInjectedFactoryAndRetainsTheHandledInstance() {
        val registry = HostRegistry()
        val handler = HostHandler("business", registry)
        val method = TrackingMethod()
        registry.registerMethodProvider("payload", IDLMethodProvider { method })
        val sdkContext = mockk<IBridgeContext>()
        handler.setBridgeContext(sdkContext)
        val context = context(BridgePlatformType.LYNX)
        val call = BridgeCall(context).apply {
            bridgeName = "payload"
            params = JavaOnlyMap().apply {
                putMap("data", JavaOnlyMap().apply { putString("value", "host") })
            }
            platform = Platform.Lynx
        }
        var result: Any? = null
        handler.handle(context, call, object : IBridgeMethodCallback {
            override fun onBridgeResult(parcel: Any) { result = parcel }
        })

        assertSame(sdkContext, method.receivedContext)
        assertEquals(mapOf("value" to "host"), method.receivedParams)
        assertEquals(IDLBridgeMethod.SUCCESS, (result as ReadableMap).getInt("code"))
        assertSame(method, handler.table()[BridgePlatformType.LYNX]?.get("payload"))
    }

    @Test
    fun hostReleaseCanVisitOnlyTheLatestInstancesBeforeTheSharedTableIsCleared() {
        val handler = HostHandler("business", HostRegistry())
        handler.registerMethod("method") { TrackingMethod() }
        val web = context(BridgePlatformType.WEB)
        val first = requireNotNull(handler.getBridge(web, "method")) as TrackingMethod
        val latest = requireNotNull(handler.getBridge(web, "method")) as TrackingMethod
        val lynx = requireNotNull(handler.getBridge(context(BridgePlatformType.LYNX), "method")) as TrackingMethod
        val table = handler.table()

        assertNotSame(first, latest)
        assertSame(latest, table[BridgePlatformType.WEB]?.get("method"))
        assertSame(lynx, table[BridgePlatformType.LYNX]?.get("method"))
        assertSame(table[BridgePlatformType.WEB], handler.slot(BridgePlatformType.WEB))
        assertEquals(2, table.size)
        handler.releaseRetainedMethods = true
        handler.onRelease()

        assertTrue(handler.isReleased())
        assertSame(table, handler.table())
        assertTrue(table.isEmpty())
        assertEquals(0, first.releaseCalls)
        assertEquals(1, latest.releaseCalls)
        assertEquals(1, lynx.releaseCalls)
        assertTrue(handler.isMethodExists("method"))
    }

    @Test
    fun concurrentFirstLookupsShareOnePlatformTableWithoutLosingDistinctNames() {
        val handler = HostHandler("business", HostRegistry())
        val count = 8
        val start = CyclicBarrier(count)
        repeat(count) { index -> handler.registerMethod("method.$index") { TrackingMethod() } }
        val context = context(BridgePlatformType.WEB)
        val executor = Executors.newFixedThreadPool(count)
        try {
            val futures = (0 until count).map { index ->
                executor.submit<Pair<IDLBridgeMethod, ConcurrentHashMap<String, IDLBridgeMethod>>> {
                    start.await(5, TimeUnit.SECONDS)
                    val method = requireNotNull(handler.getBridge(context, "method.$index"))
                    method to handler.slot(BridgePlatformType.WEB)
                }
            }
            val results = futures.map { it.get(10, TimeUnit.SECONDS) }
            val retained = handler.slot(BridgePlatformType.WEB)
            assertEquals(count, retained.size)
            results.forEachIndexed { index, (method, slot) ->
                assertSame(retained, slot)
                assertSame(method, retained["method.$index"])
            }
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun hostSubclassesCanReuseWrapperExtraction() {
        val handler = HostHandler("business", HostRegistry())
        val direct = JavaOnlyMap().apply { putString("value", "direct") }
        val wrapped = JavaOnlyMap().apply { putMap("data", direct) }
        assertNull(handler.unwrap(null))
        assertSame(direct, handler.unwrap(direct))
        assertSame(direct, handler.unwrap(wrapped))
    }

    private fun context(platform: BridgePlatformType): BridgeContext = BridgeContext().apply { this.platformType = platform }

    private class HostRegistry : IDLMethodRegistry() {
        var nameLookups = 0
        override fun resolveMethodName(clazz: Class<out IDLBridgeMethod>): String {
            nameLookups++
            return "host.method"
        }
    }

    private class HostHandler(namespace: String, registry: IDLMethodRegistry) : BusinessCallHandler(namespace, registry) {
        var releaseRetainedMethods = false
        fun table(): ConcurrentHashMap<BridgePlatformType, ConcurrentHashMap<String, IDLBridgeMethod>> = methodTable()
        fun slot(platform: BridgePlatformType): ConcurrentHashMap<String, IDLBridgeMethod> = getPlatformTypeCache(platform)
        fun unwrap(params: ReadableMap?): ReadableMap? = unWrapperParams(params)
        override fun onRelease() {
            if (releaseRetainedMethods) {
                methodTable().values.forEach { methods -> methods.values.forEach { it.release() } }
            }
            super.onRelease()
        }
    }

    class TrackingMethod : IDLBridgeMethod {
        override val name: String = "tracking"
        var receivedContext: IBridgeContext? = null
        var receivedParams: Map<String, Any?>? = null
        var releaseCalls = 0

        @IDLMethodParamModel
        interface Params : IDLMethodBaseParamModel

        override fun setBridgeContext(bridgeContext: IBridgeContext) { receivedContext = bridgeContext }
        override fun setProviderFactory(contextProviderFactory: ContextProviderFactory?) = Unit
        override fun realHandle(params: Map<String, Any?>, callback: IDLBridgeMethod.Callback, type: BridgePlatformType) {
            receivedParams = params
            callback.invoke(mapOf("code" to IDLBridgeMethod.SUCCESS))
        }
        override fun release() { releaseCalls++ }
    }
}
