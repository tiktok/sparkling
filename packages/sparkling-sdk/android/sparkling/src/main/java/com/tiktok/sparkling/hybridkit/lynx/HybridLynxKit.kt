// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.lynx

import android.app.Application
import android.content.Context
import androidx.core.net.toUri
import com.lynx.devtool.LynxDevtoolEnv
import com.lynx.service.devtool.LynxDevToolService
import com.lynx.service.http.LynxHttpService
import com.lynx.service.image.LynxImageService
import com.lynx.service.log.LynxLogService
import com.lynx.tasm.INativeLibraryLoader
import com.lynx.tasm.LynxEnv
import com.lynx.tasm.LynxViewBuilder
import com.lynx.tasm.behavior.Behavior
import com.lynx.tasm.behavior.BehaviorBundle
import com.lynx.tasm.service.LynxServiceCenter
import com.tiktok.sparkling.hybridkit.HybridCommon
import com.tiktok.sparkling.hybridkit.HybridContext
import com.tiktok.sparkling.hybridkit.base.IHybridKitLifeCycle
import com.tiktok.sparkling.hybridkit.config.SparklingLynxConfig
import com.tiktok.sparkling.hybridkit.scheme.HybridSchemeParam
import com.tiktok.sparkling.hybridkit.utils.GlobalPropsUtils
import com.tiktok.sparkling.hybridkit.utils.LogLevel
import com.tiktok.sparkling.hybridkit.utils.LogUtils
import com.tiktok.sparkling.method.registry.api.SparklingBridge
import com.tiktok.sparkling.method.registry.api.util.BridgeProtocolConstants


object HybridLynxKit {
    private const val TAG = "HybridLynxKit"

    fun init(application: Application?) {
        LynxServiceCenter.inst().registerService(LynxImageService.getInstance())
        LynxServiceCenter.inst().registerService(LynxHttpService)
        if (HybridCommon.hybridConfig?.baseInfoConfig?.isDebug == true) {
            LynxServiceCenter.inst().registerService(LynxLogService)
            LynxServiceCenter.inst().registerService(LynxDevToolService.INSTANCE)
            LynxEnv.inst().enableLynxDebug(true)
            LynxEnv.inst().enableLogBox(true)
            LynxEnv.inst().enableDevtool(true)
            LynxDevtoolEnv.inst().enableLongPressMenu(true)
        } else {
            LynxEnv.inst().enableLynxDebug(false)
            LynxEnv.inst().enableLogBox(false)
            LynxEnv.inst().enableDevtool(false)
        }

        val lynxConfig = HybridCommon.hybridConfig?.lynxConfig as SparklingLynxConfig?
        val libraryLoader = lynxConfig?.libraryLoader
            ?: INativeLibraryLoader {
                try {
                    // load by default
                    System.loadLibrary(it)
                } catch (e: Throwable) {
                    e.message?.let { message ->
                        LogUtils.printLog(
                            message, LogLevel.E,
                            TAG
                        )
                    }
                }
            }

        val behaviorBundle = BehaviorBundle {
            ArrayList<Behavior>().apply {
                lynxConfig?.globalBehaviors?.let { addAll(it) }
            }
        }

        LynxEnv.inst().isCheckPropsSetter = lynxConfig?.isCheckPropsSetter ?: true

        LynxEnv.inst().init(
            application,
            libraryLoader,
            lynxConfig?.templateProvider,
            behaviorBundle
        )

        // register global LynxModule
        lynxConfig?.globalModules?.entries?.forEach {
            LynxEnv.inst().registerModule(it.key, it.value.clz, it.value.moduleParams)
        }

        lynxConfig?.additionInit?.invoke(LynxEnv.inst())
    }


    fun createKitView(
        scheme: HybridSchemeParam,
        hybridContext: HybridContext,
        context: Context,
        lifeCycle: IHybridKitLifeCycle? = null
    ): SimpleLynxKitView {
        val createStart = System.currentTimeMillis()
        hybridContext.tryResetTemplateResData(createStart)

        lifeCycle?.onPreKitCreate()

        GlobalPropsUtils.instance.init(hybridContext, context)

        var kitInitParams: LynxKitInitParams = (hybridContext.hybridParams as? LynxKitInitParams)
            ?: LynxKitInitParams(loadUri = null)
        if (kitInitParams.loadUri == null) {
            kitInitParams.loadUri = hybridContext.getFullScheme()?.toUri()
        }

        val viewBuilder = LynxViewBuilder()
        val bridge = SparklingBridge()
        bridge.registerLynxModule(viewBuilder, hybridContext.containerId)
        val lynxView =
            SimpleLynxKitView(context, hybridContext, viewBuilder, kitInitParams, lifeCycle)
        bridge.init(
            lynxView,
            hybridContext.containerId,
            BridgeProtocolConstants.BRIDGE_LYNX_PROTOCOL
        )
        hybridContext.bridge = bridge

        lifeCycle?.onPostKitCreated(lynxView)
        return lynxView
    }


}
