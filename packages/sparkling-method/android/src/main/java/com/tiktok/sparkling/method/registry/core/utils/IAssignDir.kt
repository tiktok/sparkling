// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core.utils

/**
 * @since 2021/8/31
 * @desc
 */

interface IAssignDir<V> {
    fun getValue(): V

    object Creator {
        fun <T> create(obj: T): IAssignDir<T> =
            object : IAssignDir<T> {
                override fun getValue(): T = obj
            }
    }
}
