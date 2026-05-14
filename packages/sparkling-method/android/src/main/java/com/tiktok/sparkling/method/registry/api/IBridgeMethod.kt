// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.api

import com.lynx.react.bridge.JavaOnlyMap
import com.lynx.react.bridge.ReadableMap
import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeMethodCallback
import com.tiktok.sparkling.method.registry.core.IBridgeContext
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.interfaces.IPlatformDataProcessor
import com.tiktok.sparkling.method.registry.core.exception.IllegalInputParamException
import com.tiktok.sparkling.method.registry.core.exception.IllegalOperationException
import com.tiktok.sparkling.method.registry.core.exception.IllegalOutputParamException
import com.tiktok.sparkling.method.registry.api.processor.LynxPlatformDataProcessor
import com.tiktok.sparkling.method.registry.api.processor.WebPlatformDataProcessor
import com.tiktok.sparkling.method.registry.api.util.printStackString
import org.json.JSONObject

class JsonProcessor
    @JvmOverloads
    constructor(
        bridge: IDLBridgeMethod,
        data: JSONObject,
        val platForm: BridgeCall.PlatForm = BridgeCall.PlatForm.Web,
    ) : BaseProcessor<JSONObject>(bridge, data) {
        var context: IBridgeContext? = null

        override fun getPlatformType() = BridgePlatformType.WEB

        override val processor =
            WebPlatformDataProcessor().apply {
                this@apply.context = this@JsonProcessor.context
            }

        override fun onError(
            code: Int,
            message: String,
        ): JSONObject =
            JSONObject().apply {
                put("code", code)
                put("msg", message)
            }
    }

class ReadableMapProcessor(
    bridge: IDLBridgeMethod,
    data: ReadableMap,
) : BaseProcessor<ReadableMap>(bridge, data) {
    var context: IBridgeContext? = null

    override fun getPlatformType() = BridgePlatformType.LYNX

    override val processor =
        LynxPlatformDataProcessor().apply {
            this@apply.context = this@ReadableMapProcessor.context
        }

    override fun onError(
        code: Int,
        message: String,
    ): ReadableMap =
        JavaOnlyMap.from(
            hashMapOf<String, Any>().apply {
                put("code", code)
                put("msg", message)
            },
        )
}

abstract class BaseProcessor<Input>(
    val bridge: IDLBridgeMethod,
    val data: Input,
) {
    abstract fun getPlatformType(): BridgePlatformType

    abstract val processor: IPlatformDataProcessor<Input>

    fun handle(callback: IBridgeMethodCallback) {
        try {
            val map = processor.transformPlatformDataToMap(data, bridge::class.java)
            if (map == null) {
                callback.onBridgeResult(
                    onError(
                        IDLBridgeMethod.ANNOTATION_ERROR,
                        "processor.transformPlatformDataToMap failed",
                    )!!,
                )
            } else {
                bridge.realHandle(
                    map,
                    object : IDLBridgeMethod.Callback {
                        override fun invoke(data: Map<String, Any?>) {
                            val code =
                                (data[IDLBridgeMethod.PARAM_CODE] as? Int) ?: IDLBridgeMethod.SUCCESS
                            if (code != IDLBridgeMethod.SUCCESS) {
                                callback.onBridgeResult(
                                    processor.transformMapToPlatformData(
                                        data,
                                        bridge::class.java,
                                    )!!,
                                )
                            } else {
                                callback.onBridgeResult(
                                    processor.transformMapToPlatformData(
                                        data,
                                        bridge::class.java,
                                    )!!,
                                )
                            }
                        }
                    },
                    getPlatformType(),
                )
            }
        } catch (e: IllegalInputParamException) {
            callback.onBridgeResult(onError(IDLBridgeMethod.INVALID_PARAM, e.toString())!!)
        } catch (e: IllegalOutputParamException) {
            callback.onBridgeResult(onError(IDLBridgeMethod.INVALID_RESULT, e.toString())!!)
        } catch (e: IllegalOperationException) {
            callback.onBridgeResult(
                onError(
                    IDLBridgeMethod.ILLEGAL_OPERATION_ERROR,
                    e.toString(),
                )!!,
            )
        } catch (e: Throwable) {
            callback.onBridgeResult(onError(IDLBridgeMethod.UNKNOWN_ERROR, e.printStackString())!!)
        }
    }

    abstract fun onError(
        code: Int,
        message: String,
    ): Input
}
