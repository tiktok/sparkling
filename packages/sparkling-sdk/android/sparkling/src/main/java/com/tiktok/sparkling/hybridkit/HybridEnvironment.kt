// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.hybridkit

import android.app.Application
import com.tiktok.sparkling.hybridkit.api.IDependencyIterator
import com.tiktok.sparkling.hybridkit.api.IDependencyProvider
import com.tiktok.sparkling.hybridkit.base.DefaultDependencyProvider
import com.tiktok.sparkling.hybridkit.config.BaseInfoConfig
import com.tiktok.sparkling.hybridkit.config.DebugConfig
import com.tiktok.sparkling.hybridkit.config.ILynxConfig
import com.tiktok.sparkling.hybridkit.config.IWebConfig

class HybridEnvironment {
    var isDebug: Boolean = false

    @Volatile
    lateinit var context: Application
    var dependencyProviders = HashMap<String, IDependencyProvider>()
    var baseInfoConfig: BaseInfoConfig? = null
    var lynxConfig: ILynxConfig? = null
    var debugConfig: DebugConfig? = null
    var webConfig: IWebConfig? = null
    val hybridConfigInfo = HybridConfigInfo()

    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            HybridEnvironment()
        }
        val hybridKitInitLock = Any()
    }

    private val keepDependencyWhenRecreateClasses = mutableListOf<Class<*>>()

    /**
     * mark class need to be kept when recreated
     */
    fun addKeepDependencyWhenRecreate(clazz: Class<*>) {
        if (!keepDependencyWhenRecreateClasses.contains(clazz)) {
            keepDependencyWhenRecreateClasses.add(clazz)
        }
    }

    /**
     * mark classes need to be kept when recreated
     */
    fun addKeepDependencyWhenRecreate(classes: List<Class<*>>) {
        classes.forEach {
            addKeepDependencyWhenRecreate(it)
        }
    }

    /**
     * check if class need to be kept when recreated
     */
    fun shouldKeepDependencyWhenRecreate(value: Any?): Boolean {
        keepDependencyWhenRecreateClasses.forEach {
            if (value?.javaClass?.let { clazz -> it.isAssignableFrom(clazz) } == true) {
                return true
            }
        }
        return false
    }

    fun <T> putDependency(
        containerId: String,
        clazz: Class<T>,
        instance: T?,
    ) {
        if (dependencyProviders[containerId] == null) {
            dependencyProviders[containerId] = DefaultDependencyProvider()
        }
        dependencyProviders[containerId]?.put(clazz, instance)
    }

    fun <T> getDependency(
        containerId: String,
        clazz: Class<T>,
    ): T? = dependencyProviders[containerId]?.get(clazz)

    fun <T> removeDependency(
        containerId: String,
        clazz: Class<T>,
    ) {
        val dependencyProvider = dependencyProviders[containerId]
        dependencyProvider?.remove(clazz)
    }

    fun <T> removeDependency(
        containerId: String,
        clazz: Class<T>,
        instance: T,
    ) {
        val dependencyProvider = dependencyProviders[containerId]
        dependencyProvider ?: return
        val headInstance = dependencyProvider.get(clazz)
        if (headInstance is IDependencyIterator<*> && instance is IDependencyIterator<*>) {
            if (headInstance == instance) {
                dependencyProvider.remove(clazz)
                dependencyProvider.put(clazz, headInstance.next() as? T)
            } else {
                var last = headInstance as? IDependencyIterator<T>
                var current = headInstance.next() as? IDependencyIterator<*>
                while (current != null) {
                    if (current == instance) {
                        last?.next(current.next() as? T)
                        break
                    }
                    last = current as? IDependencyIterator<T>
                    current = current.next() as? IDependencyIterator<*>
                }
            }
        } else {
            dependencyProvider.remove(clazz)
        }
    }

    fun removeDependency(
        containerId: String,
        clearContext: Boolean,
    ) {
        if (clearContext) {
            dependencyProviders.remove(containerId)
        } else {
            val dependencyProvider = dependencyProviders[containerId]
            dependencyProvider?.release()
        }
    }

    fun cloneDependency(
        oldContainerId: String,
        newContainerId: String,
    ) {
        dependencyProviders[newContainerId] = dependencyProviders[oldContainerId]?.cloneDependency() ?: DefaultDependencyProvider()
    }
}

class HybridConfigInfo {
    var cleanCacheTiming: CleanCacheTiming = CleanCacheTiming.WHEN_MATCH_VIEW
}

enum class CleanCacheTiming {
    WHEN_MATCH_VIEW,
    WHEN_FETCH,
}
