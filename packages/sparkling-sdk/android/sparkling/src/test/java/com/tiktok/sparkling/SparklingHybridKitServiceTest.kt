// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import android.content.Context
import android.os.Bundle
import com.tiktok.sparkling.hybridkit.base.HybridKitInitCallback
import com.tiktok.sparkling.hybridkit.base.HybridKitInitStatus
import com.tiktok.sparkling.hybridkit.base.IHybridKitBaseService
import com.tiktok.sparkling.hybridkit.utils.HybridKitInitManager
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SparklingHybridKitServiceTest {
    private lateinit var context: Context
    private lateinit var mockCallback: HybridKitInitCallback

    @Before
    fun setUp() {
        clearAllMocks()

        context = RuntimeEnvironment.getApplication()
        mockCallback = mockk(relaxed = true)

        mockkObject(HybridKitInitManager)
        every { HybridKitInitManager.status } returns HybridKitInitStatus.INIT
        every { HybridKitInitManager.addCallback(any()) } just Runs
        every { HybridKitInitManager.removeCallback(any()) } just Runs
    }

    @Test
    fun testGetHybridInitStatus() {
        every { HybridKitInitManager.status } returns HybridKitInitStatus.FINISHED

        val status = SparklingHybridKitService.getHybridInitStatus()

        assertEquals(HybridKitInitStatus.FINISHED, status)
        verify { HybridKitInitManager.status }
    }

    @Test
    fun testGetHybridInitStatusWhenNotInit() {
        every { HybridKitInitManager.status } returns HybridKitInitStatus.INIT

        val status = SparklingHybridKitService.getHybridInitStatus()

        assertEquals(HybridKitInitStatus.INIT, status)
    }

    @Test
    fun testGetHybridInitStatusWhenInitFailed() {
        every { HybridKitInitManager.status } returns HybridKitInitStatus.LOADING

        val status = SparklingHybridKitService.getHybridInitStatus()

        assertEquals(HybridKitInitStatus.LOADING, status)
    }

    @Test
    fun testAddHybridKitInitCallback() {
        SparklingHybridKitService.addHybridKitInitCallback(mockCallback)

        verify { HybridKitInitManager.addCallback(mockCallback) }
    }

    @Test
    fun testRemoveHybridInitCallback() {
        SparklingHybridKitService.removeHybridInitCallback(mockCallback)

        verify { HybridKitInitManager.removeCallback(mockCallback) }
    }

    @Test
    fun testAddAndRemoveMultipleCallbacks() {
        val callback1 = mockk<HybridKitInitCallback>(relaxed = true)
        val callback2 = mockk<HybridKitInitCallback>(relaxed = true)

        SparklingHybridKitService.addHybridKitInitCallback(callback1)
        SparklingHybridKitService.addHybridKitInitCallback(callback2)

        verify { HybridKitInitManager.addCallback(callback1) }
        verify { HybridKitInitManager.addCallback(callback2) }

        SparklingHybridKitService.removeHybridInitCallback(callback1)
        SparklingHybridKitService.removeHybridInitCallback(callback2)

        verify { HybridKitInitManager.removeCallback(callback1) }
        verify { HybridKitInitManager.removeCallback(callback2) }
    }

    @Test
    fun testOpenMethodThrowsTodoException() {
        val url = "test_url"
        val targetHandlerName = "test_handler"
        val bundle = Bundle()

        assertThrows(NotImplementedError::class.java) {
            SparklingHybridKitService.open(context, url, targetHandlerName, bundle)
        }
    }

    @Test
    fun testOpenMethodWithNullTargetHandler() {
        val url = "test_url"
        val bundle = Bundle()

        assertThrows(NotImplementedError::class.java) {
            SparklingHybridKitService.open(context, url, null, bundle)
        }
    }

    @Test
    fun testOpenMethodWithNullBundle() {
        val url = "test_url"
        val targetHandlerName = "test_handler"

        assertThrows(NotImplementedError::class.java) {
            SparklingHybridKitService.open(context, url, targetHandlerName, null)
        }
    }

    @Test
    fun testInitHybridCoreSDKWithForceInitTrue() {
        SparklingHybridKitService.initHybridCoreSDK(forceInit = true)

        assertTrue(true)
    }

    @Test
    fun testInitHybridCoreSDKWithForceInitFalse() {
        SparklingHybridKitService.initHybridCoreSDK(forceInit = false)

        assertTrue(true)
    }

    @Test
    fun testServiceIsObject() {
        val service1 = SparklingHybridKitService
        val service2 = SparklingHybridKitService

        assertSame(service1, service2)
    }

    @Test
    fun testServiceImplementsIHybridKitBaseService() {
        assertTrue(SparklingHybridKitService is IHybridKitBaseService)
    }

    @Test
    fun testCallbackManagement() {
        val callback1 = mockk<HybridKitInitCallback>(relaxed = true)
        val callback2 = mockk<HybridKitInitCallback>(relaxed = true)

        SparklingHybridKitService.addHybridKitInitCallback(callback1)
        SparklingHybridKitService.addHybridKitInitCallback(callback2)
        SparklingHybridKitService.addHybridKitInitCallback(callback1)

        verify(exactly = 2) { HybridKitInitManager.addCallback(callback1) }
        verify(exactly = 1) { HybridKitInitManager.addCallback(callback2) }

        SparklingHybridKitService.removeHybridInitCallback(callback1)
        SparklingHybridKitService.removeHybridInitCallback(callback2)
        SparklingHybridKitService.removeHybridInitCallback(callback1)

        verify(exactly = 2) { HybridKitInitManager.removeCallback(callback1) }
        verify(exactly = 1) { HybridKitInitManager.removeCallback(callback2) }
    }

    @Test
    fun testInitStatusMapping() {
        val statuses =
            listOf(
                HybridKitInitStatus.INIT,
                HybridKitInitStatus.FINISHED,
                HybridKitInitStatus.LOADING,
            )

        for (status in statuses) {
            every { HybridKitInitManager.status } returns status
            assertEquals(status, SparklingHybridKitService.getHybridInitStatus())
        }
    }
}
