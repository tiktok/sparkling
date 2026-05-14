// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LocalBridgeTest {
    private lateinit var localBridge: LocalBridge
    private lateinit var mockRegistry: IDLMethodRegistry

    // Use a concrete test class instead of mocking Class<out IDLBridgeMethod>
    private class TestIDLBridgeMethod : IDLBridgeMethod {
        override val name: String = "TestMethod"

        override fun realHandle(
            params: Map<String, Any?>,
            callback: IDLBridgeMethod.Callback,
            type: BridgePlatformType,
        ) {
            // Test implementation
        }

        override fun setProviderFactory(contextProviderFactory: com.tiktok.sparkling.method.registry.core.model.context.ContextProviderFactory?) {
            // Test implementation
        }

        override fun setBridgeContext(bridgeContext: IBridgeContext) {
            // Test implementation
        }
    }

    @Before
    fun setUp() {
        localBridge = LocalBridge()
        mockRegistry = mockk(relaxed = true)

        every { mockRegistry.namespace } returns "TEST_NAMESPACE"
        every { mockRegistry.registerMethod(any(), any()) } returns Unit
        every { mockRegistry.findMethodClass(any(), any()) } returns null
        every { mockRegistry.getMethodList(any()) } returns null
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testRegisterRegistry() {
        // Given
        val testNamespace = "TEST_NAMESPACE"
        every { mockRegistry.namespace } returns testNamespace

        // When
        localBridge.registerRegistry(mockRegistry)

        // Then - Should complete without exception
        assertTrue("Registry should be registered successfully", true)
    }

    @Test
    fun testRegisterIDLMethodWithDefaultNamespace() {
        // Given
        val defaultNamespace = "DEFAULT"
        val mockRegistry = createMockRegistryForNamespace(defaultNamespace)
        localBridge.registerRegistry(mockRegistry)

        // When
        localBridge.registerIDLMethod(TestIDLBridgeMethod::class.java, BridgePlatformType.WEB, defaultNamespace)

        // Then
        verify { mockRegistry.registerMethod(TestIDLBridgeMethod::class.java, BridgePlatformType.WEB) }
    }

    @Test
    fun testRegisterIDLMethodWithCustomNamespace() {
        // Given
        val customNamespace = "CUSTOM_NAMESPACE"
        val mockRegistry = createMockRegistryForNamespace(customNamespace)
        localBridge.registerRegistry(mockRegistry)

        // When
        localBridge.registerIDLMethod(TestIDLBridgeMethod::class.java, BridgePlatformType.LYNX, customNamespace)

        // Then
        verify { mockRegistry.registerMethod(TestIDLBridgeMethod::class.java, BridgePlatformType.LYNX) }
    }

    @Test
    fun testRegisterIDLMethodWithNullClass() {
        // Given
        val defaultNamespace = "DEFAULT"
        val mockRegistry = createMockRegistryForNamespace(defaultNamespace)
        localBridge.registerRegistry(mockRegistry)

        // When
        localBridge.registerIDLMethod(null, BridgePlatformType.ALL, defaultNamespace)

        // Then - Should handle null gracefully without throwing exception
        assertTrue("Null class registration should be handled gracefully", true)
    }

    @Test
    fun testFindIDLMethodClassWithExistingRegistry() {
        // Given
        val namespace = "TEST_NAMESPACE"
        val methodName = "testMethod"
        val mockRegistry = createMockRegistryForNamespace(namespace)

        every { mockRegistry.findMethodClass(BridgePlatformType.WEB, methodName) } returns TestIDLBridgeMethod::class.java
        localBridge.registerRegistry(mockRegistry)

        // When
        val result = localBridge.findIDLMethodClass(BridgePlatformType.WEB, methodName, namespace)

        // Then
        assertEquals("Should find the method class", TestIDLBridgeMethod::class.java, result)
        verify { mockRegistry.findMethodClass(BridgePlatformType.WEB, methodName) }
    }

    @Test
    fun testFindIDLMethodClassWithNonExistentRegistry() {
        // Given
        val nonExistentNamespace = "NON_EXISTENT"
        val methodName = "testMethod"

        // When
        val result = localBridge.findIDLMethodClass(BridgePlatformType.WEB, methodName, nonExistentNamespace)

        // Then
        assertNull("Should return null for non-existent registry", result)
    }

    @Test
    fun testFindIDLMethodClassWithNonExistentMethod() {
        // Given
        val namespace = "TEST_NAMESPACE"
        val methodName = "nonExistentMethod"
        val mockRegistry = createMockRegistryForNamespace(namespace)

        every { mockRegistry.findMethodClass(BridgePlatformType.WEB, methodName) } returns null
        localBridge.registerRegistry(mockRegistry)

        // When
        val result = localBridge.findIDLMethodClass(BridgePlatformType.WEB, methodName, namespace)

        // Then
        assertNull("Should return null for non-existent method", result)
    }

    @Test
    fun testGetIDLMethodListWithExistingRegistry() {
        // Given
        val namespace = "TEST_NAMESPACE"
        val mockMethodList =
            mutableMapOf<String, Class<out IDLBridgeMethod>>(
                "method1" to TestIDLBridgeMethod::class.java,
                "method2" to TestIDLBridgeMethod::class.java,
            )
        val mockRegistry = createMockRegistryForNamespace(namespace)

        every { mockRegistry.getMethodList(BridgePlatformType.ALL) } returns mockMethodList
        localBridge.registerRegistry(mockRegistry)

        // When
        val result = localBridge.getIDLMethodList(BridgePlatformType.ALL, namespace)

        // Then
        assertEquals("Should return the method list", mockMethodList, result)
        verify { mockRegistry.getMethodList(BridgePlatformType.ALL) }
    }

    @Test
    fun testGetIDLMethodListWithNonExistentRegistry() {
        // Given
        val nonExistentNamespace = "NON_EXISTENT"

        // When
        val result = localBridge.getIDLMethodList(BridgePlatformType.ALL, nonExistentNamespace)

        // Then
        assertNull("Should return null for non-existent registry", result)
    }

    @Test
    fun testGetIDLMethodListWithEmptyRegistry() {
        // Given
        val namespace = "EMPTY_NAMESPACE"
        val mockRegistry = createMockRegistryForNamespace(namespace)

        every { mockRegistry.getMethodList(BridgePlatformType.WEB) } returns null
        localBridge.registerRegistry(mockRegistry)

        // When
        val result = localBridge.getIDLMethodList(BridgePlatformType.WEB, namespace)

        // Then
        assertNull("Should return null for empty registry", result)
    }

    @Test
    fun testMultipleRegistries() {
        // Given
        val namespace1 = "NAMESPACE_1"
        val namespace2 = "NAMESPACE_2"
        val mockRegistry1 = createMockRegistryForNamespace(namespace1)
        val mockRegistry2 = createMockRegistryForNamespace(namespace2)

        val methodName = "testMethod"
        every { mockRegistry1.findMethodClass(BridgePlatformType.WEB, methodName) } returns TestIDLBridgeMethod::class.java
        every { mockRegistry2.findMethodClass(BridgePlatformType.WEB, methodName) } returns null

        localBridge.registerRegistry(mockRegistry1)
        localBridge.registerRegistry(mockRegistry2)

        // When
        val result1 = localBridge.findIDLMethodClass(BridgePlatformType.WEB, methodName, namespace1)
        val result2 = localBridge.findIDLMethodClass(BridgePlatformType.WEB, methodName, namespace2)

        // Then
        assertEquals("Should find method in first registry", TestIDLBridgeMethod::class.java, result1)
        assertNull("Should not find method in second registry", result2)
    }

    @Test
    fun testRegistryOverride() {
        // Given
        val namespace = "TEST_NAMESPACE"
        val mockRegistry1 = createMockRegistryForNamespace(namespace)
        val mockRegistry2 = createMockRegistryForNamespace(namespace)

        val methodName = "testMethod"

        // Create a second test class for the override scenario
        class TestIDLBridgeMethod2 : IDLBridgeMethod {
            override val name: String = "TestMethod2"

            override fun realHandle(
                params: Map<String, Any?>,
                callback: IDLBridgeMethod.Callback,
                type: BridgePlatformType,
            ) {
                // Test implementation
            }

            override fun setProviderFactory(contextProviderFactory: com.tiktok.sparkling.method.registry.core.model.context.ContextProviderFactory?) {
                // Test implementation
            }

            override fun setBridgeContext(bridgeContext: IBridgeContext) {
                // Test implementation
            }
        }

        every { mockRegistry1.findMethodClass(BridgePlatformType.WEB, methodName) } returns TestIDLBridgeMethod::class.java
        every { mockRegistry2.findMethodClass(BridgePlatformType.WEB, methodName) } returns TestIDLBridgeMethod2::class.java

        localBridge.registerRegistry(mockRegistry1)
        localBridge.registerRegistry(mockRegistry2) // This should override the first one

        // When
        val result = localBridge.findIDLMethodClass(BridgePlatformType.WEB, methodName, namespace)

        // Then
        assertEquals("Should use the overridden registry", TestIDLBridgeMethod2::class.java, result)
    }

    @Test
    fun testAllPlatformTypes() {
        // Given
        val namespace = "TEST_NAMESPACE"
        val mockRegistry = createMockRegistryForNamespace(namespace)
        val methodName = "testMethod"

        every { mockRegistry.findMethodClass(any(), any()) } returns TestIDLBridgeMethod::class.java
        localBridge.registerRegistry(mockRegistry)

        // When & Then - Test all platform types
        val allResult = localBridge.findIDLMethodClass(BridgePlatformType.ALL, methodName, namespace)
        val webResult = localBridge.findIDLMethodClass(BridgePlatformType.WEB, methodName, namespace)
        val lynxResult = localBridge.findIDLMethodClass(BridgePlatformType.LYNX, methodName, namespace)
        val noneResult = localBridge.findIDLMethodClass(BridgePlatformType.NONE, methodName, namespace)

        assertEquals("ALL platform should work", TestIDLBridgeMethod::class.java, allResult)
        assertEquals("WEB platform should work", TestIDLBridgeMethod::class.java, webResult)
        assertEquals("LYNX platform should work", TestIDLBridgeMethod::class.java, lynxResult)
        assertEquals("NONE platform should work", TestIDLBridgeMethod::class.java, noneResult)
    }

    private fun createMockRegistryForNamespace(namespace: String): IDLMethodRegistry {
        val registry = mockk<IDLMethodRegistry>(relaxed = true)
        every { registry.namespace } returns namespace
        return registry
    }
}
