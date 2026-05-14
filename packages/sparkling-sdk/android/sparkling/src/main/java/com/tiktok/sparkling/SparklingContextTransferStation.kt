// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

object SparklingContextTransferStation {
    private val sparklingContextMap = mutableMapOf<String, SparklingContext>()

    fun saveSparklingContext(context: SparklingContext) {
        sparklingContextMap[context.containerId] = context
    }

    fun getSparklingContext(containerId: String?): SparklingContext? = sparklingContextMap[containerId]

    fun releaseSparklingContext(containerId: String?) {
        sparklingContextMap.remove(containerId)
    }

    @JvmStatic
    internal fun clearAllContexts() {
        sparklingContextMap.clear()
    }
}
