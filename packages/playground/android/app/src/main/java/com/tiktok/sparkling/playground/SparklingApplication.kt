// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.playground

import android.app.Application

import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.memory.PoolConfig
import com.facebook.imagepipeline.memory.PoolFactory
import com.lynx.tasm.behavior.Behavior
import com.lynx.tasm.behavior.LynxContext
import com.lynx.tasm.behavior.ui.LynxUI
import com.tiktok.sparkling.hybridkit.HybridKit
import com.tiktok.sparkling.hybridkit.config.BaseInfoConfig
import com.tiktok.sparkling.hybridkit.config.SparklingHybridConfig
import com.tiktok.sparkling.hybridkit.config.SparklingLynxConfig
import com.tiktok.sparkling.method.registry.core.SparklingBridgeManager
import com.tiktok.sparkling.method.router.close.RouterCloseMethod
import com.tiktok.sparkling.method.router.open.RouterOpenMethod
import com.tiktok.sparkling.method.router.utils.RouterProvider
import com.tiktok.sparkling.method.runtime.depend.CommonDependsProvider
import com.tiktok.sparkling.method.storage.getItem.StorageGetItemMethod
import com.tiktok.sparkling.method.storage.removeItem.StorageRemoveItemMethod
import com.tiktok.sparkling.method.storage.setItem.StorageSetItemMethod
import com.tiktok.sparkling.method.media.choosemedia.ChooseMediaMethod
import com.tiktok.sparkling.method.media.downloadfile.DownloadFileMethod
import com.tiktok.sparkling.method.media.savedataurl.SaveDataURLMethod
import com.tiktok.sparkling.method.media.uploadfile.UploadFileMethod
import com.tiktok.sparkling.method.media.uploadimage.UploadImageMethod
import com.tiktok.sparkling.method.media.utils.MediaProvider
import com.tiktok.sparkling.playground.depend.AppMediaDepend
import com.tiktok.sparkling.playground.input.LynxInputComponent
import com.tiktok.sparkling.playground.provider.BuiltinTemplateProvider
import com.tiktok.sparkling.debugtool.SparklingDebugTool
import com.tiktok.sparkling.playground.depend.AppNetworkDepend
import com.tiktok.sparkling.playground.depend.AppPermissionDepend
import com.tiktok.sparkling.playground.depend.AppThreadPoolDepend


class SparklingApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initFresco()
        initSparkling()
    }

    private fun initFresco() {
        val factory = PoolFactory(PoolConfig.newBuilder().build())
        val builder = ImagePipelineConfig.newBuilder(applicationContext).setPoolFactory(factory)
        Fresco.initialize(applicationContext, builder.build())
    }

    private fun initSparkling() {
        if (BuildConfig.DEBUG) {
            SparklingDebugTool.init(this)
        }
        initHybridKit()
        initDepends()
        initSparklingMethods()
    }


    private fun initHybridKit() {
        HybridKit.init(this)
        val baseInfoConfig = BaseInfoConfig(isDebug = BuildConfig.DEBUG)
        val lynxConfig = SparklingLynxConfig.build(this) {
            addBehaviors(listOf(
                object : Behavior("input", false) {
                    override fun createUI(context: LynxContext?): LynxUI<*>? {
                        return LynxInputComponent(context)
                    }
                }
            ))
            setTemplateProvider(BuiltinTemplateProvider(this@SparklingApplication))
        }
        val hybridConfig = SparklingHybridConfig.build(baseInfoConfig) {
            setLynxConfig(lynxConfig)
        }
        HybridKit.setHybridConfig(hybridConfig, this)
        HybridKit.initLynxKit()
    }

    private fun initSparklingMethods() {
        SparklingBridgeManager.registerIDLMethod(RouterOpenMethod::class.java)
        SparklingBridgeManager.registerIDLMethod(RouterCloseMethod::class.java)
        RouterProvider.hostRouterDepend = SparklingHostRouterDepend()

        SparklingBridgeManager.registerIDLMethod(StorageSetItemMethod::class.java)
        SparklingBridgeManager.registerIDLMethod(StorageGetItemMethod::class.java)
        SparklingBridgeManager.registerIDLMethod(StorageRemoveItemMethod::class.java)

        SparklingBridgeManager.registerIDLMethod(ChooseMediaMethod::class.java)
        SparklingBridgeManager.registerIDLMethod(DownloadFileMethod::class.java)
        SparklingBridgeManager.registerIDLMethod(SaveDataURLMethod::class.java)
        SparklingBridgeManager.registerIDLMethod(UploadFileMethod::class.java)
        SparklingBridgeManager.registerIDLMethod(UploadImageMethod::class.java)
        MediaProvider.hostMediaDepend = AppMediaDepend.register(this)
    }

    private fun initDepends() {
        CommonDependsProvider.hostNetworkDepend = AppNetworkDepend()
        CommonDependsProvider.hostPermissionDepend = AppPermissionDepend()
        CommonDependsProvider.hostThreadPoolExecutorDepend = AppThreadPoolDepend()
    }
}
