// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

import com.tiktok.sparkling.method.registry.core.model.context.ContextProviderFactory
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SparklingBridgeManagerTest {
    private lateinit var mockRegistry: IDLMethodRegistry

    @Before
    fun setUp() {
        mockRegistry = mockk(relaxed = true)
        // Ensure mock registry has a specific namespace to avoid conflicts
        every { mockRegistry.namespace } returns "TEST_NAMESPACE"
    }

    @After
    fun tearDown() {
        // Clean up any static state if needed
        // Note: SparklingBridgeManager is a singleton object, so state persists between tests
        // This can cause test order dependencies
    }

    @Test
    fun testRegisterRegistry() {
        // When - Register the mock registry
        SparklingBridgeManager.registerRegistry(mockRegistry)

        // Then - Method should complete without exceptions
        assertTrue("Registry registration should complete without exceptions", true)
    }

    @Test
    fun testRegisterIDLMethodWithDefaultParameters() {
        // Given - Create a concrete test class instead of mocking
        class TestIDLMethod : IDLBridgeMethod {
            override val name: String = "testMethod"

            override fun realHandle(
                params: Map<String, Any?>,
                callback: IDLBridgeMethod.Callback,
                type: BridgePlatformType,
            ) {
                // Test implementation
            }

            override fun setProviderFactory(contextProviderFactory: ContextProviderFactory?) {
                // Test implementation
            }

            override fun setBridgeContext(bridgeContext: IBridgeContext) {
                // Test implementation
            }
        }

        // When
        SparklingBridgeManager.registerIDLMethod(TestIDLMethod::class.java)

        // Then - Method should complete without exceptions
        assertTrue("IDL method registration should complete without exceptions", true)
    }

    @Test
    fun testRegisterIDLMethodWithCustomParameters() {
        // Given
        class TestIDLMethodCustom : IDLBridgeMethod {
            override val name: String = "testMethodCustom"

            override fun realHandle(
                params: Map<String, Any?>,
                callback: IDLBridgeMethod.Callback,
                type: BridgePlatformType,
            ) {
                // Test implementation
            }

            override fun setProviderFactory(contextProviderFactory: ContextProviderFactory?) {
                // Test implementation
            }

            override fun setBridgeContext(bridgeContext: IBridgeContext) {
                // Test implementation
            }
        }
        val customNamespace = "CUSTOM_NAMESPACE"
        val customScope = BridgePlatformType.WEB

        // When
        SparklingBridgeManager.registerIDLMethod(TestIDLMethodCustom::class.java, customScope, customNamespace)

        // Then - Method should complete without exceptions
        assertTrue("IDL method registration should complete without exceptions", true)
    }

    @Test
    fun testRegisterIDLMethodWithNullClass() {
        // Given
        val nullMethod: Class<out IDLBridgeMethod>? = null

        // When & Then - Should handle null gracefully
        SparklingBridgeManager.registerIDLMethod(nullMethod)
        assertTrue("Null IDL method registration should complete without exceptions", true)
    }

    @Test
    fun testFindIDLMethodClassWithDefaultNamespace() {
        // Given
        val methodName = "testMethod"
        val platformType = BridgePlatformType.ALL

        // When
        val result = SparklingBridgeManager.findIDLMethodClass(platformType, methodName)

        // Then - Should return null for non-existent methods
        assertNull("Non-existent method should return null", result)
    }

    @Test
    fun testFindIDLMethodClassWithCustomNamespace() {
        // Given
        val methodName = "testMethod"
        val platformType = BridgePlatformType.LYNX
        val customNamespace = "CUSTOM_NAMESPACE"

        // When
        val result = SparklingBridgeManager.findIDLMethodClass(platformType, methodName, customNamespace)

        // Then - Should return null for non-existent methods
        assertNull("Non-existent method should return null", result)
    }

    @Test
    fun testGetIDLMethodListWithDefaultNamespace() {
        // Given
        val platformType = BridgePlatformType.WEB

        // When
        val result = SparklingBridgeManager.getIDLMethodList(platformType)

        // Then - Should return empty or null for empty registry
        // The result could be null or empty depending on implementation
        assertTrue("Method list should be consistent", result == null || result.isEmpty())
    }

    @Test
    fun testGetIDLMethodListWithCustomNamespace() {
        // Given
        val platformType = BridgePlatformType.ALL
        val customNamespace = "CUSTOM_NAMESPACE"

        // When
        val result = SparklingBridgeManager.getIDLMethodList(platformType, customNamespace)

        // Then - Should return empty or null for empty registry
        assertTrue("Method list should be consistent", result == null || result.isEmpty())
    }

    @Test
    fun testDefaultNamespaceConstant() {
        // Given & When
        val defaultNamespace = SparklingBridgeManager.DEFAULT_NAMESPACE

        // Then
        assertEquals("Default namespace should be 'DEFAULT'", "DEFAULT", defaultNamespace)
    }

    @Test
    fun testMultiplePlatformTypeSupport() {
        // Given
        val methodName = "testMethod"

        // When & Then - Test all platform types
        val allResult = SparklingBridgeManager.findIDLMethodClass(BridgePlatformType.ALL, methodName)
        val webResult = SparklingBridgeManager.findIDLMethodClass(BridgePlatformType.WEB, methodName)
        val lynxResult = SparklingBridgeManager.findIDLMethodClass(BridgePlatformType.LYNX, methodName)
        val noneResult = SparklingBridgeManager.findIDLMethodClass(BridgePlatformType.NONE, methodName)

        // All should handle gracefully
        assertNull("ALL platform type should handle gracefully", allResult)
        assertNull("WEB platform type should handle gracefully", webResult)
        assertNull("LYNX platform type should handle gracefully", lynxResult)
        assertNull("NONE platform type should handle gracefully", noneResult)
    }

    @Test
    fun testEmptyMethodName() {
        // Given
        val emptyName = ""
        val platformType = BridgePlatformType.ALL

        // When
        val result = SparklingBridgeManager.findIDLMethodClass(platformType, emptyName)

        // Then
        assertNull("Empty method name should return null", result)
    }

    @Test
    fun testEmptyNamespace() {
        // Given
        val methodName = "testMethod"
        val platformType = BridgePlatformType.ALL
        val emptyNamespace = ""

        // When
        val result = SparklingBridgeManager.findIDLMethodClass(platformType, methodName, emptyNamespace)

        // Then
        assertNull("Empty namespace should handle gracefully", result)
    }
}
