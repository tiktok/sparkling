// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.utils

import android.content.Context
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.tiktok.sparkling.hybridkit.utils.ColorUtil
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ColorUtilTest {
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        ColorUtil.appContext = mockContext
    }

    @Test
    fun testRgbaToArgbWithValidEightCharHex() {
        val result = ColorUtil.rgbaToArgb("99887766")
        assertEquals("#66998877", result)
    }

    @Test
    fun testRgbaToArgbWithValidNineCharHexWithHash() {
        val result = ColorUtil.rgbaToArgb("#99887766")
        assertEquals("#66998877", result)
    }

    @Test
    fun testRgbaToArgbWithSixCharHex() {
        val result = ColorUtil.rgbaToArgb("998877")
        assertEquals("#998877", result)
    }

    @Test
    fun testRgbaToArgbWithInvalidLength() {
        val result = ColorUtil.rgbaToArgb("99")
        assertEquals("99", result)
    }

    @Test
    fun testRgbaToArgbWithException() {
        val result = ColorUtil.rgbaToArgb("")
        assertEquals("#00000000", result)
    }

    @Test
    fun testParseColorSafelyWithValidColor() {
        val result = ColorUtil.parseColorSafely("#FF0000")
        assertEquals(Color.RED, result)
    }

    @Test
    fun testParseColorSafelyWithInvalidColor() {
        val result = ColorUtil.parseColorSafely("invalid_color")
        assertEquals(Color.TRANSPARENT, result)
    }

    @Test
    fun testGetColor() {
        val resourceId = 123
        val expectedColor = Color.BLUE

        mockkStatic(ContextCompat::class)
        every { ContextCompat.getColor(mockContext, resourceId) } returns expectedColor

        val result = ColorUtil.getColor(resourceId)

        assertEquals(expectedColor, result)
        verify { ContextCompat.getColor(mockContext, resourceId) }
    }
}
