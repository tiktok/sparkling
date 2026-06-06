// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debug

import androidx.fragment.app.FragmentActivity
import com.tiktok.sparkling.hybridkit.KitViewManager
import com.tiktok.sparkling.hybridkit.base.IKitView
import com.tiktok.sparkling.method.registry.api.SparklingMethodInvocationCenter
import java.util.ServiceLoader
import java.util.concurrent.atomic.AtomicBoolean

object SparklingDebugToolRegistry {
    @Volatile
    private var testProviders: List<SparklingDebugToolProvider>? = null

    @Volatile
    private var serviceProviders: List<SparklingDebugToolProvider>? = null
    private val methodObserverInstalled = AtomicBoolean(false)

    fun hasDebugToolProvider(): Boolean = providers().isNotEmpty()

    fun openInspectorPanel(activity: FragmentActivity): Boolean {
        val provider = providers().firstOrNull() ?: return false
        provider.openInspectorPanel(activity)
        return true
    }

    internal fun setProvidersForTest(providers: List<SparklingDebugToolProvider>?) {
        testProviders = providers
    }

    fun notifyKitViewCreated(
        containerId: String,
        kitView: IKitView,
    ) {
        providers().forEach { runCatching { it.onKitViewCreated(containerId, kitView) } }
    }

    fun notifyKitViewDestroyed(
        containerId: String,
        kitView: IKitView?,
    ) {
        providers().forEach { runCatching { it.onKitViewDestroyed(containerId, kitView) } }
    }

    fun notifyMethodStart(event: SparklingMethodInvocationCenter.Event) {
        providers().forEach { runCatching { it.onMethodInvocationStart(event) } }
    }

    fun notifyMethodEnd(event: SparklingMethodInvocationCenter.Event) {
        providers().forEach { runCatching { it.onMethodInvocationEnd(event) } }
    }

    private fun providers(): List<SparklingDebugToolProvider> {
        testProviders?.let {
            configureProviders(it)
            return it
        }
        serviceProviders?.let { return it }
        val loaded =
            runCatching {
                ServiceLoader
                    .load(SparklingDebugToolProvider::class.java)
                    .iterator()
                    .asSequence()
                    .toList()
            }.getOrDefault(emptyList())
        configureProviders(loaded)
        serviceProviders = loaded
        return loaded
    }

    private fun configureProviders(providers: List<SparklingDebugToolProvider>) {
        if (providers.isEmpty()) return
        providers.forEach {
            runCatching { it.setGlobalPropsCollector { KitViewManager.getKitViews() } }
        }
        installMethodObserverOnce()
    }

    private fun installMethodObserverOnce() {
        if (!methodObserverInstalled.compareAndSet(false, true)) return
        SparklingMethodInvocationCenter.addObserver(
            object : SparklingMethodInvocationCenter.Observer {
                override fun onStart(event: SparklingMethodInvocationCenter.Event) {
                    notifyMethodStart(event)
                }

                override fun onEnd(event: SparklingMethodInvocationCenter.Event) {
                    notifyMethodEnd(event)
                }
            },
        )
    }
}
