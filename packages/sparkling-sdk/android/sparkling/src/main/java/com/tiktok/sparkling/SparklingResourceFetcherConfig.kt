// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import com.lynx.tasm.resourceprovider.generic.LynxGenericResourceFetcher
import com.lynx.tasm.resourceprovider.media.LynxMediaResourceFetcher
import com.lynx.tasm.resourceprovider.template.LynxTemplateResourceFetcher

/**
 * Typed Lynx resource fetchers used by one Sparkling page.
 */
class SparklingResourceFetcherConfig private constructor(
    val genericResourceFetcher: LynxGenericResourceFetcher?,
    val mediaResourceFetcher: LynxMediaResourceFetcher?,
    val templateResourceFetcher: LynxTemplateResourceFetcher?,
) {
    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var genericResourceFetcher: LynxGenericResourceFetcher? = null
        private var mediaResourceFetcher: LynxMediaResourceFetcher? = null
        private var templateResourceFetcher: LynxTemplateResourceFetcher? = null

        fun setGenericResourceFetcher(fetcher: LynxGenericResourceFetcher?): Builder =
            apply {
                genericResourceFetcher = fetcher
            }

        fun setMediaResourceFetcher(fetcher: LynxMediaResourceFetcher?): Builder =
            apply {
                mediaResourceFetcher = fetcher
            }

        fun setTemplateResourceFetcher(fetcher: LynxTemplateResourceFetcher?): Builder =
            apply {
                templateResourceFetcher = fetcher
            }

        fun build(): SparklingResourceFetcherConfig =
            SparklingResourceFetcherConfig(
                genericResourceFetcher,
                mediaResourceFetcher,
                templateResourceFetcher,
            )
    }
}

/**
 * Creates typed resource fetchers for a Sparkling page.
 */
fun interface SparklingResourceFetcherFactory {
    fun create(sparklingContext: SparklingContext): SparklingResourceFetcherConfig?
}
