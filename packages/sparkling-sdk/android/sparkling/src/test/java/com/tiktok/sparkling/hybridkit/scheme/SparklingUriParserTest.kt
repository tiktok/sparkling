// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.scheme

import android.net.Uri
import android.os.Bundle
import com.tiktok.sparkling.hybridkit.base.HybridContainerType
import com.tiktok.sparkling.hybridkit.base.HybridKitType
import com.tiktok.sparkling.hybridkit.scheme.BaseSchemeParam
import com.tiktok.sparkling.hybridkit.scheme.HybridSchemeParam
import com.tiktok.sparkling.hybridkit.scheme.SparklingUriParser
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SparklingUriParserTest {
    @Before
    fun setUp() {
        // Clear any cached data
    }

    @Test
    fun testParseUriWithQueryParameters() {
        val uri = Uri.parse("https://example.com?param1=value1&param2=value2&param3=value3")
        val result = SparklingUriParser.parseUri(uri)

        assertEquals(3, result.size)
        assertEquals("value1", result["param1"])
        assertEquals("value2", result["param2"])
        assertEquals("value3", result["param3"])
    }

    @Test
    fun testParseUriWithExtraParameters() {
        val uri = Uri.parse("https://example.com?param1=value1")
        val extra = mapOf("param2" to "value2", "param3" to "value3")
        val result = SparklingUriParser.parseUri(uri, extra)

        assertEquals(3, result.size)
        assertEquals("value1", result["param1"])
        assertEquals("value2", result["param2"])
        assertEquals("value3", result["param3"])
    }

    @Test
    fun testParseUriWithOverrideParameters() {
        val uri = Uri.parse("https://example.com?param1=uri_value")
        val extra = mapOf("param1" to "extra_value")
        val result = SparklingUriParser.parseUri(uri, extra)

        assertEquals(1, result.size)
        assertEquals("uri_value", result["param1"])
    }

    @Test
    fun testParseQueryMapWithBundle() {
        val uri = Uri.parse("https://example.com?param1=value1")
        val bundle =
            Bundle().apply {
                putString("param2", "value2")
                putInt("param3", 123)
                putBoolean("param4", true)
            }

        val result = SparklingUriParser.parseQueryMap(uri, bundle = bundle)

        assertEquals(4, result.size)
        assertEquals("value1", result["param1"])
        assertEquals("value2", result["param2"])
        assertEquals("123", result["param3"])
        assertEquals("true", result["param4"])
    }

    @Test
    fun testParseQueryMapWithExtra() {
        val uri = Uri.parse("https://example.com?param1=value1")
        val extra = mapOf("param2" to "value2", "param3" to "value3")

        val result = SparklingUriParser.parseQueryMap(uri, extra)

        assertEquals(3, result.size)
        assertEquals("value1", result["param1"])
        assertEquals("value2", result["param2"])
        assertEquals("value3", result["param3"])
    }

    @Test
    fun testParseQueryMapWithNestedUrl() {
        val nestedUrl = "https://nested.com?nested_param=nested_value"
        val uri = Uri.parse("https://example.com?url=${Uri.encode(nestedUrl)}&param1=value1")

        val result = SparklingUriParser.parseQueryMap(uri)

        assertTrue(result.containsKey("nested_param"))
        assertEquals("nested_value", result["nested_param"])
        assertEquals("value1", result["param1"])
    }

    @Test
    fun testSaveAndQueryParsedParams() {
        val containerId = "test-container-123"
        val queryMap =
            mutableMapOf(
                "param1" to "value1",
                "param2" to "value2",
            )

        SparklingUriParser.saveUriAndQueries(containerId, queryMap)
        val result = SparklingUriParser.queryParsedParams(containerId)

        assertEquals(2, result.size)
        assertEquals("value1", result["param1"])
        assertEquals("value2", result["param2"])
    }

    @Test
    fun testQueryParsedParamsWithNonExistentContainer() {
        val result = SparklingUriParser.queryParsedParams("non-existent-container")

        assertTrue(result.isEmpty())
    }

    @Test
    fun testAppendMissingQueryToUri_preservesExistingAndAddsMissing() {
        val uri = Uri.parse("https://example.com?a=1")
        val merged = mapOf("a" to "fromMerged", "b" to "2")
        val out = SparklingUriParser.appendMissingQueryToUri(uri, merged)
        assertEquals("1", out.getQueryParameter("a"))
        assertEquals("2", out.getQueryParameter("b"))
    }

    @Test
    fun testBundleToMap() {
        val bundle =
            Bundle().apply {
                putString("string_key", "string_value")
                putInt("int_key", 42)
                putBoolean("boolean_key", false)
                putDouble("double_key", 3.14)
            }

        val result = SparklingUriParser.bundleToMap(bundle)

        assertEquals(4, result.size)
        assertEquals("string_value", result["string_key"])
        assertEquals("42", result["int_key"])
        assertEquals("false", result["boolean_key"])
        assertEquals("3.14", result["double_key"])
    }

    @Test
    fun testBundleToMapWithEmptyBundle() {
        val bundle = Bundle()
        val result = SparklingUriParser.bundleToMap(bundle)

        assertTrue(result.isEmpty())
    }

    @Test
    fun testParseUriWithNoQueryParameters() {
        val uri = Uri.parse("https://example.com/path")
        val result = SparklingUriParser.parseUri(uri)
        assertTrue(result.isEmpty())
    }

    @Test
    fun testParseUriWithEncodedValues() {
        val uri = Uri.parse("https://example.com?path=${Uri.encode("/some/path with spaces")}")
        val result = SparklingUriParser.parseUri(uri)
        assertEquals("/some/path with spaces", result["path"])
    }

    @Test
    fun testSaveAndQueryParsedParamsOverwrite() {
        val containerId = "container-overwrite"
        val first = mutableMapOf("k" to "v1")
        val second = mutableMapOf("k" to "v2", "k2" to "extra")

        SparklingUriParser.saveUriAndQueries(containerId, first)
        SparklingUriParser.saveUriAndQueries(containerId, second)
        val result = SparklingUriParser.queryParsedParams(containerId)

        assertEquals("v2", result["k"])
        assertEquals("extra", result["k2"])
    }

    @Test
    fun testParseQueryMapWithBundleUrlKeyIsExpandedIntoMap() {
        val nestedUrl = "https://nested.example.com?nested_key=nested_val"
        val bundle = Bundle().apply { putString("url", nestedUrl) }
        val uri = Uri.parse("https://outer.example.com?outer_key=outer_val")

        val result = SparklingUriParser.parseQueryMap(uri, bundle = bundle)

        assertTrue(result.containsKey("nested_key"))
        assertEquals("nested_val", result["nested_key"])
        assertEquals("outer_val", result["outer_key"])
    }

    @Test
    fun testParseQueryMapExtraUrlKeyIsExpandedIntoMap() {
        val nestedUrl = "https://nested.example.com?extra_key=extra_val"
        val extra = mapOf("url" to nestedUrl)
        val uri = Uri.parse("https://outer.example.com?outer_key=outer_val")

        val result = SparklingUriParser.parseQueryMap(uri, extra)

        assertTrue(result.containsKey("extra_key"))
        assertEquals("extra_val", result["extra_key"])
    }

    @Test
    fun testOuterUriParametersTakePriorityOverNestedUrlParameters() {
        val nestedUrl = "https://nested.example.com?shared_key=from_nested"
        val uri = Uri.parse("https://outer.example.com?shared_key=from_outer&url=${Uri.encode(nestedUrl)}")

        val result = SparklingUriParser.parseQueryMap(uri)

        assertEquals("from_outer", result["shared_key"])
    }

    @Test
    fun testAppendMissingQueryToUri_doesNotDuplicateExistingKeys() {
        val uri = Uri.parse("https://example.com?a=1&b=2")
        val merged = mapOf("a" to "should_not_overwrite", "c" to "3")
        val out = SparklingUriParser.appendMissingQueryToUri(uri, merged)

        assertEquals("1", out.getQueryParameter("a"))
        assertEquals("2", out.getQueryParameter("b"))
        assertEquals("3", out.getQueryParameter("c"))
    }

    @Test
    fun testAppendMissingQueryToUri_withEmptyMergedParams() {
        val uri = Uri.parse("https://example.com?a=1")
        val out = SparklingUriParser.appendMissingQueryToUri(uri, emptyMap())
        assertEquals("1", out.getQueryParameter("a"))
    }
}

class TestSchemeParam : BaseSchemeParam(HybridKitType.LYNX)

@RunWith(RobolectricTestRunner::class)
class ApplyEngineTest {
    @Test
    fun testApplyEngineWithWebViewHost() {
        val uri = Uri.parse("https://webview.example.com/path")
        val schemeParam = TestSchemeParam()

        with(SparklingUriParser) {
            schemeParam.applyEngine(uri)
        }

        assertEquals(HybridKitType.WEB, schemeParam.engineType)
    }

    @Test
    fun testApplyEngineWithLynxViewPageHost() {
        val uri = Uri.parse("https://lynxview_page.example.com/path")
        val schemeParam = TestSchemeParam()

        with(SparklingUriParser) {
            schemeParam.applyEngine(uri)
        }

        assertEquals(HybridKitType.LYNX, schemeParam.engineType)
    }

    @Test
    fun testApplyEngineWithLynxViewCardHost() {
        val uri = Uri.parse("https://lynxview_card.example.com/path")
        val schemeParam = TestSchemeParam()

        with(SparklingUriParser) {
            schemeParam.applyEngine(uri)
        }

        assertEquals(HybridKitType.LYNX, schemeParam.engineType)
    }

    @Test
    fun testApplyEngineSetsContainerTypeForHybridSchemeParam() {
        val uri = Uri.parse("https://lynxview_card.example.com/path")
        val schemeParam = HybridSchemeParam()

        with(SparklingUriParser) {
            schemeParam.applyEngine(uri)
        }

        assertEquals(HybridKitType.LYNX, schemeParam.engineType)
        assertEquals(HybridContainerType.CARD, schemeParam.containerType)
    }

    @Test
    fun testApplyEngineWithUnknownHost() {
        val uri = Uri.parse("https://unknown.example.com/path")
        val schemeParam = TestSchemeParam()

        with(SparklingUriParser) {
            schemeParam.applyEngine(uri)
        }

        assertEquals(HybridKitType.UNKNOWN, schemeParam.engineType)
    }

    @Test
    fun testApplyEngineWithNullHost() {
        val uri = Uri.parse("/path")
        val schemeParam = TestSchemeParam()

        with(SparklingUriParser) {
            schemeParam.applyEngine(uri)
        }

        assertEquals(HybridKitType.UNKNOWN, schemeParam.engineType)
    }

    @Test
    fun testApplyEngineSetsPageContainerTypeForLynxViewPage() {
        val uri = Uri.parse("https://lynxview_page.example.com/path")
        val schemeParam = HybridSchemeParam()

        with(SparklingUriParser) {
            schemeParam.applyEngine(uri)
        }

        assertEquals(HybridKitType.LYNX, schemeParam.engineType)
        assertEquals(HybridContainerType.PAGE, schemeParam.containerType)
    }

    @Test
    fun testApplyEngineContainerTypeUnknownForWebViewHost() {
        val uri = Uri.parse("https://webview.example.com/path")
        val schemeParam = HybridSchemeParam()

        with(SparklingUriParser) {
            schemeParam.applyEngine(uri)
        }

        assertEquals(HybridKitType.WEB, schemeParam.engineType)
        assertEquals(HybridContainerType.UNKNOWN, schemeParam.containerType)
    }

    @Test
    fun testApplyEngineHostMatchingIsCaseInsensitive() {
        val uri = Uri.parse("https://WEBVIEW.example.com/path")
        val schemeParam = TestSchemeParam()

        with(SparklingUriParser) {
            schemeParam.applyEngine(uri)
        }

        assertEquals(HybridKitType.WEB, schemeParam.engineType)
    }
}
