// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.lynx

import android.app.Application
import com.lynx.tasm.LynxBooleanOption
import com.lynx.tasm.LynxViewBuilder
import com.lynx.tasm.provider.AbsTemplateProvider
import com.lynx.tasm.resourceprovider.generic.LynxGenericResourceFetcher
import com.lynx.tasm.resourceprovider.media.LynxMediaResourceFetcher
import com.lynx.tasm.resourceprovider.template.LynxTemplateResourceFetcher
import com.tiktok.sparkling.SparklingContext
import com.tiktok.sparkling.SparklingResourceFetcherConfig
import com.tiktok.sparkling.SparklingResourceFetcherFactory
import com.tiktok.sparkling.hybridkit.config.SparklingLynxConfig
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class SparklingResourceFetcherConfiguratorTest {
    @Test
    fun pageConfigTakesPrecedenceOverGlobalFactory() {
        val pageConfig = SparklingResourceFetcherConfig.builder().build()
        val globalConfig = SparklingResourceFetcherConfig.builder().build()
        val sparklingContext =
            SparklingContext().apply {
                resourceFetcherConfig = pageConfig
            }
        var factoryCalls = 0
        val lynxConfig =
            SparklingLynxConfig.build(mockk<Application>()) {
                setResourceFetcherFactory(
                    SparklingResourceFetcherFactory {
                        factoryCalls += 1
                        globalConfig
                    },
                )
            }

        val resolved = SparklingResourceFetcherConfigurator.resolve(sparklingContext, lynxConfig)

        assertSame(pageConfig, resolved)
        assertEquals(0, factoryCalls)
    }

    @Test
    fun globalFactoryCreatesPerPageConfigWhenOverrideIsAbsent() {
        val globalConfig = SparklingResourceFetcherConfig.builder().build()
        val sparklingContext = SparklingContext()
        var receivedContext: SparklingContext? = null
        val lynxConfig =
            SparklingLynxConfig.build(mockk<Application>()) {
                setResourceFetcherFactory(
                    SparklingResourceFetcherFactory {
                        receivedContext = it
                        globalConfig
                    },
                )
            }

        val resolved = SparklingResourceFetcherConfigurator.resolve(sparklingContext, lynxConfig)

        assertSame(globalConfig, resolved)
        assertSame(sparklingContext, receivedContext)
    }

    @Test
    fun resolveReturnsNullWithoutPageConfigOrFactory() {
        assertNull(SparklingResourceFetcherConfigurator.resolve(SparklingContext(), null))
    }

    @Test
    fun applyInstallsTypedFetchersAndEnablesGenericFetching() {
        val viewBuilder = mockk<LynxViewBuilder>(relaxed = true)
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

        SparklingResourceFetcherConfigurator.apply(
            viewBuilder,
            config,
            mockk<AbsTemplateProvider>(),
            null,
        ) { null }

        verify(exactly = 1) { viewBuilder.setGenericResourceFetcher(genericFetcher) }
        verify(exactly = 1) {
            viewBuilder.setEnableGenericResourceFetcher(LynxBooleanOption.TRUE)
        }
        verify(exactly = 1) { viewBuilder.setMediaResourceFetcher(mediaFetcher) }
        verify(exactly = 1) {
            viewBuilder.setTemplateResourceFetcher(any<SparklingTemplateResourceFetcher>())
        }
        verify(exactly = 0) { viewBuilder.setTemplateProvider(any()) }
    }

    @Test
    fun applyEnablesResourcePipelineForTemplateOnlyFetcher() {
        val viewBuilder = mockk<LynxViewBuilder>(relaxed = true)
        val templateFetcher = mockk<LynxTemplateResourceFetcher>()
        val config =
            SparklingResourceFetcherConfig
                .builder()
                .setTemplateResourceFetcher(templateFetcher)
                .build()

        SparklingResourceFetcherConfigurator.apply(
            viewBuilder,
            config,
            mockk<AbsTemplateProvider>(),
            null,
        ) { null }

        verify(exactly = 1) {
            viewBuilder.setEnableGenericResourceFetcher(LynxBooleanOption.TRUE)
        }
        verify(exactly = 1) {
            viewBuilder.setTemplateResourceFetcher(any<SparklingTemplateResourceFetcher>())
        }
        verify(exactly = 0) { viewBuilder.setGenericResourceFetcher(any()) }
        verify(exactly = 0) { viewBuilder.setMediaResourceFetcher(any()) }
    }

    @Test
    fun applyEnablesResourcePipelineForMediaOnlyFetcher() {
        val viewBuilder = mockk<LynxViewBuilder>(relaxed = true)
        val mediaFetcher = mockk<LynxMediaResourceFetcher>()
        val config =
            SparklingResourceFetcherConfig
                .builder()
                .setMediaResourceFetcher(mediaFetcher)
                .build()

        SparklingResourceFetcherConfigurator.apply(
            viewBuilder,
            config,
            mockk<AbsTemplateProvider>(),
            null,
        ) { null }

        verify(exactly = 1) {
            viewBuilder.setEnableGenericResourceFetcher(LynxBooleanOption.TRUE)
        }
        verify(exactly = 1) { viewBuilder.setMediaResourceFetcher(mediaFetcher) }
        verify(exactly = 0) { viewBuilder.setGenericResourceFetcher(any()) }
        verify(exactly = 1) {
            viewBuilder.setTemplateProvider(any<SimpleLynxTemplateProvider>())
        }
    }

    @Test
    fun applyKeepsDefaultTemplateProviderPathWhenTypedTemplateIsUnset() {
        val viewBuilder = mockk<LynxViewBuilder>(relaxed = true)
        val defaultTemplateProvider = mockk<AbsTemplateProvider>()

        SparklingResourceFetcherConfigurator.apply(
            viewBuilder,
            SparklingResourceFetcherConfig.builder().build(),
            defaultTemplateProvider,
            null,
        ) { null }

        verify(exactly = 1) {
            viewBuilder.setTemplateProvider(any<SimpleLynxTemplateProvider>())
        }
        verify(exactly = 0) { viewBuilder.setTemplateResourceFetcher(any()) }
        verify(exactly = 0) {
            viewBuilder.setEnableGenericResourceFetcher(any())
        }
    }
}
