// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

import com.tiktok.sparkling.method.registry.api.BridgeSettings
import com.tiktok.sparkling.method.registry.core.model.context.ContextProviderFactory
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class IDLMethodRegistryTest {
    private lateinit var registry: IDLMethodRegistry
    private lateinit var mockBridgeContext: IBridgeContext

    // Create concrete test classes for different method names
    private class TestMethod1 : IDLBridgeMethod {
        override val name: String = "testMethod"
        override val compatibility: IDLBridgeMethod.Compatibility = IDLBridgeMethod.Compatibility.Compatible

        override fun realHandle(
            params: Map<String, Any?>,
            callback: IDLBridgeMethod.Callback,
            type: BridgePlatformType,
        ) {}

        override fun setProviderFactory(contextProviderFactory: ContextProviderFactory?) {}

        override fun setBridgeContext(bridgeContext: IBridgeContext) {}
    }

    private class TestMethod2 : IDLBridgeMethod {
        override val name: String = "webMethod"
        override val compatibility: IDLBridgeMethod.Compatibility = IDLBridgeMethod.Compatibility.Compatible

        override fun realHandle(
            params: Map<String, Any?>,
            callback: IDLBridgeMethod.Callback,
            type: BridgePlatformType,
        ) {}

        override fun setProviderFactory(contextProviderFactory: ContextProviderFactory?) {}

        override fun setBridgeContext(bridgeContext: IBridgeContext) {}
    }

    private class TestMethod3 : IDLBridgeMethod {
        override val name: String = "optimizedMethod"
        override val compatibility: IDLBridgeMethod.Compatibility = IDLBridgeMethod.Compatibility.Compatible

        override fun realHandle(
            params: Map<String, Any?>,
            callback: IDLBridgeMethod.Callback,
            type: BridgePlatformType,
        ) {}

        override fun setProviderFactory(contextProviderFactory: ContextProviderFactory?) {}

        override fun setBridgeContext(bridgeContext: IBridgeContext) {}
    }

    private class TestMethod4 : IDLBridgeMethod {
        override val name: String = "findableMethod"
        override val compatibility: IDLBridgeMethod.Compatibility = IDLBridgeMethod.Compatibility.Compatible

        override fun realHandle(
            params: Map<String, Any?>,
            callback: IDLBridgeMethod.Callback,
            type: BridgePlatformType,
        ) {}

        override fun setProviderFactory(contextProviderFactory: ContextProviderFactory?) {}

        override fun setBridgeContext(bridgeContext: IBridgeContext) {}
    }

    private class TestMethod5 : IDLBridgeMethod {
        override val name: String = "existingMethod"
        override val compatibility: IDLBridgeMethod.Compatibility = IDLBridgeMethod.Compatibility.Compatible

        override fun realHandle(
            params: Map<String, Any?>,
            callback: IDLBridgeMethod.Callback,
            type: BridgePlatformType,
        ) {}

        override fun setProviderFactory(contextProviderFactory: ContextProviderFactory?) {}

        override fun setBridgeContext(bridgeContext: IBridgeContext) {}
    }

    private class TestMethod6 : IDLBridgeMethod {
        override val name: String = "method1"
        override val compatibility: IDLBridgeMethod.Compatibility = IDLBridgeMethod.Compatibility.Compatible

        override fun realHandle(
            params: Map<String, Any?>,
            callback: IDLBridgeMethod.Callback,
            type: BridgePlatformType,
        ) {}

        override fun setProviderFactory(contextProviderFactory: ContextProviderFactory?) {}

        override fun setBridgeContext(bridgeContext: IBridgeContext) {}
    }

    private class TestMethod7 : IDLBridgeMethod {
        override val name: String = "method2"
        override val compatibility: IDLBridgeMethod.Compatibility = IDLBridgeMethod.Compatibility.Compatible

        override fun realHandle(
            params: Map<String, Any?>,
            callback: IDLBridgeMethod.Callback,
            type: BridgePlatformType,
        ) {}

        override fun setProviderFactory(contextProviderFactory: ContextProviderFactory?) {}

        override fun setBridgeContext(bridgeContext: IBridgeContext) {}
    }

    private class TestMethod8 : IDLBridgeMethod {
        override val name: String = "nullCacheMethod"
        override val compatibility: IDLBridgeMethod.Compatibility = IDLBridgeMethod.Compatibility.Compatible

        override fun realHandle(
            params: Map<String, Any?>,
            callback: IDLBridgeMethod.Callback,
            type: BridgePlatformType,
        ) {}

        override fun setProviderFactory(contextProviderFactory: ContextProviderFactory?) {}

        override fun setBridgeContext(bridgeContext: IBridgeContext) {}
    }

    private class TestMethod9 : IDLBridgeMethod {
        override val name: String = "localMethod"
        override val compatibility: IDLBridgeMethod.Compatibility = IDLBridgeMethod.Compatibility.Compatible

        override fun realHandle(
            params: Map<String, Any?>,
            callback: IDLBridgeMethod.Callback,
            type: BridgePlatformType,
        ) {}

        override fun setProviderFactory(contextProviderFactory: ContextProviderFactory?) {}

        override fun setBridgeContext(bridgeContext: IBridgeContext) {}
    }

    private class TestMethodEmpty : IDLBridgeMethod {
        override val name: String = ""
        override val compatibility: IDLBridgeMethod.Compatibility = IDLBridgeMethod.Compatibility.Compatible

        override fun realHandle(
            params: Map<String, Any?>,
            callback: IDLBridgeMethod.Callback,
            type: BridgePlatformType,
        ) {}

        override fun setProviderFactory(contextProviderFactory: ContextProviderFactory?) {}

        override fun setBridgeContext(bridgeContext: IBridgeContext) {}
    }

    @Before
    fun setUp() {
        mockBridgeContext = mockk()

        // Mock BridgeSettings
        mockkObject(BridgeSettings)
        every { BridgeSettings.bridgeRegistryOptimize } returns false

        // Mock IDLMethodRegistryCacheManager
        mockkObject(IDLMethodRegistryCacheManager)
        every { IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(any()) } returns null
        every { IDLMethodRegistryCacheManager.registerIDLMethodRegistryCache(any(), any()) } returns Unit
        every { IDLMethodRegistryCacheManager.unregisterIDLMethodRegistryCache(any()) } returns Unit

        every { mockBridgeContext.containerID } returns "test_container"

        registry = IDLMethodRegistry()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testDefaultConstructor() {
        // Given & When
        val defaultRegistry = IDLMethodRegistry()

        // Then
        assertEquals("Default namespace should be 'DEFAULT'", "DEFAULT", defaultRegistry.namespace)
    }

    @Test
    fun testConstructorWithParameters() {
        // Given & When
        val localRegistry = IDLMethodRegistry(isLocalBridgeRegistry = true, bridgeContext = mockBridgeContext)

        // Then
        assertEquals("Default namespace should be 'DEFAULT'", "DEFAULT", localRegistry.namespace)
        verify { IDLMethodRegistryCacheManager.registerIDLMethodRegistryCache("test_container", any()) }
    }

    @Test
    fun testCopyWithFunction() {
        // Given
        val originalRegistry = IDLMethodRegistry()
        originalRegistry.namespace = "CUSTOM_NAMESPACE"

        // When
        val copiedRegistry = IDLMethodRegistry.copyWith(originalRegistry)

        // Then
        assertEquals("Copied registry should have same namespace", "CUSTOM_NAMESPACE", copiedRegistry.namespace)
    }

    @Test
    fun testRegisterMethodWithAllScope() {
        // Given
        val testMethod = createMockIDLMethod("testMethod")
        every { IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(null) } returns createMockRegistryCache("testMethod")

        // When
        registry.registerMethod(testMethod, BridgePlatformType.ALL)

        // Then - Should register for ALL, WEB, and LYNX platforms
        assertTrue("Method should be registered for ALL", registry.isMethodExists("testMethod", BridgePlatformType.ALL))
        assertTrue("Method should be registered for WEB", registry.isMethodExists("testMethod", BridgePlatformType.WEB))
        assertTrue("Method should be registered for LYNX", registry.isMethodExists("testMethod", BridgePlatformType.LYNX))
    }

    @Test
    fun testRegisterMethodWithSpecificScope() {
        // Given
        val testMethod = createMockIDLMethod("webMethod")
        every { IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(null) } returns createMockRegistryCache("webMethod")

        // When
        registry.registerMethod(testMethod, BridgePlatformType.WEB)

        // Then - Should only register for WEB platform
        assertTrue("Method should be registered for WEB", registry.isMethodExists("webMethod", BridgePlatformType.WEB))
        assertFalse("Method should not be registered for LYNX", registry.isMethodExists("webMethod", BridgePlatformType.LYNX))
    }

    @Test
    fun testRegisterMethodWithOptimization() {
        // Given
        every { BridgeSettings.bridgeRegistryOptimize } returns true
        val testMethod = createMockIDLMethod("optimizedMethod")
        every { IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(null) } returns createMockRegistryCache("optimizedMethod")

        // When
        registry.registerMethod(testMethod, BridgePlatformType.ALL)

        // Then - Should register using optimized path
        assertTrue("Method should be registered with optimization", registry.isMethodExists("optimizedMethod", BridgePlatformType.ALL))
    }

    @Test
    fun testFindMethodClass() {
        // Given
        val testMethod = createMockIDLMethod("findableMethod")
        every { IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(null) } returns createMockRegistryCache("findableMethod")
        registry.registerMethod(testMethod, BridgePlatformType.WEB)

        // When
        val foundMethod = registry.findMethodClass(BridgePlatformType.WEB, "findableMethod")
        val notFoundMethod = registry.findMethodClass(BridgePlatformType.LYNX, "findableMethod")
        val noneMethod = registry.findMethodClass(BridgePlatformType.NONE, "findableMethod")

        // Then
        assertEquals("Should find registered method", testMethod, foundMethod)
        assertNull("Should not find method in different platform", notFoundMethod)
        assertNull("Should not find method for NONE platform", noneMethod)
    }

    @Test
    fun testIsMethodExists() {
        // Given
        val testMethod = createMockIDLMethod("existingMethod")
        every { IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(null) } returns createMockRegistryCache("existingMethod")
        registry.registerMethod(testMethod, BridgePlatformType.ALL)

        // When & Then
        assertTrue("Method should exist", registry.isMethodExists("existingMethod", BridgePlatformType.ALL))
        assertFalse("Non-existent method should not exist", registry.isMethodExists("nonExistentMethod", BridgePlatformType.ALL))
    }

    @Test
    fun testGetMethodList() {
        // Given
        val testMethod1 = createMockIDLMethod("method1")
        val testMethod2 = createMockIDLMethod("method2")
        every { IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(null) } returns createMockRegistryCache("method1")
        registry.registerMethod(testMethod1, BridgePlatformType.WEB)

        every { IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(null) } returns createMockRegistryCache("method2")
        registry.registerMethod(testMethod2, BridgePlatformType.WEB)

        // When
        val methodList = registry.getMethodList(BridgePlatformType.WEB)
        val emptyList = registry.getMethodList(BridgePlatformType.NONE)

        // Then
        assertNotNull("Method list should not be null", methodList)
        assertEquals("Should have 2 methods", 2, methodList?.size)
        assertTrue("Should contain method1", methodList?.containsKey("method1") == true)
        assertTrue("Should contain method2", methodList?.containsKey("method2") == true)
        assertNull("NONE platform should return null", emptyList)
    }

    @Test
    fun testRegisterMethodWithNullCache() {
        // Given
        val testMethod = createMockIDLMethod("nullCacheMethod")
        every { IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(null) } returns null

        // When
        registry.registerMethod(testMethod, BridgePlatformType.ALL)

        // Then - Should handle null cache gracefully
        assertFalse("Method should not be registered without cache", registry.isMethodExists("nullCacheMethod", BridgePlatformType.ALL))
    }

    @Test
    fun testNamespaceProperty() {
        // Given
        val customNamespace = "CUSTOM_TEST_NAMESPACE"

        // When
        registry.namespace = customNamespace

        // Then
        assertEquals("Namespace should be updated", customNamespace, registry.namespace)
    }

    @Test
    fun testLocalBridgeRegistryWithBridgeContext() {
        // Given
        val localRegistry = IDLMethodRegistry(isLocalBridgeRegistry = true, bridgeContext = mockBridgeContext)
        val testMethod = createMockIDLMethod("localMethod")

        // Mock the local registry cache
        val mockCache = mockk<IDLMethodRegistryCache>()
        every { mockCache.find(testMethod) } returns "localMethod"
        every { IDLMethodRegistryCacheManager.registerIDLMethodRegistryCache(any(), any()) } returns Unit

        // When
        localRegistry.registerMethod(testMethod, BridgePlatformType.ALL)

        // Then
        verify { IDLMethodRegistryCacheManager.registerIDLMethodRegistryCache("test_container", any()) }
    }

    @Test
    fun testRegisterMethodWithEmptyMethodName() {
        // Given
        val testMethod = createMockIDLMethod("")
        every { IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(null) } returns createMockRegistryCache("")

        // When
        registry.registerMethod(testMethod, BridgePlatformType.ALL)

        // Then - Should handle empty method name
        assertFalse("Empty method name should not be registered", registry.isMethodExists("", BridgePlatformType.ALL))
    }

    private fun createMockIDLMethod(methodName: String): Class<out IDLBridgeMethod> =
        when (methodName) {
            "testMethod" -> TestMethod1::class.java
            "webMethod" -> TestMethod2::class.java
            "optimizedMethod" -> TestMethod3::class.java
            "findableMethod" -> TestMethod4::class.java
            "existingMethod" -> TestMethod5::class.java
            "method1" -> TestMethod6::class.java
            "method2" -> TestMethod7::class.java
            "nullCacheMethod" -> TestMethod8::class.java
            "localMethod" -> TestMethod9::class.java
            "" -> TestMethodEmpty::class.java
            else -> TestMethod1::class.java // fallback
        }

    private fun createMockRegistryCache(methodName: String): IDLMethodRegistryCache {
        val mockCache = mockk<IDLMethodRegistryCache>()
        every { mockCache.find(any()) } returns methodName
        return mockCache
    }
}
