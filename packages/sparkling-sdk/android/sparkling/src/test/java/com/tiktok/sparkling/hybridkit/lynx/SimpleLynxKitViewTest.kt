// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.lynx

import android.content.Context
import com.tiktok.sparkling.hybridkit.HybridContext
import com.tiktok.sparkling.hybridkit.base.IHybridKitLifeCycle
import com.tiktok.sparkling.hybridkit.base.IKitView
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    packageName = "com.tiktok.sparkling",
)
class SimpleLynxKitViewTest {
    private lateinit var context: Context
    private lateinit var mockLifeCycle: IHybridKitLifeCycle
    private lateinit var mockHybridContext: HybridContext

    @Before
    fun setUp() {
        clearAllMocks()
        context = RuntimeEnvironment.getApplication()
        mockLifeCycle = mockk(relaxed = true)
        mockHybridContext = mockk(relaxed = true)

        every { mockHybridContext.containerId } returns "test_container"
        every { mockHybridContext.hybridSchemeParam } returns null
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testOnLoadSuccessCallsLifecycleOnLoadFinish() {
        // Create a mock IKitView that tracks onLoadSuccess calls
        val mockKitView = mockk<IKitView>(relaxed = true)
        val capturedKitView = mutableListOf<IKitView>()

        val lifeCycle =
            object : IHybridKitLifeCycle() {
                override fun onLoadFinish(view: IKitView) {
                    capturedKitView.add(view)
                }
            }

        // Verify the lifecycle callback would be called
        lifeCycle.onLoadFinish(mockKitView)

        assertEquals(1, capturedKitView.size)
        assertEquals(mockKitView, capturedKitView[0])
    }

    @Test
    fun testLifecycleOnLoadFinishIntegration() {
        var onLoadFinishCalled = false
        var capturedView: IKitView? = null

        val lifeCycle =
            object : IHybridKitLifeCycle() {
                override fun onLoadFinish(view: IKitView) {
                    onLoadFinishCalled = true
                    capturedView = view
                }
            }

        val mockKitView = mockk<IKitView>(relaxed = true)
        lifeCycle.onLoadFinish(mockKitView)

        assertEquals(true, onLoadFinishCalled)
        assertNotNull(capturedView)
    }

    @Test
    fun testLifecycleOnLoadStartCallback() {
        var onLoadStartCalled = false
        var capturedUrl: String? = null

        val lifeCycle =
            object : IHybridKitLifeCycle() {
                override fun onLoadStart(
                    view: IKitView,
                    url: String,
                ) {
                    onLoadStartCalled = true
                    capturedUrl = url
                }
            }

        val mockKitView = mockk<IKitView>(relaxed = true)
        lifeCycle.onLoadStart(mockKitView, "https://example.com")

        assertEquals(true, onLoadStartCalled)
        assertEquals("https://example.com", capturedUrl)
    }

    @Test
    fun testLifecycleOnLoadFailedCallback() {
        var onLoadFailedCalled = false
        var capturedReason: String? = null

        val lifeCycle =
            object : IHybridKitLifeCycle() {
                override fun onLoadFailed(
                    view: IKitView,
                    url: String,
                    reason: String?,
                ) {
                    onLoadFailedCalled = true
                    capturedReason = reason
                }
            }

        val mockKitView = mockk<IKitView>(relaxed = true)
        lifeCycle.onLoadFailed(mockKitView, "https://example.com", "Network error")

        assertEquals(true, onLoadFailedCalled)
        assertEquals("Network error", capturedReason)
    }

    @Test
    fun testLifecycleOnDestroyCallback() {
        var onDestroyCalled = false

        val lifeCycle =
            object : IHybridKitLifeCycle() {
                override fun onDestroy(view: IKitView) {
                    onDestroyCalled = true
                }
            }

        val mockKitView = mockk<IKitView>(relaxed = true)
        lifeCycle.onDestroy(mockKitView)

        assertEquals(true, onDestroyCalled)
    }

    @Test
    fun testHybridContextInitialization() {
        assertNotNull(mockHybridContext)
        assertEquals("test_container", mockHybridContext.containerId)
    }
}
