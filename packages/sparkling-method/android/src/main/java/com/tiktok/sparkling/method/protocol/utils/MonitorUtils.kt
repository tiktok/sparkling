// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.utils

import org.json.JSONObject

/**
 *
 */
class MonitorUtils {
    companion object {
        fun customReport(
            eventName: String,
            eventInfo: JSONObject?,
        ) {
            customReport(eventName, null, eventInfo)
        }

        fun customReport(
            eventName: String,
            category: JSONObject?,
            metrics: JSONObject?,
        ) {
            customReport(eventName, category, metrics, null)
        }

        fun customReport(
            eventName: String,
            category: JSONObject?,
            metrics: JSONObject?,
            extraInfo: String?,
        ) {
            // TODO: implement by injection
        }

        fun lifecycleReportPV(params: HashMap<String, Any>) {
            // TODO: implement by injection
        }

        fun lifecycleReportPerf(params: HashMap<String, Any>) {
            // TODO: implement by injection
        }
    }
}
