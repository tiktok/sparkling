// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.hybridkit.lynx

import com.lynx.jsbridge.LynxModule

class SparklingLynxModuleWrapper(
    var clz: Class<out LynxModule>? = null,
    var moduleParams: Any? = null,
)
