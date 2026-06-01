// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.runtime.depend.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class HttpUrlBuilderTest {
    @Test
    fun buildWithoutParamsReturnsBaseUrl() {
        val builder = HttpUrlBuilder("https://example.com/path")
        assertEquals("https://example.com/path", builder.build())
        assertEquals("https://example.com/path", builder.toString())
    }

    @Test
    fun addIntLongDoubleAndStringParamsAppendsCorrectly() {
        val url =
            HttpUrlBuilder("https://api.example.com/v1/foo")
                .addParam("intKey", 42)
                .addParam("longKey", 100L)
                .addParam("doubleKey", 1.5)
                .addParam("stringKey", "hello")
                .build()

        // The base URL should be preserved as the prefix.
        assertTrue(url.startsWith("https://api.example.com/v1/foo?"))

        // All keys should be present somewhere in the resulting query.
        assertTrue(url.contains("intKey=42"))
        assertTrue(url.contains("longKey=100"))
        assertTrue(url.contains("doubleKey=1.5"))
        assertTrue(url.contains("stringKey=hello"))
    }

    @Test
    fun buildWithExistingQueryStringJoinsUsingAmpersand() {
        val url =
            HttpUrlBuilder("https://example.com/path?existing=1")
                .addParam("k", "v")
                .build()
        assertEquals("https://example.com/path?existing=1&k=v", url)
    }

    @Test
    fun stringValueIsUrlEncodedByDefault() {
        val url =
            HttpUrlBuilder("https://example.com")
                .addParam("q", "a b/c")
                .build()
        // Default encoding is UTF-8 -> spaces become '+', '/' becomes %2F.
        assertTrue(url.contains("q=a+b%2Fc"))
    }

    @Test
    fun nullEncodingMagicValueLeavesValuesUntouched() {
        val builder =
            HttpUrlBuilder("https://example.com")
                .addParam("q", "a b/c")
        builder.encoding = "null_encoding"
        val url = builder.build()
        assertTrue(url.contains("q=a b/c"))
    }

    @Test
    fun unsupportedEncodingThrowsIllegalArgumentException() {
        val builder =
            HttpUrlBuilder("https://example.com")
                .addParam("q", "value")
        builder.encoding = "no-such-encoding"
        try {
            builder.build()
            throw AssertionError("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun emptyBaseUrlWithParamsReturnsJustQueryString() {
        val url =
            HttpUrlBuilder("")
                .addParam("k", "v")
                .build()
        assertEquals("k=v", url)
    }
}
