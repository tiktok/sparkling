// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.utils

import com.tiktok.sparkling.hybridkit.base.HybridContainerType
import com.tiktok.sparkling.hybridkit.base.HybridKitType
import com.tiktok.sparkling.hybridkit.scheme.HybridSchemeParam
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SchemeParserTest {

    @Test
    fun testParseSchemeWithLynxViewPage() {
        val scheme = "hybrid://lynxview_page?bundle=test_bundle&title=Test%20Title"
        val result = SchemeParser.parseScheme(scheme)
        
        assertNotNull(result)
        assertEquals(HybridKitType.LYNX, result!!.engineType)
        assertEquals(HybridContainerType.PAGE, result.containerType)
        assertEquals("test_bundle", result.bundle)
        assertEquals("Test Title", result.title)
    }

    @Test
    fun testParseSchemeWithLynxViewCard() {
        val scheme = "hybrid://lynxview_card?bundle=card_bundle&title=Card%20Title"
        val result = SchemeParser.parseScheme(scheme)

        assertNotNull(result)
        assertEquals(HybridKitType.LYNX, result!!.engineType)
        assertEquals(HybridContainerType.CARD, result.containerType)
        assertEquals("card_bundle", result.bundle)
        assertEquals("Card Title", result.title)
    }

    @Test
    fun testParseSchemeWithLegacyLynxView() {
        val scheme = "hybrid://lynxview?bundle=legacy_bundle"
        val result = SchemeParser.parseScheme(scheme)

        assertNotNull(result)
        assertEquals(HybridKitType.LYNX, result!!.engineType)
        assertEquals(HybridContainerType.PAGE, result.containerType)
        assertEquals("legacy_bundle", result.bundle)
    }

    @Test
    fun testParseSchemeUsesUrlWhenBundleMissing() {
        val scheme = "hybrid://lynxview_page?url=http%3A%2F%2F10.0.2.2%3A5969%2Fmain.lynx.bundle"
        val result = SchemeParser.parseScheme(scheme)

        assertNotNull(result)
        assertEquals(HybridKitType.LYNX, result!!.engineType)
        assertEquals(HybridContainerType.PAGE, result.containerType)
        assertEquals("http://10.0.2.2:5969/main.lynx.bundle", result.bundle)
    }

    @Test
    fun testParseSchemeWithWebView() {
        val scheme = "hybrid://webview?bundle=web_bundle&title=Web%20Title"
        val result = SchemeParser.parseScheme(scheme)
        
        assertNotNull(result)
        assertEquals(HybridKitType.WEB, result!!.engineType)
        assertEquals("web_bundle", result.bundle)
        assertEquals("Web Title", result.title)
    }

    @Test
    fun testParseSchemeWithUnknownHost() {
        val scheme = "hybrid://unknown?bundle=test_bundle"
        val result = SchemeParser.parseScheme(scheme)
        
        assertNull(result)
    }

    @Test
    fun testParseSchemeWithInvalidProtocol() {
        val scheme = "https://lynxview?bundle=test_bundle"
        val result = SchemeParser.parseScheme(scheme)
        
        assertNull(result)
    }

    @Test
    fun testParseSchemeWithAllParameters() {
        val scheme = "hybrid://lynxview_page?" +
                "bundle=test_bundle&" +
                "title=Test%20Title&" +
                "title_color=%23FF0000&" +
                "hide_nav_bar=1&" +
                "nav_bar_color=%23000000&" +
                "screen_orientation=portrait&" +
                "hide_status_bar=1&" +
                "trans_status_bar=1&" +
                "hide_loading=1&" +
                "loading_bg_color=%23FFFFFF&" +
                "container_bg_color=%23F0F0F0&" +
                "hide_error=1&" +
                "force_theme_style=dark"
        
        val result = SchemeParser.parseScheme(scheme)
        
        assertNotNull(result)
        with(result!!) {
            assertEquals(HybridKitType.LYNX, engineType)
            assertEquals(HybridContainerType.PAGE, containerType)
            assertEquals("test_bundle", bundle)
            assertEquals("Test Title", title)
            assertEquals("#FF0000", titleColor)
            assertTrue(hideNavBar)
            assertEquals("#000000", navBarColor)
            assertEquals("portrait", screenOrientation)
            assertTrue(hideStatusBar)
            assertTrue(transStatusBar)
            assertTrue(hideLoading)
            assertEquals("#FFFFFF", loadingBgColor)
            assertEquals("#F0F0F0", containerBgColor)
            assertTrue(hideError)
            assertEquals("dark", forceThemeStyle)
        }
    }

    @Test
    fun testParseSchemeWithBooleanParametersFalse() {
        val scheme = "hybrid://lynxview_page?" +
                "hide_nav_bar=0&" +
                "hide_status_bar=0&" +
                "trans_status_bar=0&" +
                "hide_loading=0&" +
                "hide_error=0"
        
        val result = SchemeParser.parseScheme(scheme)
        
        assertNotNull(result)
        with(result!!) {
            assertEquals(HybridContainerType.PAGE, containerType)
            assertFalse(hideNavBar)
            assertFalse(hideStatusBar)
            assertFalse(transStatusBar)
            assertFalse(hideLoading)
            assertFalse(hideError)
        }
    }

    @Test
    fun testParseSchemeWithMissingParameters() {
        val scheme = "hybrid://lynxview_page"
        val result = SchemeParser.parseScheme(scheme)
        
        assertNotNull(result)
        with(result!!) {
            assertEquals(HybridKitType.LYNX, engineType)
            assertEquals(HybridContainerType.PAGE, containerType)
            assertNull(bundle)
            assertNull(title)
            assertNull(titleColor)
            assertFalse(hideNavBar)
            assertNull(navBarColor)
            assertNull(screenOrientation)
            assertFalse(hideStatusBar)
            assertFalse(transStatusBar)
            assertFalse(hideLoading)
            assertNull(loadingBgColor)
            assertNull(containerBgColor)
            assertFalse(hideError)
            assertNull(forceThemeStyle)
        }
    }

    @Test
    fun testParseSchemeWithEmptyString() {
        val scheme = ""
        val result = SchemeParser.parseScheme(scheme)
        
        assertNull(result)
    }

    @Test
    fun testParseSchemeWithJustProtocol() {
        val scheme = "hybrid://"
        val result = SchemeParser.parseScheme(scheme)
        
        assertNull(result)
    }

    @Test
    fun testParseSchemeWithSpecialCharacters() {
        val scheme = "hybrid://lynxview_page?title=Special%20%26%20Characters%20%23%40%24&bundle=test"
        val result = SchemeParser.parseScheme(scheme)
        
        assertNotNull(result)
        val parsed = result!!
        assertEquals(HybridContainerType.PAGE, parsed.containerType)
        assertEquals("Special & Characters #@$", parsed.title)
        assertEquals("test", parsed.bundle)
    }

    @Test
    fun testParseSchemeWithInvalidBooleanValues() {
        val scheme = "hybrid://lynxview_page?hide_nav_bar=invalid&hide_status_bar=true"
        val result = SchemeParser.parseScheme(scheme)
        
        assertNotNull(result)
        with(result!!) {
            assertEquals(HybridContainerType.PAGE, containerType)
            assertFalse(hideNavBar)
            assertFalse(hideStatusBar)
        }
    }

    @Test
    fun testParseSchemeWithDuplicateParameters() {
        val scheme = "hybrid://lynxview_page?bundle=first&bundle=second&title=first_title&title=second_title"
        val result = SchemeParser.parseScheme(scheme)
        
        assertNotNull(result)
        val parsed = result!!
        assertEquals(HybridContainerType.PAGE, parsed.containerType)
        assertEquals("first", parsed.bundle)
        assertEquals("first_title", parsed.title)
    }

    @Test
    fun testCustomSchemeParserHandlesCustomSchemes() {
        val expected = HybridSchemeParam().apply {
            engineType = HybridKitType.LYNX
            bundle = "custom_bundle"
        }

        SchemeParser.setCustomSchemeParser {
            if (it.startsWith("custom://")) expected else null
        }

        try {
            val result = SchemeParser.parseScheme("custom://lynxview_page?bundle=ignored")
            assertSame(expected, result)
        } finally {
            SchemeParser.setCustomSchemeParser(null)
        }
    }

    @Test
    fun testCustomSchemeParserFallsBackToDefaultWhenNull() {
        var invoked = false
        SchemeParser.setCustomSchemeParser {
            invoked = true
            null
        }

        try {
            val result = SchemeParser.parseScheme("hybrid://lynxview?bundle=fallback_bundle")

            assertTrue(invoked)
            assertNotNull(result)
            assertEquals("fallback_bundle", result?.bundle)
            assertEquals(HybridKitType.LYNX, result?.engineType)
        } finally {
            SchemeParser.setCustomSchemeParser(null)
        }
    }
}
