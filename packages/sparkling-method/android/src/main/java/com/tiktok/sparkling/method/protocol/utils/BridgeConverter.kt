// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.utils

import com.tiktok.sparkling.method.registry.api.stringify
import com.lynx.react.bridge.JavaOnlyArray
import com.lynx.react.bridge.JavaOnlyMap
import com.lynx.react.bridge.PiperData
import com.lynx.react.bridge.ReadableType
import org.json.JSONArray
import org.json.JSONObject

object BridgeConverter {
    private const val TAG = "BridgeConverter"

    fun revertJavaOnlyMap2JSONObject(map: JavaOnlyMap?): JSONObject {
        val obj = JSONObject()
        if (map.isNullOrEmpty()) {
            return obj
        }
        val keys = map.keySetIterator()
        while (keys.hasNextKey()) {
            val nextKey = keys.nextKey()
            val nextValue = map[nextKey]
            try {
                when (map.getType(nextKey)) {
                    ReadableType.Array -> {
                        obj.putOpt(nextKey, revertJavaOnlyArray2JSONArray(nextValue as JavaOnlyArray))
                    }

                    ReadableType.Map -> {
                        obj.putOpt(nextKey, revertJavaOnlyMap2JSONObject(nextValue as JavaOnlyMap))
                    }

                    ReadableType.Number -> {
                        val num = nextValue as Number
                        obj.put(nextKey, getNumber(num))
                    }

                    ReadableType.PiperData -> {
                        val jsonObject = (nextValue as PiperData).stringify()
                        obj.put(nextKey, jsonObject)
                    }

                    else -> {
                        obj.putOpt(nextKey, nextValue)
                    }
                }
            } catch (ex: Throwable) {
                LogUtils.e(TAG, "revertJavaOnlyMap2JSONObject $ex")
            }
        }
        return obj
    }

    private fun revertJavaOnlyArray2JSONArray(array: JavaOnlyArray): JSONArray {
        val result = JSONArray()
        for (i in 0 until array.size) {
            val value = array[i]
            try {
                when (array.getType(i)) {
                    ReadableType.Map -> {
                        result.put(revertJavaOnlyMap2JSONObject(value as JavaOnlyMap))
                    }

                    ReadableType.Array -> {
                        result.put(revertJavaOnlyArray2JSONArray(value as JavaOnlyArray))
                    }

                    ReadableType.Number -> {
                        val num = value as Number
                        result.put(getNumber(num))
                    }

                    ReadableType.PiperData -> {
                        val jsonObject = (value as PiperData).stringify()
                        result.put(jsonObject)
                    }

                    else -> {
                        result.put(value)
                    }
                }
            } catch (ex: Throwable) {
                LogUtils.e(TAG, "revertJavaOnlyArray2JSONArray $ex")
            }
        }
        return result
    }

    private fun getNumber(rawNumber: Number): Number {
        val intValue: Int = runCatching { rawNumber.toInt() }.getOrNull() ?: 0
        val doubleValue: Double = runCatching { rawNumber.toDouble() }.getOrNull() ?: 0.0
        if (intValue.compareTo(doubleValue) == 0) {
            return intValue
        } else {
            val longValue: Long = doubleValue.toLong()
            if (doubleValue.compareTo(longValue) == 0) {
                return longValue
            } else {
                return doubleValue
            }
        }
    }

    fun convertJSONObject2JavaOnlyMap(obj: JSONObject): JavaOnlyMap {
        val keys = obj.keys()
        val result = JavaOnlyMap()
        while (keys.hasNext()) {
            val key = keys.next()
            when (val sonValue = obj.opt(key)) {
                is JSONObject -> {
                    result[key] = convertJSONObject2JavaOnlyMap(sonValue)
                }

                is JSONArray -> {
                    result[key] = convertJSONArray2JavaOnlyArray(sonValue)
                }

                else -> {
                    if (sonValue == JSONObject.NULL) {
                        result[key] = null
                    } else {
                        result[key] = sonValue
                    }
                }
            }
        }
        return result
    }

    private fun convertJSONArray2JavaOnlyArray(arrays: JSONArray): JavaOnlyArray {
        val result = JavaOnlyArray()
        for (i in 0 until arrays.length()) {
            when (val sonValue = arrays.opt(i)) {
                is JSONArray -> {
                    result.add(convertJSONArray2JavaOnlyArray(sonValue))
                }

                is JSONObject -> {
                    result.add(convertJSONObject2JavaOnlyMap(sonValue))
                }

                else -> {
                    result.add(sonValue)
                }
            }
        }
        return result
    }
}
