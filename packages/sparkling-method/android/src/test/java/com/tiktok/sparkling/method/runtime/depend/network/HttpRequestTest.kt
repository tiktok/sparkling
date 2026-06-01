// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.runtime.depend.network

import com.tiktok.sparkling.method.runtime.depend.common.IHostNetworkDepend
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Tests for [HttpRequest], its fluent setters, the [RequestMethod] enum,
 * [NetworkRequestImpl] delegation, and the default no-op behaviour of the
 * [AbsStringConnection] / [AbsStreamConnection] abstract base classes.
 */
class HttpRequestTest {
    @Test
    fun fluentSettersUpdateAllProperties() {
        val req = HttpRequest("https://example.com/path")
        assertEquals("https://example.com/path", req.getUrl())

        val headers = LinkedHashMap<String, String>().apply { put("X-Foo", "bar") }
        val params = mapOf("k" to "v")
        val payload = ByteArray(3) { it.toByte() }
        val files = LinkedHashMap<String, File>().apply { put("upload", File("/tmp/a")) }

        val chain =
            req
                .cacheTime(60)
                .headers(headers)
                .needAddCommonParams(true)
                .disableRedirect(true)
                .params(params)
                .sendData(payload)
                .contentEncoding("gzip")
                .contentType("application/json")
                .connectTimeOut(1_000L)
                .readTimeOut(2_000L)
                .writeTimeOut(3_000L)
                .postFilePart(files)

        // each fluent call must return the same instance to support chaining
        assertSame(req, chain)

        assertEquals(60, req.cacheTime)
        assertEquals(headers, req.headers)
        assertTrue(req.needAddCommonParams)
        assertTrue(req.disableRedirect)
        assertEquals(params, req.params)
        assertArrayEquals(payload, req.sendData)
        assertEquals("gzip", req.contentEncoding)
        assertEquals("application/json", req.contentType)
        assertEquals(1_000L, req.connectTimeOut)
        assertEquals(2_000L, req.readTimeOut)
        assertEquals(3_000L, req.writeTimeOut)
        assertEquals(files, req.postFilePart)
    }

    @Test
    fun defaultPropertyValuesAreReasonable() {
        val req = HttpRequest("https://example.com")
        assertEquals(0, req.cacheTime)
        assertNull(req.headers)
        assertFalse(req.needAddCommonParams)
        assertFalse(req.disableRedirect)
        assertNull(req.params)
        assertNull(req.sendData)
        assertNull(req.contentEncoding)
        assertNull(req.contentType)
        assertEquals(0L, req.connectTimeOut)
        assertEquals(0L, req.readTimeOut)
        assertEquals(0L, req.writeTimeOut)
        assertNull(req.postFilePart)
    }

    @Test
    fun sendDataAcceptsNull() {
        val req = HttpRequest("https://example.com").sendData(null)
        assertNull(req.sendData)
    }

    @Test
    fun doMethodsRouteToDelegateWithCorrectMethod() {
        val req = HttpRequest("https://example.com")
        val depend = mockk<IHostNetworkDepend>()
        val stringConn = mockk<AbsStringConnection>()
        val streamConn = mockk<AbsStreamConnection>()

        every { depend.requestForString(RequestMethod.GET, req) } returns stringConn
        every { depend.requestForString(RequestMethod.POST, req) } returns stringConn
        every { depend.requestForString(RequestMethod.PUT, req) } returns stringConn
        every { depend.requestForString(RequestMethod.DELETE, req) } returns stringConn
        every { depend.requestForStream(RequestMethod.GET, req) } returns streamConn
        every { depend.requestForStream(RequestMethod.POST, req) } returns streamConn
        every { depend.requestForStream(RequestMethod.DOWNLOAD, req) } returns streamConn

        assertSame(stringConn, req.doGetForString(depend))
        assertSame(stringConn, req.doPostForString(depend))
        assertSame(stringConn, req.doPutForString(depend))
        assertSame(stringConn, req.doDeleteForString(depend))
        assertSame(streamConn, req.doGetForStream(depend))
        assertSame(streamConn, req.doPostForStream(depend))
        assertSame(streamConn, req.doDownloadFile(depend))

        verify(exactly = 1) { depend.requestForString(RequestMethod.GET, req) }
        verify(exactly = 1) { depend.requestForString(RequestMethod.POST, req) }
        verify(exactly = 1) { depend.requestForString(RequestMethod.PUT, req) }
        verify(exactly = 1) { depend.requestForString(RequestMethod.DELETE, req) }
        verify(exactly = 1) { depend.requestForStream(RequestMethod.GET, req) }
        verify(exactly = 1) { depend.requestForStream(RequestMethod.POST, req) }
        verify(exactly = 1) { depend.requestForStream(RequestMethod.DOWNLOAD, req) }
    }

    @Test
    fun networkRequestImplDelegatesDirectlyToHost() {
        val req = HttpRequest("https://example.com")
        val depend = mockk<IHostNetworkDepend>()
        val stringConn = mockk<AbsStringConnection>()
        val streamConn = mockk<AbsStreamConnection>()
        every { depend.requestForString(RequestMethod.GET, req) } returns stringConn
        every { depend.requestForStream(RequestMethod.POST, req) } returns streamConn

        assertSame(stringConn, NetworkRequestImpl.requestForString(RequestMethod.GET, req, depend))
        assertSame(streamConn, NetworkRequestImpl.requestForStream(RequestMethod.POST, req, depend))
    }

    @Test
    fun requestMethodEnumExposesAllValues() {
        val values = RequestMethod.values()
        assertEquals(5, values.size)
        assertNotNull(RequestMethod.valueOf("GET"))
        assertNotNull(RequestMethod.valueOf("POST"))
        assertNotNull(RequestMethod.valueOf("PUT"))
        assertNotNull(RequestMethod.valueOf("DELETE"))
        assertNotNull(RequestMethod.valueOf("DOWNLOAD"))
    }

    @Test
    fun absStringConnectionDefaultsAreEmpty() {
        val conn = object : AbsStringConnection() {}
        assertNull(conn.getStringResponseBody())
        assertEquals(emptyMap<String, String>(), conn.getResponseHeader())
        assertNull(conn.getResponseCode())
        assertEquals("", conn.getErrorMsg())
        assertNull(conn.getException())
        assertNull(conn.getClientCode())
    }

    @Test
    fun absStreamConnectionDefaultsAreEmpty() {
        val conn = object : AbsStreamConnection() {}
        assertNull(conn.getInputStreamResponseBody())
        assertEquals(emptyMap<String, String>(), conn.getResponseHeader())
        assertEquals(0, conn.getResponseCode())
        assertEquals(0, conn.getClientCode())
        assertEquals("", conn.getErrorMsg())
        assertNull(conn.getException())
        // cancel is a no-op but should not throw
        conn.cancel()
    }
}
