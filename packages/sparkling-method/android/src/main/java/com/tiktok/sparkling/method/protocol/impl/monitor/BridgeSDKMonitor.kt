// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.impl.monitor

import android.content.Context
import com.tiktok.sparkling.method.protocol.utils.MonitorUtils
import org.json.JSONObject

class BridgeSDKMonitor {
    companion object {
        private const val TAG = "BridgeSDKMonitor"
        private const val SERVICE_NAME = "bridge_performance"
        var channel_: String = ""
    }

    constructor(
        context: Context,
        appInfo: APPInfo4Monitor,
    ) {
        val appVersion = appInfo.appVersion
        val updateVersionCode = appInfo.update_version_code
        val channel = appInfo.channel
        val sdkVersion = appInfo.sdkVersion
        channel_ = channel ?: ""

        val header = JSONObject()
        header.apply {
            // optional，the major version number of the host
            put("app_version", appVersion)
            // optional, the minor version number of the host
            put("update_version_code", updateVersionCode)
            put("channel", channel)
            // optional，sdk version
            put("sdk_version", sdkVersion)
        }
    }

    /**
     *
     * @param serviceName event name
     * @param category    enumerable Key-Value
     * @param metric      not enumerable Key-Value
     * @param logExtr     Information fields that other users want to upload, can be queried by hive, and do not participate in the output of the report
     */
    fun monitorEvent(data: MonitorModel) {
        val category = JSONObject() // enumerable
        val metric = JSONObject() // not enumerable

        category.put("code", data.code)
        category.put("url", data.url)
        category.put("channel", data.channel)
        category.put("method", data.method)
        category.put("container_type", data.containerType)

        metric.put("duration", data.duration)
        metric.put("request_data_length", data.request_data_length)
        metric.put("request_send_timestamp", data.request_send_timestamp)
        metric.put("request_receive_timestamp", data.request_receive_timestamp)
        metric.put("request_decode_duration", data.request_decode_duration)
        metric.put("request_duration", data.request_duration)

        metric.put("response_data_length", data.response_data_length)
        metric.put("response_send_timestamp", data.response_send_timestamp)
        metric.put("response_receive_timestamp", data.response_receive_timestamp)
        metric.put("response_encode_duration", data.response_encode_duration)
        metric.put("response_duration", data.response_duration)

        MonitorUtils.customReport(SERVICE_NAME, category, metric, null)
    }

    data class APPInfo4Monitor(
        val appVersion: String? = null,
        val update_version_code: String? = null,
        val channel: String? = null,
        val sdkVersion: String? = null,
    )

    enum class ContainerType(
        val type: String,
    ) {
        LYNX("lynx"),
        H5("h5"),
    }

    data class MonitorModel(
        val method: String?, // not enumerable
        val code: Int?, // enumerable
        val channel: String?, // enumerable
        val containerType: String?, // enumerable
        val duration: Long?, // not enumerable response.receive_timestamp - request.send_timestamp, unit: ms
        val url: String?, // not enumerable
        val request_data_length: Int?, // input parameter data size, unit: byte
        val request_send_timestamp: Long?, // send time of fe, unit: ms
        val request_receive_timestamp: Long?, // client receiving timing, unit: ms
        val request_decode_duration: Long?, // data deserialization time, unit: ms
        val request_duration: Long?, // receive_timestamp - send_timestamp, unit: ms
        val response_data_length: Int?, // return parameter data size, unit: byte
        val response_encode_duration: Long?, // data serialization time, unit:ms
        val response_send_timestamp: Long?, // client sending timing, unit: ms
        val response_receive_timestamp: Long?, // front-end reception timing, unit: ms
        val response_duration: Long?, // receive_timestamp - send_timestamp, unit: ms
    ) {
        class Builder {
            private var method: String? = null
            private var code: Int? = null
            private var channel: String? = null
            private var containerType: String? = null

            private var duration: Long? = null
            private var url: String? = null

            private var request_data_length: Int? = null
            private var request_send_timestamp: Long? = null
            private var request_receive_timestamp: Long? = null
            private var request_decode_duration: Long? = null
            private var request_duration: Long? = null

            private var response_data_length: Int? = null
            private var response_encode_duration: Long? = null
            private var response_send_timestamp: Long? = null
            private var response_receive_timestamp: Long? = null
            private var response_duration: Long? = null

            fun setMethod(str: String): Builder {
                this.method = str
                return this
            }

            fun setURL(url: String): Builder {
                this.url = url
                return this
            }

            fun setCode(code: Int): Builder {
                this.code = code
                return this
            }

            fun setChannel(channel: String): Builder {
                this.channel = channel
                return this
            }

            fun setContainerType(type: String): Builder {
                this.containerType = type
                return this
            }

            fun setDuration(): Builder {
                if (response_receive_timestamp != null && request_send_timestamp != null) {
                    this.duration = response_receive_timestamp!! - request_send_timestamp!!
                }
                return this
            }

            fun setRequestDataLength(length: Int): Builder {
                this.request_data_length = length
                return this
            }

            fun setRequestSendTimestamp(ts: Long): Builder {
                this.request_send_timestamp = ts
                return this
            }

            fun setRequestReceiveTimestamp(ts: Long): Builder {
                this.request_receive_timestamp = ts
                return this
            }

            fun setRequestDuration(): Builder {
                if (request_receive_timestamp != null && request_send_timestamp != null) {
                    this.request_duration = request_receive_timestamp!! - request_send_timestamp!!
                }
                return this
            }

            fun setRequestDecodeDuration(duration: Long): Builder {
                this.request_decode_duration = duration
                return this
            }

            // /

            fun setResponseDataLength(length: Int): Builder {
                this.response_data_length = length
                return this
            }

            fun setResponseSendTimestamp(ts: Long): Builder {
                this.response_send_timestamp = ts
                return this
            }

            fun setResponseReceiveTimestamp(ts: Long): Builder {
                this.response_receive_timestamp = ts
                return this
            }

            fun setResponseDuration(): Builder {
                if (response_receive_timestamp != null && response_send_timestamp != null) {
                    this.response_duration =
                        response_receive_timestamp!! - response_send_timestamp!!
                }
                return this
            }

            fun setResponseEncodeDuration(duration: Long): Builder {
                this.response_encode_duration = duration
                return this
            }

            fun build(): MonitorModel =
                MonitorModel(
                    method,
                    code,
                    channel,
                    containerType,
                    duration,
                    url,
                    request_data_length,
                    request_send_timestamp,
                    request_receive_timestamp,
                    request_decode_duration,
                    request_duration,
                    response_data_length,
                    response_encode_duration,
                    response_send_timestamp,
                    response_receive_timestamp,
                    response_duration,
                )
        }
    }
}
