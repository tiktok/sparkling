// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core.utils

import com.google.gson.Gson
import com.tiktok.sparkling.method.registry.core.model.idl.IDLDynamic
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import com.tiktok.sparkling.method.registry.core.model.idl.getValue
import com.tiktok.sparkling.method.registry.core.model.idl.toPrimitiveOrJSON
import org.json.JSONArray
import org.json.JSONObject

object JsonUtils {
    val GSON = Gson()

    /**
     * serializes the specified object into its equivalent Json representation.
     */
    fun toJson(obj: Any): String = GSON.toJson(obj)

    /**
     * deserializes the specified Json into an object of the specified class.
     */
    fun <T> fromJson(
        json: String,
        typeClass: Class<T>,
    ): T = GSON.fromJson(json, typeClass)

    fun mapToJSON(map: Map<String, Any>): JSONObject {
        val jsonObject = JSONObject()
        map.forEach {
            val key = it.key
            when (val value = it.value) {
                is Long -> {
                    jsonObject.put(key, value.toDouble())
                }

                is Float -> {
                    jsonObject.put(key, value.toDouble())
                }

                is Int -> {
                    jsonObject.put(key, value)
                }

                is Double -> {
                    jsonObject.put(key, value)
                }

                is String -> {
                    jsonObject.put(key, value)
                }

                is Boolean -> {
                    jsonObject.put(key, value)
                }

                is Map<*, *> -> {
                    try {
                        jsonObject.put(key, mapToJSON(value as Map<String, Any>))
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                is List<*> -> {
                    try {
                        jsonObject.put(key, listToJSON(value as List<Any>))
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                is IDLDynamic -> {
                    jsonObject.put(key, value.getValue())
                }
            }
        }
        return jsonObject
    }

    fun listToJSON(list: List<Any>): JSONArray {
        val jsonArray = JSONArray()
        list.forEach {
            when (it) {
                is Float -> {
                    jsonArray.put(it.toDouble())
                }

                is Long -> {
                    jsonArray.put(it.toDouble())
                }

                is Int -> {
                    jsonArray.put(it)
                }

                is Double -> {
                    jsonArray.put(it)
                }

                is String -> {
                    jsonArray.put(it)
                }

                is Boolean -> {
                    jsonArray.put(it)
                }

                is Map<*, *> -> {
                    try {
                        jsonArray.put(mapToJSON(it as Map<String, Any>))
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                is List<*> -> {
                    try {
                        jsonArray.put(listToJSON(it as List<Any>))
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                is IDLDynamic -> {
                    jsonArray.put(it.getValue())
                }
            }
        }
        return jsonArray
    }

    fun jsonToMap(json: JSONObject): Map<String, Any?> =
        mutableMapOf<String, Any?>().apply {
            val iterator = json.keys()
            while (iterator.hasNext()) {
                val key = iterator.next()
                when (json.opt(key)) {
                    is Long -> put(key, json.optLong(key))
                    is Double -> put(key, json.optDouble(key))
                    is Int -> put(key, json.optInt(key))
                    is String -> put(key, json.optString(key))
                    is JSONObject -> put(key, jsonToMap(json.optJSONObject(key)))
                    is JSONArray -> put(key, jsonToList(json.optJSONArray(key)))
                    is Boolean -> put(key, json.optBoolean(key))
                    else -> put(key, null)
                }
            }
        }

    fun jsonToList(json: JSONArray): List<Any?> =
        mutableListOf<Any?>().apply {
            val len = json.length()
            for (index in 0 until len) {
                when (json.opt(index)) {
                    is Long -> add(json.optLong(index))
                    is Double -> add(json.optDouble(index))
                    is Int -> add(json.getInt(index))
                    is String -> add(json.optString(index))
                    is JSONObject -> add(jsonToMap(json.optJSONObject(index)))
                    is JSONArray -> add(jsonToList(json.optJSONArray(index)))
                    is Boolean -> add(json.optBoolean(index))
                    else -> add(null)
                }
            }
        }

    @JvmStatic
    fun toJSONArray(source: List<*>): JSONArray {
        val result = JSONArray()
        source
            .filterNotNull()
            .map {
                when (it) {
                    is Int -> {
                        it
                    }

                    is Long -> {
                        it
                    }

                    is String -> {
                        it
                    }

                    is Boolean -> {
                        it
                    }

                    is Double -> {
                        it
                    }

                    is List<*> -> {
                        toJSONArray(it)
                    }

                    is Map<*, *> -> {
                        toJSONObject(it)
                    }

                    is IDLDynamic -> {
                        it.toPrimitiveOrJSON()
                    }

                    else -> {
                        // nested class type
                        if (it is IDLMethodBaseModel) {
                            it.toJSON()
                        } else {
                            null
                        }
                    }
                }
            }.forEach {
                it?.let {
                    result.put(it)
                }
            }
        return result
    }

    @JvmStatic
    fun toJSONObject(source: Map<*, *>): JSONObject {
        val result = JSONObject()
        source
            .map { entry ->
                val entryValue = entry.value
                entry.key to
                    when (entryValue) {
                        is Int -> {
                            entryValue
                        }

                        is Long -> {
                            entryValue
                        }

                        is String -> {
                            entryValue
                        }

                        is Boolean -> {
                            entryValue
                        }

                        is Double -> {
                            entryValue
                        }

                        is List<*> -> {
                            toJSONArray(entryValue)
                        }

                        is Map<*, *> -> {
                            toJSONObject(entryValue)
                        }

                        is IDLDynamic -> {
                            entryValue.toPrimitiveOrJSON()
                        }

                        else -> {
                            // nested class type
                            if (entryValue is IDLMethodBaseModel) {
                                entryValue.toJSON()
                            } else {
                                null
                            }
                        }
                    }
            }.forEach { pair ->
                pair.first?.let {
                    if (it is String) {
                        result.put(it, pair.second)
                    }
                }
            }
        return result
    }
}
