// Copyright (c) 2024 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.impl.lifecycle.fe

import android.view.View
import com.tiktok.sparkling.method.protocol.utils.MonitorUtils
import org.json.JSONObject

/**
 */
class FeCallMonitorModel {
    companion object {
        const val _JSB_CALLER_INFO = "_jsb_caller_info"
        const val _JSB_PERF_METRICS = "_jsb_perf_metrics"
        const val JSB_FUNC_CALL_END = "jsb_func_call_end"
        const val JSB_FUNC_CALL_START = "jsb_func_call_start"
        const val JS_JSSDK = "js_jssdk"
        const val JS_CANCEL_CALLBACK = "js_cancel_callback"
        const val CALLER_PREFIX = "caller_"

        private const val JSB_FUNC_CALL = "jsb_func_call"
        private const val JSB_CALLBACK_CALL = "jsb_callback_call"
        private const val JSB_CALL = "jsb_call"
        private const val JSB_CLIENT_CALL = "jsb_client_call"
        private const val BRIDGE_METHOD_NAME = "bridge_method_name"
    }

    val JSB_FE_CALL_MONITOR_EVENT = "jsb_fe_call_monitor"
    var view: View? = null
    var bridgeName: String? = null
    var containerID: String? = null

    var jsbFuncCallStart: Long = 0
    var jsbFuncCallEnd: Long = 0
    var jsbCallbackStart: Long = 0
    var jsbCallbackEnd: Long = 0
    var jsbNativeCallStart: Long = 0

    private var category = JSONObject()

    fun addCategory(
        key: String,
        value: Any,
    ) {
        category.put(key, value)
    }

    fun reportFeCallInfo() {
        MonitorUtils.customReport(
            JSB_FE_CALL_MONITOR_EVENT,
            JSONObject().apply {
                put(BRIDGE_METHOD_NAME, bridgeName)

                if (jsbFuncCallStart == 0L) {
                    put(JSB_FUNC_CALL, -1)
                    put(JSB_CALL, -1)
                } else {
                    put(JSB_FUNC_CALL, jsbFuncCallEnd - jsbFuncCallStart)
                    put(JSB_CALL, jsbCallbackEnd - jsbFuncCallStart)
                }
                put(JSB_CALLBACK_CALL, jsbCallbackEnd - jsbCallbackStart)
                put(JSB_CLIENT_CALL, jsbCallbackEnd - jsbNativeCallStart)
            },
        )
    }
}
