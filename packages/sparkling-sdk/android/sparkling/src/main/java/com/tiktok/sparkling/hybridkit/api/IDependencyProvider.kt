// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.hybridkit.api

/**
 * Basic information about the context of the service, including basic dependencies and complex dependencies.
 */

interface IDependencyProvider {
    fun <T> put(
        clazz: Class<T>,
        instance: T?,
    )

    fun <T> get(clazz: Class<T>): T?

    fun <T> remove(clazz: Class<T>)

    fun release()

    fun cloneDependency(): IDependencyProvider
}

interface IInstanceProvider<out T> {
    fun provideInstance(): T
}
