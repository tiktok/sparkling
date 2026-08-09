// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.lynx

import com.lynx.tasm.LynxBooleanOption
import com.lynx.tasm.LynxViewBuilder
import com.lynx.tasm.provider.AbsTemplateProvider
import com.tiktok.sparkling.SparklingContext
import com.tiktok.sparkling.SparklingResourceFetcherConfig
import com.tiktok.sparkling.hybridkit.base.IHybridKitLifeCycle
import com.tiktok.sparkling.hybridkit.config.SparklingLynxConfig

internal object SparklingResourceFetcherConfigurator {
    fun resolve(
        sparklingContext: SparklingContext?,
        lynxConfig: SparklingLynxConfig?,
    ): SparklingResourceFetcherConfig? =
        sparklingContext?.resourceFetcherConfig
            ?: sparklingContext?.let { lynxConfig?.resourceFetcherFactory?.create(it) }

    fun apply(
        viewBuilder: LynxViewBuilder,
        config: SparklingResourceFetcherConfig?,
        defaultTemplateProvider: AbsTemplateProvider?,
        lifeCycle: IHybridKitLifeCycle?,
        kitViewProvider: () -> SimpleLynxKitView?,
    ) {
        val hasTypedFetcher =
            config?.genericResourceFetcher != null ||
                config?.mediaResourceFetcher != null ||
                config?.templateResourceFetcher != null
        if (hasTypedFetcher) {
            viewBuilder.setEnableGenericResourceFetcher(LynxBooleanOption.TRUE)
        }
        config?.genericResourceFetcher?.let {
            viewBuilder.setGenericResourceFetcher(it)
        }
        config?.mediaResourceFetcher?.let {
            viewBuilder.setMediaResourceFetcher(it)
        }
        val templateResourceFetcher = config?.templateResourceFetcher
        if (templateResourceFetcher != null) {
            viewBuilder.setTemplateResourceFetcher(
                SparklingTemplateResourceFetcher(
                    templateResourceFetcher,
                    lifeCycle,
                    kitViewProvider,
                ),
            )
        } else {
            defaultTemplateProvider?.let {
                viewBuilder.setTemplateProvider(
                    SimpleLynxTemplateProvider(it, lifeCycle, kitViewProvider),
                )
            }
        }
    }
}
