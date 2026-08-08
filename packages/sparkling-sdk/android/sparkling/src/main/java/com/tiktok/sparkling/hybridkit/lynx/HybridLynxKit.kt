// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.lynx

import android.app.Application
import android.content.Context
import androidx.core.net.toUri
import com.lynx.service.http.LynxHttpService
import com.lynx.service.image.LynxImageService
import com.lynx.tasm.INativeLibraryLoader
import com.lynx.tasm.LynxEnv
import com.lynx.tasm.LynxViewBuilder
import com.lynx.tasm.behavior.Behavior
import com.lynx.tasm.behavior.BehaviorBundle
import com.lynx.tasm.service.LynxServiceCenter
import com.tiktok.sparkling.SparklingContext
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

        val lynxConfig = HybridCommon.hybridConfig?.lynxConfig as SparklingLynxConfig?
        val libraryLoader =
            lynxConfig?.libraryLoader
                ?: INativeLibraryLoader {
                    try {
                        // load by default
                        System.loadLibrary(it)
                    } catch (e: Throwable) {
                        e.message?.let { message ->
                            LogUtils.printLog(
                                message,
                                LogLevel.E,
                                TAG,
                            )
                        }
                    }
                }

        val behaviorBundle =
            BehaviorBundle {
                ArrayList<Behavior>().apply {
                    lynxConfig?.globalBehaviors?.let { addAll(it) }
                }
            }

        LynxEnv.inst().isCheckPropsSetter = lynxConfig?.isCheckPropsSetter ?: true

        LynxEnv.inst().init(
            application,
            libraryLoader,
            lynxConfig?.templateProvider,
            behaviorBundle,
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
        lifeCycle: IHybridKitLifeCycle? = null,
    ): SimpleLynxKitView {
        val createStart = System.currentTimeMillis()
        hybridContext.tryResetTemplateResData(createStart)

        lifeCycle?.onPreKitCreate()

        GlobalPropsUtils.instance.init(hybridContext, context)
        val lynxConfig = HybridCommon.hybridConfig?.lynxConfig as SparklingLynxConfig?

        var kitInitParams: LynxKitInitParams =
            (hybridContext.hybridParams as? LynxKitInitParams)
                ?: LynxKitInitParams(loadUri = null)
        if (kitInitParams.loadUri == null) {
            kitInitParams.loadUri = hybridContext.resolveFullScheme()?.toUri()
        }

        val viewBuilder = LynxViewBuilder()
        var lynxViewRef: SimpleLynxKitView? = null
        (lynxConfig?.templateProvider ?: LynxEnv.inst().templateProvider)?.let { templateProvider ->
            viewBuilder.setTemplateProvider(
                SimpleLynxTemplateProvider(templateProvider, lifeCycle) {
                    lynxViewRef
                },
            )
        }
        val bridge = SparklingBridge()
        bridge.registerLynxModule(viewBuilder, hybridContext.containerId)
        val sparklingContext = hybridContext as? SparklingContext
        val lynxView =
            SimpleLynxKitView(context, hybridContext, viewBuilder, kitInitParams, lifeCycle)
        lynxViewRef = lynxView
        bridge.init(
            lynxView,
            hybridContext.containerId,
            BridgeProtocolConstants.BRIDGE_LYNX_PROTOCOL,
        )
        hybridContext.bridge = bridge
        sparklingContext?.lynxViewCreatedListener?.onCreated(lynxView)

        lifeCycle?.onPostKitCreated(lynxView)
        return lynxView
    }
}
