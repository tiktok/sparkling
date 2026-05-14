// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.hybridkit.lynx

import android.os.Bundle
import com.lynx.tasm.TemplateData
import com.tiktok.sparkling.hybridkit.utils.LogLevel
import com.tiktok.sparkling.hybridkit.utils.LogUtils
import org.json.JSONArray
import org.json.JSONObject

/**
 * wrap [TemplateData] and check data type
 * Hope to gradually replace TemplateData, and do not directly expose TemplateData to the access side.
 * @since 2020/7/27
 */

class LynxInitData {
    private var mData: TemplateData = TemplateData.empty()

    fun getTemplateData(): TemplateData = mData

    fun put(
        key: String,
        value: Any?,
    ) {
        val optValue =
            tryTransformUnsupportedData(
                value,
            )
        mData.put(key, optValue)
    }

    companion object {
        private const val TAG = "LynxInitData"

        @JvmStatic
        fun fromMap(data: Map<String, Any>?): LynxInitData =
            LynxInitData().apply {
                val optValue =
                    tryTransformUnsupportedData(
                        data,
                    ) as? Map<String, Any>
                mData = TemplateData.fromMap(optValue)
            }

        @JvmStatic
        fun fromString(json: String?): LynxInitData =
            LynxInitData().apply {
                mData = TemplateData.fromString(json)
            }

        /**
         * try to transform a data format not supported by TemplateData
         */
        @JvmStatic
        fun tryTransformUnsupportedData(value: Any?): Any? {
            if (value == null) return null
            LogUtils.printLog("dealing with $value[${value.javaClass}]", LogLevel.D, TAG)
            var outData = value
            when (value) {
                is List<*> -> {
                    outData =
                        mutableListOf<Any?>().apply {
                            value.forEach {
                                add(
                                    tryTransformUnsupportedData(
                                        it,
                                    ),
                                )
                            }
                        }
                }

                is Map<*, *> -> {
                    outData =
                        mutableMapOf<String, Any?>().apply {
                            value.forEach {
                                if (it.key is String) {
                                    put(
                                        it.key as String,
                                        tryTransformUnsupportedData(
                                            it.value,
                                        ),
                                    )
                                } else {
                                    LogUtils.printLog("unsupported value $it", LogLevel.E, TAG)
                                }
                            }
                        }
                }

                is Bundle -> {
                    outData =
                        mutableMapOf<String, Any?>().apply {
                            value.keySet().forEach {
                                put(
                                    it,
                                    tryTransformUnsupportedData(
                                        value.get(it),
                                    ),
                                )
                            }
                        }
                }

                is JSONObject -> {
                    outData =
                        mutableMapOf<String, Any?>().apply {
                            value.keys().forEach {
                                put(
                                    it,
                                    tryTransformUnsupportedData(
                                        value.get(it),
                                    ),
                                )
                            }
                        }
                }

                is JSONArray -> {
                    outData =
                        mutableListOf<Any?>().apply {
                            for (i in 0 until value.length()) {
                                add(
                                    tryTransformUnsupportedData(
                                        if (value.isNull(i)) {
                                            null
                                        } else {
                                            value[i]
                                        },
                                    ),
                                )
                            }
                        }
                }

                else -> {}
            }
            return outData
        }
    }
}
