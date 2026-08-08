// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import android.content.pm.ActivityInfo

/**
 * Controls the screen orientation of a full-page [SparklingActivity].
 *
 * This policy does not change the Activity that hosts an embedded [SparklingView].
 */
enum class SparklingScreenOrientationPolicy {
    SYSTEM,
    PORTRAIT,
    LANDSCAPE,
}

internal fun SparklingContext.resolveScreenOrientationPolicy(
    globalDefault: SparklingScreenOrientationPolicy?,
): SparklingScreenOrientationPolicy? =
    screenOrientationPolicy
        ?: hybridSchemeParam?.screenOrientation?.toLegacyScreenOrientationPolicy()
        ?: globalDefault

internal fun SparklingScreenOrientationPolicy.toRequestedOrientation(): Int =
    when (this) {
        SparklingScreenOrientationPolicy.SYSTEM -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        SparklingScreenOrientationPolicy.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        SparklingScreenOrientationPolicy.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

private fun String.toLegacyScreenOrientationPolicy(): SparklingScreenOrientationPolicy =
    when (this) {
        "portrait" -> SparklingScreenOrientationPolicy.PORTRAIT
        "landscape" -> SparklingScreenOrientationPolicy.LANDSCAPE
        else -> SparklingScreenOrientationPolicy.SYSTEM
    }
