// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.protocol.entity

import com.tiktok.sparkling.method.protocol.BridgeContext
import com.tiktok.sparkling.method.protocol.impl.monitor.BridgeSDKMonitor
import com.tiktok.sparkling.method.registry.core.model.context.BridgeCallThreadTypeEnum
import com.tiktok.sparkling.method.registry.api.BridgeSettings
import com.lynx.react.bridge.JavaOnlyMap
import com.tiktok.sparkling.method.protocol.impl.errors.JSBErrorReportModel
import com.tiktok.sparkling.method.protocol.impl.lifecycle.fe.FeCallMonitorModel
import org.json.JSONObject

class BridgeCall(
    val context: BridgeContext,
) {
    companion object {
        const val DEFAULT_NAMESPACE = "DEFAULT"
    }

    enum class PlatForm {
        Lynx,
        Web,
        Other,
    }

    var callbackId = ""
    var bridgeName = ""
    var url = ""
    var msgType = ""
    var params: Any? = null
    var sdkVersion = ""
    var nameSpace = ""
    var frameUrl = ""
    var timestamp: Long = -1
    var rawReq = ""
    var hitBusinessHandler: Boolean = false
    lateinit var platform: PlatForm

    // when bridgeCallThreadType == null, it means bridgeCallThreadType no one set, so just use the default thread.
    // the default thread in jsbsdk will be same as the main thread
    var bridgeCallThreadType: BridgeCallThreadTypeEnum? = null

    var cancelCallBack: CancelCallbackType = CancelCallbackType.NONE

    /**
     * force on instance sdk error
     */
    val jsbSDKErrorReportModel: JSBErrorReportModel = JSBErrorReportModel()

    // for lifeClient
    internal var callBeginTime: Long = 0

//    var protocolVersion: Int = ProtocolVersion.UNKNOWN
    internal var monitorBuilder: BridgeSDKMonitor.MonitorModel.Builder? = null
    internal var invocation: String = "" // same as rawReq
    internal var beginCreateCallBaskMsgTime: Long = 0
    internal var endCreateCallBaskMsgTime: Long = 0
    internal var callbackMsg: String? = null
    internal var lynxCallbackMap: JavaOnlyMap? = null
    internal var isInMainThread: Boolean = true
    internal var feCallMonitorModel = FeCallMonitorModel()
    internal var jsbEngine: String = ""

    // for call to get invocation's jsonObject
    internal var invocationJson: JSONObject? = null

    override fun toString(): String {
        if (BridgeSettings.bridgeCallToStringOptimization) {
            return "BridgeCall(callbackId='$callbackId', bridgeName='$bridgeName')"
        } else {
            return "BridgeCall(callbackId='$callbackId', bridgeName='$bridgeName', hitBusinessHandler='$hitBusinessHandler', url='$url', msgType='$msgType', params='$params', sdkVersion=$sdkVersion, nameSpace='$nameSpace', frameUrl='$frameUrl')"
        }
    }
}

enum class CancelCallbackType {
    NONE,
    ALL,
    ONLY_SUCCESS,
}
