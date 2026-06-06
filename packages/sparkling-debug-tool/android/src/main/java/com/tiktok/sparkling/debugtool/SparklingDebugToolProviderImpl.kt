// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.lynx.tasm.LynxView
import com.tiktok.sparkling.debug.SparklingDebugToolProvider
import com.tiktok.sparkling.debugtool.console.LynxConsoleSink
import com.tiktok.sparkling.debugtool.inspect.GlobalPropsRegistry
import com.tiktok.sparkling.debugtool.inspect.GlobalPropsSnapshot
import com.tiktok.sparkling.debugtool.inspect.MethodInvocation
import com.tiktok.sparkling.debugtool.inspect.MethodInvocationStore
import com.tiktok.sparkling.debugtool.inspect.SparklingDebugAutoWiring
import com.tiktok.sparkling.hybridkit.base.IKitView
import com.tiktok.sparkling.hybridkit.config.RuntimeInfo
import com.tiktok.sparkling.method.registry.api.SparklingMethodInvocationCenter

class SparklingDebugToolProviderImpl : SparklingDebugToolProvider {
    override fun openInspectorPanel(activity: FragmentActivity) {
        SparklingDebugTool.openInspectorPanel(activity)
    }

    override fun onKitViewCreated(
        containerId: String,
        kitView: IKitView,
    ) {
        SparklingDebugAutoWiring.attachConsoleWithRetry(kitView.realView())
        GlobalPropsRegistry.notifyChanged()
    }

    override fun onKitViewDestroyed(
        containerId: String,
        kitView: IKitView?,
    ) {
        (kitView?.realView() as? LynxView)?.let { LynxConsoleSink.detach(it) }
        GlobalPropsRegistry.notifyChanged()
    }

    override fun onMethodInvocationStart(event: SparklingMethodInvocationCenter.Event) {
        MethodInvocationStore.add(event.toMethodInvocation(isEnd = false))
    }

    override fun onMethodInvocationEnd(event: SparklingMethodInvocationCenter.Event) {
        MethodInvocationStore.update(event.toMethodInvocation(isEnd = true))
    }

    override fun setGlobalPropsCollector(collector: () -> Map<String, IKitView>) {
        GlobalPropsRegistry.setProvider {
            collector().map { (containerId, kitView) ->
                val raw = kitView.getGlobalProps().orEmpty()
                val templateUrl = kitView.getScheme()

                @Suppress("UNCHECKED_CAST")
                val queryItems =
                    (raw[RuntimeInfo.QUERY_ITEMS] as? Map<String, Any?>)
                        ?.takeIf { it.isNotEmpty() }
                        ?: parseQueryItems(templateUrl)
                GlobalPropsSnapshot(
                    containerId = containerId,
                    templateUrl = templateUrl,
                    globalProps = raw.filterKeys { it != RuntimeInfo.QUERY_ITEMS },
                    queryItems = queryItems,
                )
            }
        }
    }

    private fun SparklingMethodInvocationCenter.Event.toMethodInvocation(isEnd: Boolean): MethodInvocation {
        val code = code
        return MethodInvocation(
            id = id,
            name = name,
            namespace = namespace,
            platform = platform.toString(),
            params = MethodInvocation.formatPayload(params),
            result = if (isEnd) MethodInvocation.formatPayload(result) else null,
            code = code,
            success = if (isEnd) code == null || code == 1 else null,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
        )
    }

    private fun parseQueryItems(url: String?): Map<String, Any?> {
        if (url.isNullOrBlank()) return emptyMap()
        return runCatching {
            val uri = Uri.parse(url)
            uri.queryParameterNames.associateWith { uri.getQueryParameter(it) }
        }.getOrDefault(emptyMap())
    }
}
