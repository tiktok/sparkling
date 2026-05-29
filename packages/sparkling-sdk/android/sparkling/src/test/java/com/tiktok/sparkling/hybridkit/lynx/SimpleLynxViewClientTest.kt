// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.lynx

import com.lynx.tasm.LynxError
import com.tiktok.sparkling.hybridkit.base.HybridErrorConstantCode
import com.tiktok.sparkling.hybridkit.base.HybridKitError
import com.tiktok.sparkling.hybridkit.base.HybridKitType
import com.tiktok.sparkling.hybridkit.base.IHybridKitLifeCycle
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    packageName = "com.tiktok.sparkling",
)
class SimpleLynxViewClientTest {
    private lateinit var kitView: SimpleLynxKitView
    private lateinit var lifeCycle: IHybridKitLifeCycle
    private lateinit var client: SimpleLynxViewClient

    @Before
    fun setUp() {
        clearAllMocks()
        kitView = mockk(relaxed = true)
        lifeCycle = mockk(relaxed = true)
        every { kitView.hasDestroyed() } returns false
        client = SimpleLynxViewClient(kitView, lifeCycle)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun onLoadSuccessForwardsToKitViewOnceForCurrentLoad() {
        client.onPageStart("https://example.com/main.lynx.bundle")

        client.onLoadSuccess()
        client.onLoadSuccess()

        verify(exactly = 1) { kitView.onLoadSuccess() }
    }

    @Test
    fun onLoadSuccessCanFinishAgainAfterNextPageStart() {
        client.onPageStart("https://example.com/first.lynx.bundle")
        client.onLoadSuccess()

        client.onPageStart("https://example.com/second.lynx.bundle")
        client.onLoadSuccess()

        verify(exactly = 2) { kitView.onLoadSuccess() }
    }

    @Test
    fun onReceivedErrorKeepsFatalErrorOnFailedPath() {
        val errorSlot = slot<HybridKitError>()

        client.onPageStart("https://example.com/main.lynx.bundle")
        client.onReceivedError(fatalLynxError())

        verify(exactly = 1) {
            lifeCycle.onLoadFailed(
                kitView,
                "https://example.com/main.lynx.bundle",
                capture(errorSlot),
            )
        }
        assertEquals(HybridErrorConstantCode.LynxLoadError, errorSlot.captured.errorCode)
        assertEquals("LynxReceiveError", errorSlot.captured.errorReason)
        assertEquals(501, errorSlot.captured.originCode)
        assertTrue(errorSlot.captured.originReason?.contains("fatal error") == true)
    }

    @Test
    fun onReceivedErrorForwardsEveryLynxError() {
        val errorSlot = slot<HybridKitError>()

        client.onPageStart("https://example.com/main.lynx.bundle")
        client.onReceivedError(fatalLynxError())

        verify(exactly = 1) {
            lifeCycle.onReceivedError(
                kitView,
                capture(errorSlot),
            )
        }
        assertEquals(HybridErrorConstantCode.LynxLoadError, errorSlot.captured.errorCode)
        assertEquals("LynxReceiveError", errorSlot.captured.errorReason)
        assertEquals(501, errorSlot.captured.originCode)
        assertTrue(errorSlot.captured.originReason?.contains("fatal error") == true)
    }

    @Test
    fun onRuntimeReadyForwardsLifecycle() {
        client.onRuntimeReady()

        verify(exactly = 1) { lifeCycle.onRuntimeReady(HybridKitType.LYNX) }
    }

    @Test
    fun onRuntimeReadyAfterRuntimeDestroyDoesNotForwardLifecycle() {
        client.onLynxViewAndJSRuntimeDestroy()

        client.onRuntimeReady()

        verify(exactly = 0) { lifeCycle.onRuntimeReady(any()) }
    }

    @Test
    fun onFirstScreenForwardsLifecycle() {
        client.onFirstScreen()

        verify(exactly = 1) { lifeCycle.onFirstScreen(kitView) }
    }

    @Test
    fun onPageUpdateForwardsLifecycle() {
        client.onPageUpdate()

        verify(exactly = 1) { lifeCycle.onPageUpdate(kitView) }
    }

    @Test
    fun onDataUpdatedForwardsLifecycleUpdate() {
        client.onDataUpdated()

        verify(exactly = 1) { lifeCycle.onUpdate(kitView) }
    }

    @Test
    fun onTimingSetupForwardsPerformanceLifecycle() {
        val timingInfo =
            mutableMapOf<String, Any>(
                "setup_timing" to mapOf("draw_end" to 123L),
            )

        client.onTimingSetup(timingInfo)

        verify(exactly = 1) { lifeCycle.onReceivedPerformance(kitView, timingInfo) }
    }

    @Test
    fun onLoadSuccessAfterFatalErrorDoesNotHideErrorPath() {
        client.onPageStart("https://example.com/main.lynx.bundle")
        client.onReceivedError(fatalLynxError())

        client.onLoadSuccess()

        verify(exactly = 0) { kitView.onLoadSuccess() }
    }

    @Test
    fun onLoadSuccessAfterRuntimeDestroyDoesNotAccessKitView() {
        client.onPageStart("https://example.com/main.lynx.bundle")
        client.onLynxViewAndJSRuntimeDestroy()

        client.onLoadSuccess()

        verify(exactly = 1) { kitView.destroyWhenJSRuntimeCallback() }
        verify(exactly = 0) { kitView.onLoadSuccess() }
    }

    @Test
    fun onRuntimeDestroyIsIdempotent() {
        client.onLynxViewAndJSRuntimeDestroy()
        client.onLynxViewAndJSRuntimeDestroy()

        verify(exactly = 1) { kitView.destroyWhenJSRuntimeCallback() }
    }

    @Test
    fun onLoadSuccessDoesNotAccessAlreadyDestroyedKitView() {
        every { kitView.hasDestroyed() } returns true
        client.onPageStart("https://example.com/main.lynx.bundle")

        client.onLoadSuccess()

        verify(exactly = 0) { kitView.onLoadSuccess() }
    }

    private fun fatalLynxError(): LynxError = LynxError(50101, "fatal error")
}
