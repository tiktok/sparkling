// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.router.utils

import android.content.Context
import com.tiktok.sparkling.method.registry.core.IBridgeContext
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.model.context.ContextProviderFactory

interface IHostRouterDepend {
    fun openScheme(
        bridgeContext: IBridgeContext?,
        scheme: String,
        extraParams: Map<String, Any>,
        platformType: BridgePlatformType,
        context: Context?,
    ): Boolean {
        var handled = false
        val contextProviderFactory = ContextProviderFactory()
        val headHandlerNode = assembleHandlerChain(contextProviderFactory) ?: return false
        var curHandlerNode: AbsRouteOpenHandler? = headHandlerNode
        while (!handled && curHandlerNode != null) {
            if (curHandlerNode.getSupportPlatformTypeList().contains(BridgePlatformType.ALL) || curHandlerNode.getSupportPlatformTypeList().contains(platformType)) {
                try {
                    handled = curHandlerNode.openScheme(scheme, extraParams, contextProviderFactory?.provideInstance(Context::class.java))
                    if (handled) {
                        break
                    }
                    curHandlerNode = curHandlerNode.nextHandler
                } catch (e: Throwable) {
                    curHandlerNode = curHandlerNode!!.exceptionHandler
                }
            } else {
                curHandlerNode = curHandlerNode.nextHandler
            }
        }

        return handled
    }

    fun closeView(
        bridgeContext: IBridgeContext?,
        type: BridgePlatformType,
        containerID: String? = null,
        animated: Boolean? = false,
    ): Boolean

    fun provideRouteOpenHandlerList(contextProviderFactory: ContextProviderFactory?): List<AbsRouteOpenHandler> = listOf()

    fun provideRouteOpenExceptionHandler(contextProviderFactory: ContextProviderFactory?): AbsRouteOpenHandler? = null

    private fun assembleHandlerChain(contextProviderFactory: ContextProviderFactory?): AbsRouteOpenHandler? {
        val chainHandlerList = provideRouteOpenHandlerList(contextProviderFactory)
        val exceptionHandlerNode = provideRouteOpenExceptionHandler(contextProviderFactory)
        var prevChainNode: AbsRouteOpenHandler? = null
        var headChainNode: AbsRouteOpenHandler? = null
        chainHandlerList.forEach {
            val curChainHandler = it
            if (prevChainNode == null) {
                headChainNode = curChainHandler
            }
            prevChainNode?.setNextHandler(curChainHandler)
            curChainHandler.setExceptionHandler(exceptionHandlerNode)
            prevChainNode = curChainHandler
        }
        return headChainNode
    }
}
