// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.playground

import android.app.Application

import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.memory.PoolConfig
import com.facebook.imagepipeline.memory.PoolFactory
import com.lynx.xelement.XElementBehaviors
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
import com.tiktok.sparkling.playground.provider.BuiltinTemplateProvider
import com.tiktok.sparkling.playground.depend.AppNetworkDepend
import com.tiktok.sparkling.playground.depend.AppPermissionDepend
import com.tiktok.sparkling.playground.depend.AppThreadPoolDepend

class SparklingApplication : Application() {
    private val debugHooks by lazy { createPlaygroundDebugHooks() }

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
        debugHooks.onApplicationCreate(this)
        initHybridKit()
        initDepends()
        initSparklingMethods()
    }

    private fun initHybridKit() {
        HybridKit.init(this)
        val baseInfoConfig = BaseInfoConfig(isDebug = BuildConfig.DEBUG)
        val lynxConfig =
            SparklingLynxConfig.build(this) {
                addBehaviors(XElementBehaviors().create())
                setTemplateProvider(BuiltinTemplateProvider(this@SparklingApplication))
            }
        val hybridConfig =
            SparklingHybridConfig.build(baseInfoConfig) {
                setLynxConfig(lynxConfig)
                debugHooks.configureHybridConfig(this)
            }
        HybridKit.setHybridConfig(hybridConfig, this)
        HybridKit.initLynxKit()
    }

    private fun initSparklingMethods() {
        SparklingBridgeManager.registerIDLMethod("router.open", clazz = RouterOpenMethod::class.java) { RouterOpenMethod() }
        SparklingBridgeManager.registerIDLMethod("router.close", clazz = RouterCloseMethod::class.java) { RouterCloseMethod() }
        RouterProvider.hostRouterDepend = SparklingHostRouterDepend()

        SparklingBridgeManager.registerIDLMethod("storage.setItem", clazz = StorageSetItemMethod::class.java) { StorageSetItemMethod() }
        SparklingBridgeManager.registerIDLMethod("storage.getItem", clazz = StorageGetItemMethod::class.java) { StorageGetItemMethod() }
        SparklingBridgeManager.registerIDLMethod("storage.removeItem", clazz = StorageRemoveItemMethod::class.java) { StorageRemoveItemMethod() }

        SparklingBridgeManager.registerIDLMethod("media.chooseMedia", clazz = ChooseMediaMethod::class.java) { ChooseMediaMethod() }
        SparklingBridgeManager.registerIDLMethod("media.downloadFile", clazz = DownloadFileMethod::class.java) { DownloadFileMethod() }
        SparklingBridgeManager.registerIDLMethod("media.saveDataURL", clazz = SaveDataURLMethod::class.java) { SaveDataURLMethod() }
        SparklingBridgeManager.registerIDLMethod("media.uploadFile", clazz = UploadFileMethod::class.java) { UploadFileMethod() }
        SparklingBridgeManager.registerIDLMethod("media.uploadImage", clazz = UploadImageMethod::class.java) { UploadImageMethod() }
        MediaProvider.hostMediaDepend = AppMediaDepend.register(this)
    }

    private fun initDepends() {
        CommonDependsProvider.hostNetworkDepend = AppNetworkDepend()
        CommonDependsProvider.hostPermissionDepend = AppPermissionDepend()
        CommonDependsProvider.hostThreadPoolExecutorDepend = AppThreadPoolDepend()
    }
}
