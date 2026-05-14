// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.impl.monitor

import android.util.Log
import org.json.JSONObject

interface IBridgeMonitor {
    fun onBridgeRejected(entity: MonitorEntity)

    fun onBridgeResolved(entity: MonitorEntity)

    fun onBridgeEvent(
        eventName: String,
        data: JSONObject?,
    ) {
        Log.d("IBridgeMonitor", "eventName = $eventName, data = $data")
    }
}
