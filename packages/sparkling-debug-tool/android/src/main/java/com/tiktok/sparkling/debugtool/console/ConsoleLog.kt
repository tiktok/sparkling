// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool.console

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * One JS console message captured from a Lynx view via the inspector console
 * delegate, or from the legacy [com.tiktok.sparkling.debugtool.SparklingDebugTool.recordConsoleLine]
 * bridge.
 *
 * The Lynx `LynxInspectorConsoleDelegate` emits a JSON payload of the form
 * `{"type":"log|debug|info|warn|error","data":[ ... ]}`. We flatten the data
 * array into a single human-readable string while retaining the level so the
 * UI can color/filter by it.
 */
data class ConsoleLog(
    val type: String,
    val level: Int,
    val message: String,
    val tag: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    var expanded: Boolean = false,
) {
    fun matches(keyword: String): Boolean {
        if (keyword.isEmpty()) return true
        val needle = keyword.trim()
        if (needle.isEmpty()) return true
        return message.contains(needle, ignoreCase = true) ||
            tag.contains(needle, ignoreCase = true) ||
            type.contains(needle, ignoreCase = true)
    }

    companion object {
        const val TYPE_LOG = "log"
        const val TYPE_DEBUG = "debug"
        const val TYPE_INFO = "info"
        const val TYPE_WARN = "warn"
        const val TYPE_ERROR = "error"

        fun levelFromType(type: String?): Int =
            when (type?.lowercase()) {
                TYPE_DEBUG -> Log.DEBUG
                TYPE_INFO -> Log.INFO
                TYPE_WARN -> Log.WARN
                TYPE_ERROR -> Log.ERROR
                TYPE_LOG -> Log.VERBOSE
                else -> Log.VERBOSE
            }

        fun levelFromName(name: String?): Int =
            when (name?.uppercase()) {
                "V", "VERBOSE" -> Log.VERBOSE
                "D", "DEBUG" -> Log.DEBUG
                "I", "INFO" -> Log.INFO
                "W", "WARN", "WARNING" -> Log.WARN
                "E", "ERROR" -> Log.ERROR
                else -> Log.INFO
            }

        fun typeFromLevel(level: Int): String =
            when (level) {
                Log.VERBOSE -> TYPE_LOG
                Log.DEBUG -> TYPE_DEBUG
                Log.INFO -> TYPE_INFO
                Log.WARN -> TYPE_WARN
                Log.ERROR -> TYPE_ERROR
                else -> TYPE_LOG
            }

        /** Parse one Lynx inspector console message JSON payload. */
        fun fromJson(raw: String): ConsoleLog? {
            if (raw.isBlank()) return null
            return try {
                val obj = JSONObject(raw)
                val type = obj.optString("type", TYPE_LOG)
                val data = obj.optJSONArray("data")
                val text = formatData(data) ?: obj.optString("message", raw)
                ConsoleLog(
                    type = type,
                    level = levelFromType(type),
                    message = text,
                )
            } catch (t: Throwable) {
                ConsoleLog(
                    type = TYPE_LOG,
                    level = Log.VERBOSE,
                    message = raw,
                )
            }
        }

        /** Build a plain console line that didn't come through the JSON inspector pipe. */
        fun fromPlain(
            level: String,
            tag: String,
            message: String,
        ): ConsoleLog {
            val lvl = levelFromName(level)
            return ConsoleLog(
                type = typeFromLevel(lvl),
                level = lvl,
                message = message,
                tag = tag,
            )
        }

        private fun formatData(data: JSONArray?): String? {
            if (data == null) return null
            val length = data.length()
            if (length == 0) return ""
            val builder = StringBuilder()
            for (i in 0 until length) {
                if (i > 0) builder.append('\t')
                when (val item = data.opt(i)) {
                    null, JSONObject.NULL -> builder.append("null")
                    is JSONObject -> builder.append(item.toString(2))
                    is JSONArray -> builder.append(item.toString(2))
                    else -> builder.append(item.toString())
                }
            }
            return builder.toString()
        }
    }
}
