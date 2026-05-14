// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core.model.context

import com.tiktok.sparkling.method.registry.core.interfaces.IReleasable
import java.lang.ref.WeakReference

interface IContextProvider<out T> : IReleasable {
    fun provideInstance(): T?
}

class ContextHolder<out T>(
    t: T?,
) : IContextProvider<T> {
    private var ref: T? = t

    override fun provideInstance(): T? = ref

    override fun release() {
        ref = null
    }
}

class WeakContextHolder<out T>(
    t: T?,
) : IContextProvider<T> {
    private var ref =
        if (t == null) {
            null
        } else {
            WeakReference(t)
        }

    override fun provideInstance(): T? = ref?.get()

    override fun release() {
        ref?.clear()
        ref = null
    }
}

class WeakHostContextHolder<out T, out R>(
    host: T,
    private val provider: T.() -> R?,
) : IContextProvider<R> {
    private var ref: WeakReference<T>? = WeakReference(host)

    override fun provideInstance(): R? = ref?.get()?.run(provider)

    override fun release() {
        ref?.clear()
        ref = null
    }
}

class ContextProviderFactory {
    private val providers: MutableMap<Class<*>, IContextProvider<*>> = mutableMapOf()

    fun <T> has(clazz: Class<T>): Boolean = providers.containsKey(clazz)

    fun <T> registerProvider(
        clazz: Class<T>,
        provider: () -> T?,
    ) {
        providers[clazz]?.release()
        providers[clazz] =
            object :
                IContextProvider<T> {
                override fun provideInstance(): T? = provider()

                override fun release() {}
            }
    }

    fun <T> registerProvider(
        clazz: Class<T>,
        provider: IContextProvider<T>,
    ) {
        providers[clazz]
            ?.takeUnless {
                it === provider
            }?.apply {
                release()
            }
        providers[clazz] = provider
    }

    fun <T> registerWeakHolder(
        clazz: Class<T>,
        item: T?,
    ) {
        registerProvider(
            clazz = clazz,
            provider = WeakContextHolder(item),
        )
    }

    fun <T> registerHolder(
        clazz: Class<T>,
        item: T?,
    ) {
        registerProvider(
            clazz = clazz,
            provider = ContextHolder(item),
        )
    }

    fun <T> removeProvider(clazz: Class<T>) {
        providers[clazz]?.release()
        providers.remove(clazz)
    }

    fun removeAll() {
        providers.clear()
    }

    fun <T> getProvider(clazz: Class<T>): IContextProvider<T>? = providers[clazz]?.let { it as IContextProvider<T> }

    fun <T> provideInstance(clazz: Class<T>): T? {
        val ret =
            providers[clazz]
                ?.run {
                    provideInstance()
                }?.takeIf {
                    clazz.isAssignableFrom(it::class.java)
                }?.let {
                    it as T
                }
        return ret
    }

    fun keys(): Iterable<Class<*>> = providers.keys

    fun merge(other: ContextProviderFactory) {
        providers.putAll(other.providers)
    }

    fun copy(): ContextProviderFactory =
        ContextProviderFactory().also {
            it.merge(this)
        }
}
