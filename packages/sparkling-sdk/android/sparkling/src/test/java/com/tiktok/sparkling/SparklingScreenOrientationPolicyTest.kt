// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import android.content.pm.ActivityInfo
import androidx.fragment.app.FragmentActivity
import com.tiktok.sparkling.hybridkit.scheme.HybridSchemeParam
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], packageName = "com.tiktok.sparkling")
class SparklingScreenOrientationPolicyTest {
    @Test
    fun pagePolicyTakesPrecedenceOverSchemeAndGlobalDefault() {
        val context =
            SparklingContext().apply {
                screenOrientationPolicy = SparklingScreenOrientationPolicy.LANDSCAPE
                hybridSchemeParam = HybridSchemeParam(screenOrientation = "portrait")
            }

        assertEquals(
            SparklingScreenOrientationPolicy.LANDSCAPE,
            context.resolveScreenOrientationPolicy(SparklingScreenOrientationPolicy.PORTRAIT),
        )
    }

    @Test
    fun explicitSystemPolicyTakesPrecedenceOverGlobalDefault() {
        val context =
            SparklingContext().apply {
                screenOrientationPolicy = SparklingScreenOrientationPolicy.SYSTEM
                hybridSchemeParam = HybridSchemeParam(screenOrientation = "portrait")
            }

        assertEquals(
            SparklingScreenOrientationPolicy.SYSTEM,
            context.resolveScreenOrientationPolicy(SparklingScreenOrientationPolicy.LANDSCAPE),
        )
    }

    @Test
    fun pagePortraitPolicyTakesPrecedenceOverSchemeAndGlobalDefault() {
        val context =
            SparklingContext().apply {
                screenOrientationPolicy = SparklingScreenOrientationPolicy.PORTRAIT
                hybridSchemeParam = HybridSchemeParam(screenOrientation = "landscape")
            }

        assertEquals(
            SparklingScreenOrientationPolicy.PORTRAIT,
            context.resolveScreenOrientationPolicy(SparklingScreenOrientationPolicy.LANDSCAPE),
        )
    }

    @Test
    fun canonicalSchemePolicyTakesPrecedenceOverGlobalDefault() {
        val context =
            SparklingContext().apply {
                hybridSchemeParam = HybridSchemeParam(screenOrientation = "portrait")
            }

        assertEquals(
            SparklingScreenOrientationPolicy.PORTRAIT,
            context.resolveScreenOrientationPolicy(SparklingScreenOrientationPolicy.LANDSCAPE),
        )
    }

    @Test
    fun unknownLegacySchemeValuePreservesSystemBehavior() {
        val context =
            SparklingContext().apply {
                hybridSchemeParam = HybridSchemeParam(screenOrientation = "auto")
            }

        assertEquals(
            SparklingScreenOrientationPolicy.SYSTEM,
            context.resolveScreenOrientationPolicy(SparklingScreenOrientationPolicy.LANDSCAPE),
        )
    }

    @Test
    fun globalDefaultIsUsedWhenPageConfigurationIsAbsent() {
        assertEquals(
            SparklingScreenOrientationPolicy.LANDSCAPE,
            SparklingContext()
                .resolveScreenOrientationPolicy(SparklingScreenOrientationPolicy.LANDSCAPE),
        )
    }

    @Test
    fun unsetConfigurationLeavesAndroidDefaultUntouched() {
        assertNull(SparklingContext().resolveScreenOrientationPolicy(null))
    }

    @Test
    fun policiesMapToPublicAndroidRequestedOrientationValues() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            SparklingScreenOrientationPolicy.SYSTEM.toRequestedOrientation(),
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            SparklingScreenOrientationPolicy.PORTRAIT.toRequestedOrientation(),
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            SparklingScreenOrientationPolicy.LANDSCAPE.toRequestedOrientation(),
        )
    }

    @Test
    fun embeddedViewDoesNotChangeHostActivityOrientation() {
        val activity =
            Robolectric
                .buildActivity(FragmentActivity::class.java)
                .setup()
                .get()
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
        val context =
            SparklingContext().apply {
                screenOrientationPolicy = SparklingScreenOrientationPolicy.LANDSCAPE
            }

        val view = Sparkling.build(activity, context).createView(withoutPrepare = true)

        assertNotNull(view)
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
            activity.requestedOrientation,
        )
    }
}
