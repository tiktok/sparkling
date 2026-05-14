// Copyright (c) 2023 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.impl.errors

import android.view.View
import com.tiktok.sparkling.method.protocol.utils.MonitorUtils
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.api.SparklingBridge
import org.json.JSONObject

/**
 */
class JSBErrorReportModel {
    companion object {
        const val DEFAULT_JSB_ERROR_BID = "jsb_sdk_error_bid"
        const val DEFAULT_JSB_ERROR_EVENT = "jsb_sdk_error_event"

        // global messages
        private val globalExtension: HashMap<String, Any> = HashMap<String, Any>()

        fun putGlobalExtension(
            key: String,
            value: Any?,
        ) {
            value?.let {
                globalExtension[key] = it
            }
        }

        fun putGlobalExtension(map: Map<String, Any>) {
            globalExtension.putAll(map)
        }
    }

    // instance messages
    private var jsbMethodName: String? = null
    private var jsbUrl: String? = null
    private var jsbErrorCode: Int? = null
    private var jsbBridgeSdk: String? = null
    private var view: View? = null
    private var containerID: String? = null
    private var jsbEngine: String? = null

    // only put new value
    private val jsbExtension: HashMap<String, Any> = HashMap<String, Any>()

    internal fun setJsbMethodName(jsbMethodName: String?) {
        this.jsbMethodName = jsbMethodName
    }

    internal fun setJsbEngine(jsbEngine: String) {
        this.jsbEngine = jsbEngine
    }

    internal fun setJsbUrl(jsbUrl: String?) {
        this.jsbUrl = jsbUrl
    }

    internal fun setJsbErrorCode(jsbErrorCode: Int?) {
        this.jsbErrorCode = jsbErrorCode
    }

    internal fun setJsbBridgeSdk(jsbBridgeSdk: String?) {
        this.jsbBridgeSdk = jsbBridgeSdk
    }

    internal fun setView(view: View?) {
        this.view = view
    }

    internal fun setContainerID(containerID: String?) {
        this.containerID = containerID
    }

    fun putJsbExtension(
        key: String,
        value: Any?,
    ) {
        value?.let { jsbExtension.put(key, it) }
    }

    fun putJsbExtension(map: Map<String, Any>) {
        jsbExtension.putAll(map)
    }

    internal fun reportJSBErrorModel(contextErrorModel: JSBErrorReportModel?) {
        if (!checkJSBErrorCodeIsRight()) {
            return
        }
        if (SparklingBridge.jsbErrorReportBlockList.contains(jsbMethodName)) {
            // report be block.
            return
        }
        realReportSDKErrorModel(contextErrorModel, DEFAULT_JSB_ERROR_EVENT, 0)
    }

    private fun realReportSDKErrorModel(
        contextErrorModel: JSBErrorReportModel?,
        eventName: String,
        simple: Int,
    ) {
        MonitorUtils.customReport(
            eventName,
            JSONObject().apply {
                put("jsb_method_name", jsbMethodName)
                put("jsb_url", jsbUrl)
                put("jsb_error_code", jsbErrorCode)
                put("jsb_bridge_sdk", jsbBridgeSdk)
                put("jsb_engine", jsbEngine)
                for ((key, value) in jsbExtension) {
                    put(key, value.toString())
                }
                if (jsbErrorCode == IDLBridgeMethod.UNREGISTERED) {
                    for ((key, value) in globalExtension) {
                        put(key, value.toString())
                    }
                }
                // add context message
                contextErrorModel?.jsbExtension?.let {
                    for ((key, value) in it) {
                        put(key, value.toString())
                    }
                }
            },
        )
    }

    private val allJSBSDKErrorCode =
        arrayOf(
            IDLBridgeMethod.UNREGISTERED, // UnregisteredMethod, the called method isn't registered, due to legacy error, sdk use BridgeConstants's BRIDGE_NOT_FOUND
            IDLBridgeMethod.INVALID_PARAM, // InvalidParameter, the parameter passed by FE is invalid
            IDLBridgeMethod.INVALID_RESULT, // InvalidResult, the result passed by the method implementor is invalid
            IDLBridgeMethod.UNKNOWN_ERROR,
            IDLBridgeMethod.PERMISSION_NO_EXIST, // internal code, due to legacy error, sdk use BridgeConstants's PERMISSION_NO_EXIST
            IDLBridgeMethod.ANNOTATION_ERROR, // when annotation can't get
            IDLBridgeMethod.ILLEGAL_OPERATION_ERROR, // when call a illegal method of proxy
            IDLBridgeMethod.BRIDGE_CALL_BE_INTERCEPTED, // bridge call be intercepted
            IDLBridgeMethod.BRIDGE_HAS_BEEN_RELEASED,
        )

    private fun checkJSBErrorCodeIsRight(): Boolean = allJSBSDKErrorCode.contains(jsbErrorCode)
}
