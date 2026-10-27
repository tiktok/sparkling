// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.api

/**
 * Controls which bridge callbacks may be skipped, by switch and page url.
 */
class CancelCallbackConfig {
    var enable: Boolean = false
    var cancelCallbackUrlList: ArrayList<String>? = null
}
