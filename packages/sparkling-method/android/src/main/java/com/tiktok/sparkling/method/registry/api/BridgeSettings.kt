// Copyright (c) 2023 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.api

/**
 * when your value is just for the experiment, you can add it here.
 * and after your data has been recycled, you must delete it.
 */
object BridgeSettings {
    var bridgeRegistryOptimize: Boolean = false
    var bridgeDisableLongToDouble: Boolean = false
    var bridgeNewInputNumberTypeChange: Boolean = false
    var bridgeCallToStringOptimization: Boolean = false
}
