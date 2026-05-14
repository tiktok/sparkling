// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.hybridkit.base

import com.tiktok.sparkling.hybridkit.HybridEnvironment
import com.tiktok.sparkling.hybridkit.api.IDependencyIterator
import com.tiktok.sparkling.hybridkit.api.IDependencyProvider
import com.tiktok.sparkling.hybridkit.api.IInstanceProvider
import com.tiktok.sparkling.method.registry.core.interfaces.IReleasable
import java.util.concurrent.ConcurrentHashMap

class DefaultDependencyProvider : IDependencyProvider {
    private val providers = ConcurrentHashMap<Class<*>, IInstanceProvider<*>>()

    override fun <T> put(
        clazz: Class<T>,
        instance: T?,
    ) {
        if (instance != null) {
            if (instance is IDependencyIterator<*>) {
                if (providers[clazz] == null) {
                    providers[clazz] = DefaultInstanceProvider(instance)
                } else {
                    val lastInstance = getLastInstance(clazz)
                    if (lastInstance is IDependencyIterator<*>) {
                        (lastInstance as IDependencyIterator<T>).next(instance)
                    }
                }
            } else {
                providers[clazz] = DefaultInstanceProvider(instance)
            }
        } else {
            providers[clazz] = DefaultInstanceProvider(null)
        }
    }

    override fun <T> get(clazz: Class<T>): T? =
        providers[clazz]
            ?.run {
                provideInstance()
            }?.takeIf {
                clazz.isAssignableFrom(it::class.java)
            }?.let {
                it as T
            }

    override fun <T> remove(clazz: Class<T>) {
        providers.remove(clazz)
    }

    /**
     * get the last instance in the chain
     */
    private fun <T> getLastInstance(clazz: Class<T>): T? {
        var lastInstance = get(clazz)
        while (lastInstance is IDependencyIterator<*>) {
            if (lastInstance.next() == null) {
                return lastInstance
            }
            lastInstance = lastInstance.next() as T?
        }
        return lastInstance
    }

    override fun release() {
        val iterator = providers.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next().value
            val value = next.provideInstance()
            if (value is IReleasable) {
                value.release()
            } else if (HybridEnvironment.instance.shouldKeepDependencyWhenRecreate(value)) {
                // do nothing
            } else {
                iterator.remove()
            }
        }
    }

    override fun cloneDependency(): IDependencyProvider {
        val dependencyProvider = DefaultDependencyProvider()
        val iterator = providers.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            val key = next.key
            val value = next.value
            val instance = value.provideInstance()
            dependencyProvider.providers[key] = DefaultInstanceProvider(instance)
        }
        return dependencyProvider
    }
}

class DefaultInstanceProvider<out T>(
    val instance: T,
) : IInstanceProvider<T> {
    override fun provideInstance() = instance
}
