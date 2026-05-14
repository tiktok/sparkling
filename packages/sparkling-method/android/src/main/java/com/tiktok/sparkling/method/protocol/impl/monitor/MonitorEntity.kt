// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.impl.monitor

import org.json.JSONObject

class MonitorEntity {
    var name: String? = null
    var code: Int? = null
    var message: String? = null
    var url: String? = null
    var beginTime: Long? = null
    var endTime: Long? = null
    var rawResult: JSONObject? = null
    var rawRequest: JSONObject? = null
    var hitBusinessHandler: Boolean = false
    var nameSpace: String? = null
    var isRunInMainThread: Boolean = true
}
