// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.entity

import com.lynx.react.bridge.JavaOnlyMap
import com.tiktok.sparkling.method.protocol.utils.BridgeConverter
import com.tiktok.sparkling.method.registry.api.SparklingBridge
import com.tiktok.sparkling.method.registry.api.Utils
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import org.json.JSONObject

/**
 * Base of what a bridge method hands back to the front end: the raw payload plus the shaping and
 * inspection the minimum runtime needs. A host may extend it, for example to mark results or to
 * convert map payloads its own way.
 */
open class BridgeResult(val parcel: Any) {

    override fun toString(): String {
        if (SparklingBridge.isDebugEnv) {
            return toJSONObject().toString()
        } else {
            return ""
        }
    }

    companion object {
        fun toJsonResult(code: Int, msg: String = "", data: JSONObject? = null): BridgeResult {
            return BridgeResult(newResultJson(code, msg, data))
        }

        /**
         * Shared with the factories of host subclasses, which have to return their own type and so
         * cannot reuse [toJsonResult]. Keeping the payload shape in one place stops the factories
         * from drifting apart.
         */
        internal fun newResultJson(code: Int, msg: String, data: JSONObject?): JSONObject {
            val json = JSONObject()
            json.apply {
                put("code", code)
                put("msg", msg)
                put("data", data)
            }
            return json
        }
    }

    fun toJSONObject(): JSONObject {
        if (parcel is JSONObject) return parcel
        if (parcel is JavaOnlyMap) {
            return BridgeConverter.revertJavaOnlyMap2JSONObject(parcel)
        }
        if (parcel is Map<*, *>) {
            return mapToJSON(parcel as Map<String, Any?>)
        }
        return JSONObject()
    }

    /**
     * Converts a map payload to JSON. A host subclass may override it to convert through its own
     * utilities.
     */
    protected open fun mapToJSON(map: Map<String, Any?>): JSONObject = Utils.mapToJSON(map)

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

    open fun toJSONObject(call: BridgeCall?): JSONObject {
        return if (parcel is JSONObject) {
            parcel
        } else if (parcel is Map<*, *>) {
            mapToJSON(parcel as Map<String, Any?>)
        } else if (parcel is JavaOnlyMap) {
            BridgeConverter.revertJavaOnlyMap2JSONObject(parcel)
        } else JSONObject()
    }
}
