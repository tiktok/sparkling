// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool.inspect

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * One observed Sparkling bridge method invocation.
 *
 * `params` and `result` are pre-formatted JSON strings (or plain text fallback)
 * so the panel can render them directly. `success` is best-effort — it follows
 * the standard IDL `code` field when available.
 */
data class MethodInvocation(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val namespace: String?,
    val platform: String,
    val params: String,
    val result: String?,
    val code: Int?,
    val success: Boolean?,
    val startTimeMs: Long,
    val endTimeMs: Long?,
    val expanded: Boolean = false,
) {
    val durationMs: Long? get() = endTimeMs?.let { it - startTimeMs }

    companion object {
        const val PLATFORM_LYNX = "lynx"
        const val PLATFORM_ALL = "all"

        fun formatPayload(payload: Any?): String {
            if (payload == null) return ""
            return try {
                when (payload) {
                    is Map<*, *> -> JSONObject(payload).toString(2)
                    is JSONObject -> payload.toString(2)
                    is JSONArray -> payload.toString(2)
                    is List<*> -> JSONArray(payload).toString(2)
                    is String -> payload
                    else -> payload.toString()
                }
            } catch (t: Throwable) {
                payload.toString()
            }
        }
    }
}
