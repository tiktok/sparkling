// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.utils

import java.util.concurrent.ConcurrentHashMap

class SafeConcurrentHashMap<K, V> : ConcurrentHashMap<K, V>() {
    override fun put(
        key: K & Any,
        value: V & Any,
    ): V? {
        runCatching {
            if (key == null || value == null) {
                return null
            }
            return super.put(key, value)
        }
        return null
    }

    override fun putIfAbsent(
        key: K & Any,
        value: V & Any,
    ): V? {
        runCatching {
            if (key == null || value == null) {
                return null
            }
            return super.putIfAbsent(key, value)
        }
        return null
    }

    override fun putAll(from: Map<out K, V>) {
        runCatching {
            val keys = from.filter { it.value == null }.keys.joinToString(",")
            super.putAll(
                from.filter {
                    it.key != null && it.value != null
                },
            )
        }
    }
}
