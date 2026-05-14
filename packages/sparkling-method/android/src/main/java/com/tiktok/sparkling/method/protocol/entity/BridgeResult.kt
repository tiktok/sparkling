// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.entity

import com.tiktok.sparkling.method.protocol.utils.BridgeConverter
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.api.SparklingBridge
import com.tiktok.sparkling.method.registry.api.Utils
import com.lynx.react.bridge.JavaOnlyMap
import org.json.JSONObject

class BridgeResult(
    val parcel: Any,
) {
    override fun toString(): String {
        if (SparklingBridge.isDebugEnv) {
            return toJSONObject().toString()
        } else {
            return ""
        }
    }

    companion object {
        fun toJsonResult(
            code: Int,
            msg: String = "",
            data: JSONObject? = null,
        ): BridgeResult {
            val json = JSONObject()
            json.apply {
                put("code", code)
                put("msg", msg)
                put("data", data)
            }
            return BridgeResult(json)
        }
    }

    fun toJSONObject(): JSONObject {
        if (parcel is JSONObject) return parcel
        if (parcel is JavaOnlyMap) {
            return BridgeConverter.revertJavaOnlyMap2JSONObject(parcel)
        }
        if (parcel is Map<*, *>) {
            return Utils.mapToJSON(parcel as Map<String, Any?>)
        }
        return JSONObject()
    }

    fun isSuccessResult(): Boolean {
        try {
            if (parcel is JSONObject) return parcel.optInt("code", IDLBridgeMethod.FAIL) == IDLBridgeMethod.SUCCESS
            if (parcel is Map<*, *>) return parcel.get("code") == IDLBridgeMethod.SUCCESS
            if (parcel is JavaOnlyMap) return parcel.getInt("code") == IDLBridgeMethod.SUCCESS
            return false
        } catch (e: Exception) {
            return false
        }
    }

    fun toJSONObject(call: BridgeCall?): JSONObject =
        (
            if (parcel is JSONObject) {
                parcel
            } else if (parcel is Map<*, *>) {
                Utils.mapToJSON(parcel as Map<String, Any?>)
            } else if (parcel is JavaOnlyMap) {
                BridgeConverter.revertJavaOnlyMap2JSONObject(parcel)
            } else {
                JSONObject()
            }
        )
}
