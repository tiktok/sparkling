// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.utils

import android.net.Uri
import com.tiktok.sparkling.hybridkit.utils.safeGetQueryParameter
import com.tiktok.sparkling.hybridkit.utils.safeToUri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UriExtTest {
    @Test
    fun testSafeToUriWithValidUrl() {
        val validUrl = "https://example.com/path?param=value"
        val result = validUrl.safeToUri()

        assertNotNull(result)
        assertEquals("https", result.scheme)
        assertEquals("example.com", result.host)
        assertEquals("/path", result.path)
    }

    @Test
    fun testSafeToUriWithInvalidUrl() {
        val invalidUrl = "not-a-valid-url"
        val result = invalidUrl.safeToUri()

        assertNotNull(result)
        assertEquals("not-a-valid-url", result.toString())
    }

    @Test
    fun testSafeToUriWithNullString() {
        val nullString: String? = null
        val result = nullString.safeToUri()

        assertEquals(Uri.EMPTY, result)
    }

    @Test
    fun testSafeToUriWithEmptyString() {
        val emptyString = ""
        val result = emptyString.safeToUri()

        assertNotNull(result)
        assertEquals("", result.toString())
    }

    @Test
    fun testSafeGetQueryParameterWithValidParameter() {
        val uri = Uri.parse("https://example.com?param1=value1&param2=value2")
        val result = uri.safeGetQueryParameter("param1")

        assertEquals("value1", result)
    }

    @Test
    fun testSafeGetQueryParameterWithNonExistentParameter() {
        val uri = Uri.parse("https://example.com?param1=value1")
        val result = uri.safeGetQueryParameter("nonexistent")

        assertNull(result)
    }

    @Test
    fun testSafeGetQueryParameterWithEmptyUri() {
        val result = Uri.EMPTY.safeGetQueryParameter("param")

        assertNull(result)
    }

    @Test
    fun testSafeGetQueryParameterWithEncodedValues() {
        val uri = Uri.parse("https://example.com?param=hello%20world")
        val result = uri.safeGetQueryParameter("param")

        assertEquals("hello world", result)
    }

    @Test
    fun testSafeGetQueryParameterWithMultipleValues() {
        val uri = Uri.parse("https://example.com?param=value1&param=value2")
        val result = uri.safeGetQueryParameter("param")

        assertEquals("value1", result)
    }

    @Test
    fun testSafeGetQueryParameterWithEmptyValue() {
        val uri = Uri.parse("https://example.com?param=")
        val result = uri.safeGetQueryParameter("param")

        assertEquals("", result)
    }
}
