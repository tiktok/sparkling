// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.protocol.impl.lynx

import com.tiktok.sparkling.method.protocol.BridgeContext
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeProtocol
import com.tiktok.sparkling.method.protocol.utils.BridgeConverter
import com.lynx.react.bridge.JavaOnlyArray
import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import org.json.JSONObject

class LynxBridgeProtocol(
    val context: BridgeContext,
) : IBridgeProtocol(context) {
    override fun sendEvent(
        name: String,
        params: Any?,
    ) {
        if (params is JavaOnlyArray) {
            context.bridgeLifeClientImp.onBridgeEventStart(name, params)
            sendEvent(name, (params as? JavaOnlyArray))
            context.bridgeLifeClientImp.onBridgeEventEnd(name, params)
        } else if (params is JSONObject?) {
            var array =
                JavaOnlyArray().apply {
                    pushMap(
                        BridgeConverter.convertJSONObject2JavaOnlyMap(
                            JSONObject().apply {
                                put("data", params)
                                put("containerID", context.containerId)
                                put("protocolVersion", "1.0")
                                put("code", 1)
                            },
                        ),
                    )
                }
            context.bridgeLifeClientImp.onBridgeEventStart(name, array)
            sendEvent(name, array)
            context.bridgeLifeClientImp.onBridgeEventEnd(name, array)
        }
    }

    override fun sendJSRuntimeEvent(
        name: String,
        params: Any?,
    ) {
        if (params is JavaOnlyArray) {
            context.bridgeLifeClientImp.onBridgeEventStart(name, params)
            sendJSRuntimeEvent(name, (params as? JavaOnlyArray))
            context.bridgeLifeClientImp.onBridgeEventEnd(name, params)
        } else if (params is JSONObject?) {
            var array =
                JavaOnlyArray().apply {
                    pushMap(
                        BridgeConverter.convertJSONObject2JavaOnlyMap(
                            JSONObject().apply {
                                put("data", params)
                                put("containerID", context.containerId)
                                put("protocolVersion", "1.0")
                                put("code", 1)
                            },
                        ),
                    )
                }
            context.bridgeLifeClientImp.onBridgeEventStart(name, array)
            sendJSRuntimeEvent(name, array)
            context.bridgeLifeClientImp.onBridgeEventEnd(name, array)
        }
    }

    private fun sendJSRuntimeEvent(
        method: String,
        params: JavaOnlyArray?,
    ) {
        context.lynxBackgroundRuntime?.sendGlobalEvent(method, params)
    }

    private fun sendEvent(
        method: String,
        params: JavaOnlyArray?,
    ) {
        context.lynxView?.sendGlobalEvent(method, params)
    }

    override fun init() {}

    override fun createBridgeCall(msg: String): BridgeCall = BridgeCall(context)
}
