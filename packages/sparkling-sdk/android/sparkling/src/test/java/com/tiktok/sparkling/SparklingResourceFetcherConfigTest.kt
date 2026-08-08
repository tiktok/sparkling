// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import com.lynx.tasm.resourceprovider.generic.LynxGenericResourceFetcher
import com.lynx.tasm.resourceprovider.media.LynxMediaResourceFetcher
import com.lynx.tasm.resourceprovider.template.LynxTemplateResourceFetcher
import io.mockk.mockk
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class SparklingResourceFetcherConfigTest {
    @Test
    fun builderDefaultsToNoFetchers() {
        val config = SparklingResourceFetcherConfig.builder().build()

        assertNull(config.genericResourceFetcher)
        assertNull(config.mediaResourceFetcher)
        assertNull(config.templateResourceFetcher)
    }

    @Test
    fun builderStoresTypedFetchers() {
        val genericFetcher = mockk<LynxGenericResourceFetcher>()
        val mediaFetcher = mockk<LynxMediaResourceFetcher>()
        val templateFetcher = mockk<LynxTemplateResourceFetcher>()

        val config =
            SparklingResourceFetcherConfig
                .builder()
                .setGenericResourceFetcher(genericFetcher)
                .setMediaResourceFetcher(mediaFetcher)
                .setTemplateResourceFetcher(templateFetcher)
                .build()

        assertSame(genericFetcher, config.genericResourceFetcher)
        assertSame(mediaFetcher, config.mediaResourceFetcher)
        assertSame(templateFetcher, config.templateResourceFetcher)
    }
}
