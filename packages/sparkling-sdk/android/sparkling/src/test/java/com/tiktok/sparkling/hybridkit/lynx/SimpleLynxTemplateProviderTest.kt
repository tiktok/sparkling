// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.lynx

import com.lynx.tasm.provider.AbsTemplateProvider
import com.tiktok.sparkling.hybridkit.base.HybridKitError
import com.tiktok.sparkling.hybridkit.base.IHybridKitLifeCycle
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertArrayEquals
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
class SimpleLynxTemplateProviderTest {
    private lateinit var delegate: RecordingTemplateProvider
    private lateinit var kitView: SimpleLynxKitView
    private lateinit var lifeCycle: IHybridKitLifeCycle
    private lateinit var provider: SimpleLynxTemplateProvider

    @Before
    fun setUp() {
        clearAllMocks()
        delegate = RecordingTemplateProvider()
        kitView = mockk(relaxed = true)
        lifeCycle = mockk(relaxed = true)
        every { kitView.hasDestroyed() } returns false
        provider = SimpleLynxTemplateProvider(delegate, lifeCycle) { kitView }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun loadTemplateForwardsResourceStartAndSuccess() {
        val callback = RecordingTemplateCallback()
        val template = byteArrayOf(1, 2, 3)

        provider.loadTemplate("https://example.com/main.lynx.bundle", callback)
        delegate.callback?.onSuccess(template)

        verify(exactly = 1) {
            lifeCycle.onResourceLoadStart(kitView, "https://example.com/main.lynx.bundle")
        }
        verify(exactly = 1) {
            lifeCycle.onResourceLoadFinish(
                kitView,
                "https://example.com/main.lynx.bundle",
                template,
                null,
            )
        }
        assertArrayEquals(template, callback.template)
    }

    @Test
    fun loadTemplateForwardsResourceFailure() {
        val callback = RecordingTemplateCallback()
        val errorSlot = slot<HybridKitError>()

        provider.loadTemplate("https://example.com/main.lynx.bundle", callback)
        delegate.callback?.onFailed("network failed")

        verify(exactly = 1) {
            lifeCycle.onResourceLoadStart(kitView, "https://example.com/main.lynx.bundle")
        }
        verify(exactly = 1) {
            lifeCycle.onResourceLoadFinish(
                kitView,
                "https://example.com/main.lynx.bundle",
                null,
                capture(errorSlot),
            )
        }
        assertEquals("ResourceLoadFailed", errorSlot.captured.errorReason)
        assertEquals("network failed", errorSlot.captured.originReason)
        assertEquals("network failed", callback.error)
    }

    @Test
    fun loadTemplateSkipsLifecycleAfterDestroy() {
        every { kitView.hasDestroyed() } returns true
        val callback = RecordingTemplateCallback()

        provider.loadTemplate("https://example.com/main.lynx.bundle", callback)
        delegate.callback?.onSuccess(byteArrayOf(1))

        verify(exactly = 0) { lifeCycle.onResourceLoadStart(any(), any()) }
        verify(exactly = 0) { lifeCycle.onResourceLoadFinish(any(), any(), any(), any()) }
        assertTrue(callback.template?.contentEquals(byteArrayOf(1)) == true)
    }

    private class RecordingTemplateProvider : AbsTemplateProvider() {
        var callback: Callback? = null

        override fun loadTemplate(
            url: String,
            callback: Callback,
        ) {
            this.callback = callback
        }
    }

    private class RecordingTemplateCallback : AbsTemplateProvider.Callback {
        var template: ByteArray? = null
        var error: String? = null

        override fun onSuccess(template: ByteArray?) {
            this.template = template
        }

        override fun onFailed(msg: String?) {
            error = msg
        }
    }
}
