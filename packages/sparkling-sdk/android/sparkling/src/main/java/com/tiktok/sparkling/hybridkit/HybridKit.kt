// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit

import android.app.Activity
import android.app.Application
import android.content.Context
import com.tiktok.sparkling.hybridkit.base.HybridKitType
import com.tiktok.sparkling.hybridkit.base.IHybridKitLifeCycle
import com.tiktok.sparkling.hybridkit.base.IKitView
import com.tiktok.sparkling.hybridkit.config.SparklingHybridConfig
import com.tiktok.sparkling.hybridkit.lynx.HybridLynxKit
import com.tiktok.sparkling.hybridkit.scheme.HybridSchemeParam
import com.tiktok.sparkling.hybridkit.service.HybridActivityStackManager
import com.tiktok.sparkling.method.runtime.depend.BridgeBaseRuntime

object HybridKit {
    private const val TAG = "HybridKit"
    var application: Application? = null

    fun init(application: Application) {
        HybridCommon.init(application)
        HybridActivityStackManager.init(application)
        this.application = application
        BridgeBaseRuntime.applicationContext = application.applicationContext
    }

    /**
     * must be called before initLynxKit
     * @param hybridConfig cannot be null
     */
    fun setHybridConfig(
        hybridConfig: SparklingHybridConfig,
        application: Application,
    ) {
        HybridCommon.setHybridConfig(hybridConfig, application)
    }

    @JvmOverloads
    fun initLynxKit() {
        HybridLynxKit.init(application)
    }

    fun getTopActivity(): Activity? = HybridActivityStackManager.getTopActivity()

    fun isBackground(): Boolean = HybridActivityStackManager.isBackground()

    fun createKitView(
        scheme: HybridSchemeParam,
        param: HybridContext,
        context: Context,
        lifeCycle: IHybridKitLifeCycle? = null,
    ): IKitView? {
        if (scheme.engineType == HybridKitType.LYNX) {
            return HybridLynxKit.createKitView(scheme, param, context, lifeCycle)
        }
        // other type not implemented yet
        return null
    }
}
