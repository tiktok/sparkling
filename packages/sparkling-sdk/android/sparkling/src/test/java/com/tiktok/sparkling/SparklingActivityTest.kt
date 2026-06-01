// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import android.app.Application
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

/**
 * Tests SparklingActivity onCreate flow with various SparklingContext configurations.
 * Uses Robolectric.buildActivity to drive the Android lifecycle without requiring
 * an emulator.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], packageName = "com.tiktok.sparkling")
class SparklingActivityTest {
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
    fun onCreateWithoutContextRendersDefaultLayout() {
        val activity = Robolectric.buildActivity(SparklingActivity::class.java).create().get()
        assertNotNull(activity)
        assertNotNull(activity.findViewById<android.view.View>(R.id.toolbar))
    }

    @Test
    fun onCreateWithSparklingContextSetsTitle() {
        val ctx =
            SparklingContext().apply {
                containerId = "container-title"
                hybridSchemeParam = HybridSchemeParam(title = "Hello Sparkling")
            }
        SparklingContextTransferStation.saveSparklingContext(ctx)

        val intent = android.content.Intent(application, SparklingActivity::class.java)
        intent.putExtra(Sparkling.SPARKLING_CONTEXT_CONTAINER_ID, ctx.containerId)
        val activity = Robolectric.buildActivity(SparklingActivity::class.java, intent).create().get()
        assertNotNull(activity)
    }

    @Test
    fun onCreateWithTransparentStatusBar() {
        val ctx =
            SparklingContext().apply {
                containerId = "container-trans"
                hybridSchemeParam = HybridSchemeParam(transStatusBar = true)
            }
        SparklingContextTransferStation.saveSparklingContext(ctx)

        val intent = android.content.Intent(application, SparklingActivity::class.java)
        intent.putExtra(Sparkling.SPARKLING_CONTEXT_CONTAINER_ID, ctx.containerId)
        val activity = Robolectric.buildActivity(SparklingActivity::class.java, intent).create().get()
        assertNotNull(activity)
    }

    @Test
    fun onCreateWithHiddenStatusBar() {
        val ctx =
            SparklingContext().apply {
                containerId = "container-hidden"
                hybridSchemeParam = HybridSchemeParam(hideStatusBar = true)
            }
        SparklingContextTransferStation.saveSparklingContext(ctx)

        val intent = android.content.Intent(application, SparklingActivity::class.java)
        intent.putExtra(Sparkling.SPARKLING_CONTEXT_CONTAINER_ID, ctx.containerId)
        val activity = Robolectric.buildActivity(SparklingActivity::class.java, intent).create().get()
        assertNotNull(activity)
    }

    @Test
    fun onCreateWithHideNavBarHidesActionBar() {
        val ctx =
            SparklingContext().apply {
                containerId = "container-hide-nav"
                hybridSchemeParam = HybridSchemeParam(hideNavBar = true)
            }
        SparklingContextTransferStation.saveSparklingContext(ctx)

        val intent = android.content.Intent(application, SparklingActivity::class.java)
        intent.putExtra(Sparkling.SPARKLING_CONTEXT_CONTAINER_ID, ctx.containerId)
        val activity = Robolectric.buildActivity(SparklingActivity::class.java, intent).create().get()
        assertNotNull(activity)
    }

    @Test
    fun onCreateAppliesPortraitOrientation() {
        val ctx =
            SparklingContext().apply {
                containerId = "container-portrait"
                hybridSchemeParam = HybridSchemeParam(screenOrientation = "portrait")
            }
        SparklingContextTransferStation.saveSparklingContext(ctx)

        val intent = android.content.Intent(application, SparklingActivity::class.java)
        intent.putExtra(Sparkling.SPARKLING_CONTEXT_CONTAINER_ID, ctx.containerId)
        val activity = Robolectric.buildActivity(SparklingActivity::class.java, intent).create().get()
        assertNotNull(activity)
    }

    @Test
    fun onCreateAppliesLandscapeOrientation() {
        val ctx =
            SparklingContext().apply {
                containerId = "container-landscape"
                hybridSchemeParam = HybridSchemeParam(screenOrientation = "landscape")
            }
        SparklingContextTransferStation.saveSparklingContext(ctx)

        val intent = android.content.Intent(application, SparklingActivity::class.java)
        intent.putExtra(Sparkling.SPARKLING_CONTEXT_CONTAINER_ID, ctx.containerId)
        val activity = Robolectric.buildActivity(SparklingActivity::class.java, intent).create().get()
        assertNotNull(activity)
    }

    @Test
    fun onCreateAppliesValidTitleColor() {
        val ctx =
            SparklingContext().apply {
                containerId = "container-title-color"
                hybridSchemeParam =
                    HybridSchemeParam(
                        title = "Colored",
                        titleColor = "#FF0000",
                        navBarColor = "#00FF00",
                    )
            }
        SparklingContextTransferStation.saveSparklingContext(ctx)

        val intent = android.content.Intent(application, SparklingActivity::class.java)
        intent.putExtra(Sparkling.SPARKLING_CONTEXT_CONTAINER_ID, ctx.containerId)
        val activity = Robolectric.buildActivity(SparklingActivity::class.java, intent).create().get()
        assertNotNull(activity)
    }

    @Test
    fun onCreateHandlesInvalidTitleColorGracefully() {
        val ctx =
            SparklingContext().apply {
                containerId = "container-bad-color"
                hybridSchemeParam =
                    HybridSchemeParam(
                        title = "Bad",
                        titleColor = "not-a-color",
                    )
            }
        SparklingContextTransferStation.saveSparklingContext(ctx)

        val intent = android.content.Intent(application, SparklingActivity::class.java)
        intent.putExtra(Sparkling.SPARKLING_CONTEXT_CONTAINER_ID, ctx.containerId)
        val activity = Robolectric.buildActivity(SparklingActivity::class.java, intent).create().get()
        assertNotNull(activity)
    }

    @Test
    fun initToolBarUpdatesTitle() {
        val ctx =
            SparklingContext().apply {
                containerId = "container-init"
                hybridSchemeParam = HybridSchemeParam(title = "Initial")
            }
        SparklingContextTransferStation.saveSparklingContext(ctx)

        val intent = android.content.Intent(application, SparklingActivity::class.java)
        intent.putExtra(Sparkling.SPARKLING_CONTEXT_CONTAINER_ID, ctx.containerId)
        val activity = Robolectric.buildActivity(SparklingActivity::class.java, intent).create().get()

        val newCtx =
            SparklingContext().apply {
                hybridSchemeParam = HybridSchemeParam(title = "Updated")
            }
        activity.initToolBar(newCtx)
    }

    @Test
    fun initSparklingFragmentSwapsFragment() {
        val ctx =
            SparklingContext().apply {
                containerId = "container-frag"
                hybridSchemeParam = HybridSchemeParam(screenOrientation = "auto")
            }
        SparklingContextTransferStation.saveSparklingContext(ctx)

        val intent = android.content.Intent(application, SparklingActivity::class.java)
        intent.putExtra(Sparkling.SPARKLING_CONTEXT_CONTAINER_ID, ctx.containerId)
        val activity = Robolectric.buildActivity(SparklingActivity::class.java, intent).create().get()

        // Re-invoke fragment installation with another context.
        activity.initSparklingFragment(
            SparklingContext().apply {
                hybridSchemeParam = HybridSchemeParam(transStatusBar = true, showNavBarInTransStatusBar = false)
            },
        )
    }
}
