// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.api.util

import com.lynx.react.bridge.WritableArray
import com.lynx.react.bridge.WritableMap
import com.tiktok.sparkling.method.registry.core.XReadableArray
import com.tiktok.sparkling.method.registry.core.XReadableMap
import org.json.JSONArray
import org.json.JSONObject

/**
 * Conversion between the shapes a bridge parameter or result can arrive in: plain maps and lists,
 * `org.json` trees, the Lynx readable and writable containers, and the SDK's own `XReadable*`.
 *
 * Declared apart from its implementation so that a host which resolves it through a service
 * framework of its own has a contract to register against.
 */
interface IConvertUtils {
    fun toStringOrJson(data: Any?): String

    fun mapToJSON(map: Map<String, Any?>): JSONObject

    /** Keeps a `PiperData` value as it is rather than flattening it to a string. */
    fun mapSupportPiperdataToJSON(map: Map<String, Any?>): JSONObject

    /** Keeps a `PiperData` value as it is rather than flattening it to a string. */
    fun listSupportPiperdataToJSON(list: List<Any>): JSONArray

    fun listToJSON(list: List<Any>): JSONArray

    fun jsonToMap(json: JSONObject): Map<String, Any?>

    fun jsonToList(json: JSONArray): List<Any?>

    fun convertJsonToMap(jsonObject: JSONObject): WritableMap

    fun convertJsonToArray(jsonArray: JSONArray): WritableArray

    fun convertMapToReadableMap(source: Map<String, Any?>): WritableMap

    fun convertArrayToWritableArray(sourceArray: List<Any>): WritableArray

    fun convertXReadableMapToReadableMap(source: XReadableMap): WritableMap

    fun convertXReadableArrayToReadableArray(source: XReadableArray): WritableArray

    fun getValue(value: Any?): Any?
}
