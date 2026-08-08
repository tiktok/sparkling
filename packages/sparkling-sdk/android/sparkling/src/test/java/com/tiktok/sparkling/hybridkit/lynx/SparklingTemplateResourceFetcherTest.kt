// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.lynx

import com.lynx.tasm.resourceprovider.LynxResourceCallback
import com.lynx.tasm.resourceprovider.LynxResourceRequest
import com.lynx.tasm.resourceprovider.LynxResourceResponse
import com.lynx.tasm.resourceprovider.template.LynxTemplateResourceFetcher
import com.lynx.tasm.resourceprovider.template.TemplateProviderResult
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
class SparklingTemplateResourceFetcherTest {
    private lateinit var delegate: RecordingTemplateResourceFetcher
    private lateinit var kitView: SimpleLynxKitView
    private lateinit var lifeCycle: IHybridKitLifeCycle
    private lateinit var fetcher: SparklingTemplateResourceFetcher

    @Before
    fun setUp() {
        clearAllMocks()
        delegate = RecordingTemplateResourceFetcher()
        kitView = mockk(relaxed = true)
        lifeCycle = mockk(relaxed = true)
        every { kitView.hasDestroyed() } returns false
        fetcher = SparklingTemplateResourceFetcher(delegate, lifeCycle) { kitView }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun templateSuccessForwardsExactlyOnce() {
        val request = templateRequest()
        val callback = RecordingCallback<TemplateProviderResult>()
        val binary = byteArrayOf(1, 2, 3)
        val response = LynxResourceResponse.onSuccess(TemplateProviderResult.fromBinary(binary))

        fetcher.fetchTemplate(request, callback)
        delegate.templateCallback?.onResponse(response)
        delegate.templateCallback?.onResponse(response)

        verify(exactly = 1) { lifeCycle.onResourceLoadStart(kitView, request.url) }
        verify(exactly = 1) {
            lifeCycle.onResourceLoadFinish(kitView, request.url, binary, null)
        }
        assertEquals(1, callback.responses.size)
        assertArrayEquals(
            binary,
            callback.responses
                .single()
                .data.templateBinary,
        )
    }

    @Test
    fun templateFailureForwardsExactlyOnce() {
        val request = templateRequest()
        val callback = RecordingCallback<TemplateProviderResult>()
        val errorSlot = slot<HybridKitError>()
        val response =
            failedResponse<TemplateProviderResult>(IllegalStateException("template failed"))

        fetcher.fetchTemplate(request, callback)
        delegate.templateCallback?.onResponse(response)
        delegate.templateCallback?.onResponse(response)

        verify(exactly = 1) {
            lifeCycle.onResourceLoadFinish(
                kitView,
                request.url,
                null,
                capture(errorSlot),
            )
        }
        assertEquals("ResourceLoadFailed", errorSlot.captured.errorReason)
        assertEquals("template failed", errorSlot.captured.originReason)
        assertEquals(1, callback.responses.size)
    }

    @Test
    fun ssrSuccessAndFailureEachCompleteExactlyOnce() {
        val successRequest = templateRequest("https://example.com/page.ssr")
        val successCallback = RecordingCallback<ByteArray>()
        val successData = byteArrayOf(4, 5, 6)

        fetcher.fetchSSRData(successRequest, successCallback)
        delegate.ssrCallback?.onResponse(LynxResourceResponse.onSuccess(successData))
        delegate.ssrCallback?.onResponse(LynxResourceResponse.onSuccess(successData))

        verify(exactly = 1) {
            lifeCycle.onResourceLoadFinish(kitView, successRequest.url, successData, null)
        }
        assertEquals(1, successCallback.responses.size)

        val failureRequest = templateRequest("https://example.com/failure.ssr")
        val failureCallback = RecordingCallback<ByteArray>()
        val failureResponse = failedResponse<ByteArray>(IllegalStateException("ssr failed"))

        fetcher.fetchSSRData(failureRequest, failureCallback)
        delegate.ssrCallback?.onResponse(failureResponse)
        delegate.ssrCallback?.onResponse(failureResponse)

        verify(exactly = 1) {
            lifeCycle.onResourceLoadFinish(
                kitView,
                failureRequest.url,
                null,
                any<HybridKitError>(),
            )
        }
        assertEquals(1, failureCallback.responses.size)
    }

    @Test
    fun synchronousDelegateExceptionBecomesOneFailure() {
        val request = templateRequest()
        val callback = RecordingCallback<TemplateProviderResult>()
        delegate.templateFailure = IllegalStateException("thrown")

        fetcher.fetchTemplate(request, callback)

        verify(exactly = 1) {
            lifeCycle.onResourceLoadFinish(
                kitView,
                request.url,
                null,
                any<HybridKitError>(),
            )
        }
        assertEquals(1, callback.responses.size)
        assertEquals(
            LynxResourceResponse.ResponseState.FAILED,
            callback.responses.single().state,
        )
        assertEquals(
            "thrown",
            callback.responses
                .single()
                .error.message,
        )
    }

    private fun templateRequest(
        url: String = "https://example.com/main.lynx.bundle",
    ): LynxResourceRequest =
        LynxResourceRequest(
            url,
            LynxResourceRequest.LynxResourceType.LynxResourceTypeTemplate,
        )

    @Suppress("UNCHECKED_CAST")
    private fun <T> failedResponse(error: Throwable): LynxResourceResponse<T> = LynxResourceResponse.onFailed(error) as LynxResourceResponse<T>

    private class RecordingTemplateResourceFetcher : LynxTemplateResourceFetcher() {
        var templateCallback: LynxResourceCallback<TemplateProviderResult>? = null
        var ssrCallback: LynxResourceCallback<ByteArray>? = null
        var templateFailure: Throwable? = null

        override fun fetchTemplate(
            request: LynxResourceRequest,
            callback: LynxResourceCallback<TemplateProviderResult>,
        ) {
            templateFailure?.let { throw it }
            templateCallback = callback
        }

        override fun fetchSSRData(
            request: LynxResourceRequest,
            callback: LynxResourceCallback<ByteArray>,
        ) {
            ssrCallback = callback
        }
    }

    private class RecordingCallback<T> : LynxResourceCallback<T> {
        val responses = mutableListOf<LynxResourceResponse<T>>()

        override fun onResponse(response: LynxResourceResponse<T>) {
            responses += response
        }
    }
}
