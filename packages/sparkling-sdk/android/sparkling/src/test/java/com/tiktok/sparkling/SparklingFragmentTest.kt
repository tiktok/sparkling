// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import android.app.Application
import android.content.Intent
import com.tiktok.sparkling.hybridkit.scheme.HybridSchemeParam
import com.tiktok.sparkling.hybridkit.utils.ColorUtil
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], packageName = "com.tiktok.sparkling")
class SparklingFragmentTest {
    private lateinit var application: Application

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        application.setTheme(R.style.AppTheme_NoActionBar)
        ColorUtil.appContext = application
        SparklingContextTransferStation.clearAllContexts()
    }

    @After
    fun tearDown() {
        SparklingContextTransferStation.clearAllContexts()
    }

    @Test
    fun newInstanceProducesFreshFragment() {
        val first = SparklingFragment.newInstance()
        val second = SparklingFragment.newInstance()
        assertNotNull(first)
        assertNotNull(second)
        // Each call returns a distinct instance.
        org.junit.Assert.assertNotSame(first, second)
    }

    @Test
    fun onCreateViewWithoutContextFallsBack() {
        // No saved SparklingContext -> the early-return branch is exercised.
        val controller = Robolectric.buildActivity(SparklingActivity::class.java).create().resume()
        val activity = controller.get()
        assertNotNull(activity)
    }

    @Test
    fun loadUrlForwardsToSparklingViewWithoutCrashing() {
        val fragment = SparklingFragment.newInstance()
        // No view attached, but the public API must remain safe.
        fragment.loadUrl()
    }
}
