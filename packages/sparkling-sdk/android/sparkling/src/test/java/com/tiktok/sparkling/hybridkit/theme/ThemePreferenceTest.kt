// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.hybridkit.theme

import android.app.Application
import android.content.Context
import android.view.View
import com.tiktok.sparkling.hybridkit.HybridContext
import com.tiktok.sparkling.hybridkit.HybridEnvironment
import com.tiktok.sparkling.hybridkit.HybridKit
import com.tiktok.sparkling.hybridkit.KitViewManager
import com.tiktok.sparkling.hybridkit.base.IKitView
import com.tiktok.sparkling.hybridkit.config.RuntimeInfo
import com.tiktok.sparkling.hybridkit.utils.GlobalPropsUtils
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IBridgeContext
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.SparklingBridgeManager
import com.tiktok.sparkling.method.registry.core.model.idl.CompletionBlock
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    packageName = "com.tiktok.sparkling",
)
class ThemePreferenceTest {
    private lateinit var application: Application
    private val trackedContainerIds = mutableListOf<String>()

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        HybridEnvironment.instance.context = application
        application
            .getSharedPreferences(ThemePreferenceManager.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        trackedContainerIds.forEach {
            KitViewManager.removeKitView(it)
            GlobalPropsUtils.instance.flushGlobalProps(it)
        }
        application
            .getSharedPreferences(ThemePreferenceManager.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun defaultsToFollowSystemAndPersistsPreference() {
        assertEquals(
            ThemePreference.FOLLOW_SYSTEM,
            ThemePreferenceManager.getPreference(application),
        )

        ThemePreferenceManager.setPreference(application, ThemePreference.DARK)

        assertEquals(
            ThemePreference.DARK,
            ThemePreferenceManager.getPreference(application),
        )
    }

    @Test
    fun injectsPersistedPreferenceIntoNewContainerGlobalProps() {
        ThemePreferenceManager.setPreference(application, ThemePreference.LIGHT)
        val hybridContext = HybridContext()
        trackedContainerIds += hybridContext.containerId

        GlobalPropsUtils.instance.init(hybridContext, application)

        assertEquals(
            ThemePreference.LIGHT.value,
            GlobalPropsUtils.instance
                .getGlobalProps(hybridContext.containerId)[ThemePreferenceManager.GLOBAL_PROPS_KEY],
        )
        assertNotNull(
            GlobalPropsUtils.instance
                .getGlobalProps(hybridContext.containerId)[RuntimeInfo.QUERY_ITEMS],
        )
    }

    @Test
    fun broadcastsPreferenceAndUpdatesTrackedGlobalProps() {
        val firstView = RecordingKitView(application, "theme-container-1")
        val secondView = RecordingKitView(application, "theme-container-2")
        listOf(firstView, secondView).forEach {
            trackedContainerIds += it.hybridContext.containerId
            GlobalPropsUtils.instance.init(it.hybridContext, application)
            KitViewManager.addKitView(it)
        }

        ThemePreferenceManager.setPreference(application, ThemePreference.DARK)
        shadowOf(application.mainLooper).idle()

        listOf(firstView, secondView).forEach {
            assertEquals(
                mapOf(ThemePreferenceManager.GLOBAL_PROPS_KEY to ThemePreference.DARK.value),
                it.lastGlobalPropsUpdate,
            )
            assertEquals(
                ThemePreference.DARK.value,
                GlobalPropsUtils.instance
                    .getGlobalProps(it.hybridContext.containerId)[ThemePreferenceManager.GLOBAL_PROPS_KEY],
            )
        }
    }

    @Test
    fun builtInMethodPersistsAndReturnsNormalizedPreference() {
        val bridgeContext =
            mockk<IBridgeContext>(relaxed = true) {
                every { context } returns application
                every { containerID } returns "source-container"
            }
        val params =
            mockk<AbsSetThemePreferenceMethod.ParamModel> {
                every { preference } returns " DARK "
            }
        val callback = ResultRecorder()
        val method = SetThemePreferenceMethod().apply { setBridgeContext(bridgeContext) }

        method.handle(params, callback, BridgePlatformType.LYNX)

        assertNull(callback.failureCode)
        assertEquals(ThemePreference.DARK.value, callback.successResult?.preference)
        assertEquals(
            ThemePreference.DARK,
            ThemePreferenceManager.getPreference(application),
        )
    }

    @Test
    fun builtInMethodRejectsInvalidPreference() {
        val bridgeContext =
            mockk<IBridgeContext>(relaxed = true) {
                every { context } returns application
            }
        val params =
            mockk<AbsSetThemePreferenceMethod.ParamModel> {
                every { preference } returns "sepia"
            }
        val callback = ResultRecorder()
        val method = SetThemePreferenceMethod().apply { setBridgeContext(bridgeContext) }

        method.handle(params, callback, BridgePlatformType.LYNX)

        assertEquals(IDLBridgeMethod.INVALID_PARAM, callback.failureCode)
        assertNull(callback.successResult)
    }

    @Test
    fun hybridKitRegistersBuiltInThemeMethod() {
        HybridKit.init(application)

        val methodProvider =
            SparklingBridgeManager.findIDLMethodProvider(
                BridgePlatformType.LYNX,
                AbsSetThemePreferenceMethod.METHOD_NAME,
            )

        assertNotNull(methodProvider)
        assertEquals(AbsSetThemePreferenceMethod.METHOD_NAME, methodProvider?.provideMethod()?.name)
    }

    private class RecordingKitView(
        context: Context,
        containerId: String,
    ) : IKitView {
        override var hybridContext = HybridContext().apply { this.containerId = containerId }
        private val view = View(context)
        var lastGlobalPropsUpdate: Map<String, Any>? = null

        override fun realView(): View = view

        override fun load() = Unit

        override fun load(uri: String) = Unit

        override fun reload() = Unit

        override fun updateGlobalPropsByIncrement(data: Map<String, Any>) {
            lastGlobalPropsUpdate = data
        }

        override fun onShow() = Unit

        override fun onHide() = Unit

        override fun destroy(clearContext: Boolean) = Unit

        override fun hasDestroyed(): Boolean = false

        override fun getGlobalProps(): MutableMap<String, Any>? = null

        override fun getScheme(): String? = null

        override fun onLoadSuccess() = Unit
    }

    private class ResultRecorder : CompletionBlock<AbsSetThemePreferenceMethod.ResultModel> {
        var successResult: AbsSetThemePreferenceMethod.ResultModel? = null
        var failureCode: Int? = null

        override fun onSuccess(
            result: AbsSetThemePreferenceMethod.ResultModel,
            msg: String,
        ) {
            successResult = result
        }

        override fun onFailure(
            code: Int,
            msg: String,
            data: AbsSetThemePreferenceMethod.ResultModel?,
        ) {
            failureCode = code
        }

        override fun onRawSuccess(data: AbsSetThemePreferenceMethod.ResultModel?) = Unit
    }
}
