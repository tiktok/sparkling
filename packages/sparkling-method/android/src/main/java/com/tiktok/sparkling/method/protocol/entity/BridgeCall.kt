// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.entity

import com.tiktok.sparkling.method.protocol.BridgeContext
import com.lynx.react.bridge.JavaOnlyMap
import com.tiktok.sparkling.method.registry.core.model.context.BridgeCallThreadTypeEnum
import org.json.JSONObject

/**
 * Base of a single bridge call from the front end: what was invoked, with which payload, and the
 * protocol state the minimum runtime accumulates while handling it. A host that needs more per-call
 * state, such as auth or telemetry, extends this class.
 */
open class BridgeCall(
    open val context: BridgeContext
) {

    companion object {
        const val DEFAULT_NAMESPACE = "DEFAULT"
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
    lateinit var platform: IPlatform

    // when bridgeCallThreadType == null, it means bridgeCallThreadType no one set, so just use the default thread.
    // the default thread in jsbsdk will be same as the main thread
    var bridgeCallThreadType: BridgeCallThreadTypeEnum? = null

    var cancelCallBack: CancelCallbackType = CancelCallbackType.NONE

    //for lifeClient
    internal var callBeginTime: Long = 0
    internal var invocation: String = "" //same as rawReq
    internal var beginCreateCallBaskMsgTime: Long = 0
    internal var endCreateCallBaskMsgTime: Long = 0
    internal var callbackMsg: String? = null
    internal var lynxCallbackMap: JavaOnlyMap? = null
    internal var isInMainThread: Boolean = true
    internal var jsbEngine: String = ""
    //for call to get invocation's jsonObject
    internal var invocationJson: JSONObject? = null

    override fun toString(): String {
        return "BridgeCall(callbackId='$callbackId', bridgeName='$bridgeName', hitBusinessHandler='$hitBusinessHandler', url='$url', msgType='$msgType', params='$params', sdkVersion=$sdkVersion, nameSpace='$nameSpace', frameUrl='$frameUrl')"
    }
}

enum class CancelCallbackType {
    NONE, ALL, ONLY_SUCCESS
}
