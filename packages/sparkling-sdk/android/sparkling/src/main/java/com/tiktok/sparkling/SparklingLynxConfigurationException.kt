// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

/**
 * Identifies an unsupported combination of typed Sparkling Lynx options.
 */
enum class SparklingLynxConfigurationError {
    FIXED_VIEWPORT_WITH_MULTI_THREADS,
}

/**
 * Thrown before Lynx view construction when typed Sparkling options cannot be used together.
 */
class SparklingLynxConfigurationException(
    val error: SparklingLynxConfigurationError,
) : IllegalArgumentException(error.message())

internal fun SparklingContext.validateLynxConfiguration(defaultThreadStrategy: SparklingThreadStrategy?) {
    val lynxViewport = resolveLynxViewport()
    val threadStrategy = threadStrategy ?: defaultThreadStrategy
    if (lynxViewport != null && threadStrategy == SparklingThreadStrategy.MULTI_THREADS) {
        throw SparklingLynxConfigurationException(
            SparklingLynxConfigurationError.FIXED_VIEWPORT_WITH_MULTI_THREADS,
        )
    }
}

private fun SparklingLynxConfigurationError.message(): String =
    when (this) {
        SparklingLynxConfigurationError.FIXED_VIEWPORT_WITH_MULTI_THREADS -> {
            "A fixed Lynx viewport cannot be used with the MULTI_THREADS rendering strategy."
        }
    }
