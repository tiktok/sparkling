// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.ota

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.concurrent.thread

internal data class ZephyrOtaRuntimeConfig(
    val enabled: Boolean,
    val versionUrl: String?,
    val pollingEnabled: Boolean,
    val pollingIntervalMs: Long
)

data class ZephyrVersionInfo(
    val snapshotId: String,
    val versionUrl: String
)

object ZephyrOtaManager {
    interface Listener {
        fun onUpdateReady(info: ZephyrVersionInfo)
    }

    private const val TAG = "SparklingZephyrOTA"
    private const val PREFS_NAME = "sparkling_zephyr_ota"
    private const val CONFIG_FILE = "sparkling.zephyr.json"
    private const val VERSION_INFO_PATH = "__get_version_info__"
    private const val KEY_APPLIED_SNAPSHOT_ID = "applied_snapshot_id"
    private const val KEY_APPLIED_VERSION_URL = "applied_version_url"
    private const val KEY_PENDING_SNAPSHOT_ID = "pending_snapshot_id"
    private const val KEY_PENDING_VERSION_URL = "pending_version_url"
    private const val KEY_PENDING_UPDATE_AVAILABLE = "pending_update_available"
    private const val KEY_LAST_CHECK_AT = "last_check_at"
    private const val DEFAULT_INTERVAL_MS = 15 * 60 * 1000L
    private const val MIN_POLL_DELAY_MS = 1_000L

    @Volatile
    private var refreshInFlight = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArraySet<Listener>()
    private var pollingRunnable: Runnable? = null
    private var pollingContext: Context? = null

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun hasPendingUpdate(context: Context): Boolean {
        return context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PENDING_UPDATE_AVAILABLE, false)
    }

    fun resolveBundle(context: Context, bundle: String?): String? {
        if (bundle.isNullOrBlank() || isRemoteUrl(bundle)) {
            return bundle
        }

        val config = readRuntimeConfig(context) ?: return bundle
        if (!config.enabled) {
            return bundle
        }

        val appliedVersionUrl = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_APPLIED_VERSION_URL, null)

        return joinBundleUrl(appliedVersionUrl, bundle) ?: bundle
    }

    fun applyPendingUpdateIfNeeded(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val pendingSnapshotId = prefs.getString(KEY_PENDING_SNAPSHOT_ID, null)
        val pendingVersionUrl = prefs.getString(KEY_PENDING_VERSION_URL, null)
        if (!prefs.getBoolean(KEY_PENDING_UPDATE_AVAILABLE, false) ||
            pendingSnapshotId.isNullOrBlank() ||
            pendingVersionUrl.isNullOrBlank()
        ) {
            return false
        }

        prefs.edit()
            .putString(KEY_APPLIED_SNAPSHOT_ID, pendingSnapshotId)
            .putString(KEY_APPLIED_VERSION_URL, pendingVersionUrl)
            .remove(KEY_PENDING_SNAPSHOT_ID)
            .remove(KEY_PENDING_VERSION_URL)
            .putBoolean(KEY_PENDING_UPDATE_AVAILABLE, false)
            .apply()
        return true
    }

    fun refreshIfNeeded(context: Context) {
        val appContext = context.applicationContext
        val config = readRuntimeConfig(appContext) ?: return
        if (!config.enabled || !config.pollingEnabled) {
            return
        }

        val checkBaseUrl = normalizeVersionUrl(config.versionUrl) ?: return
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastCheckAt = prefs.getLong(KEY_LAST_CHECK_AT, 0L)
        if (now - lastCheckAt < config.pollingIntervalMs.coerceAtLeast(0L)) {
            return
        }

        synchronized(this) {
            if (refreshInFlight) {
                return
            }
            refreshInFlight = true
            prefs.edit().putLong(KEY_LAST_CHECK_AT, now).apply()
        }

        thread(name = "sparkling-zephyr-ota", isDaemon = true) {
            try {
                val versionInfo = fetchVersionInfo(checkBaseUrl) ?: return@thread
                val appliedSnapshotId = prefs.getString(KEY_APPLIED_SNAPSHOT_ID, null)
                val pendingSnapshotId = prefs.getString(KEY_PENDING_SNAPSHOT_ID, null)
                val normalizedNextSnapshotId = versionInfo.snapshotId.trim()
                val shouldNotify = when {
                    appliedSnapshotId.isNullOrBlank() -> {
                        prefs.edit()
                            .putString(KEY_APPLIED_SNAPSHOT_ID, normalizedNextSnapshotId)
                            .putString(KEY_APPLIED_VERSION_URL, versionInfo.versionUrl)
                            .remove(KEY_PENDING_SNAPSHOT_ID)
                            .remove(KEY_PENDING_VERSION_URL)
                            .putBoolean(KEY_PENDING_UPDATE_AVAILABLE, false)
                            .putLong(KEY_LAST_CHECK_AT, System.currentTimeMillis())
                            .apply()
                        false
                    }
                    shouldMarkUpdateReady(appliedSnapshotId, normalizedNextSnapshotId) -> {
                        prefs.edit()
                            .putString(KEY_PENDING_SNAPSHOT_ID, normalizedNextSnapshotId)
                            .putString(KEY_PENDING_VERSION_URL, versionInfo.versionUrl)
                            .putBoolean(KEY_PENDING_UPDATE_AVAILABLE, true)
                            .putLong(KEY_LAST_CHECK_AT, System.currentTimeMillis())
                            .apply()
                        pendingSnapshotId?.trim() != normalizedNextSnapshotId
                    }
                    else -> {
                        prefs.edit()
                            .remove(KEY_PENDING_SNAPSHOT_ID)
                            .remove(KEY_PENDING_VERSION_URL)
                            .putBoolean(KEY_PENDING_UPDATE_AVAILABLE, false)
                            .putLong(KEY_LAST_CHECK_AT, System.currentTimeMillis())
                            .apply()
                        false
                    }
                }
                Log.d(
                    TAG,
                    "refresh applied=${appliedSnapshotId ?: "nil"} pending=${pendingSnapshotId ?: "nil"} next=${versionInfo.snapshotId} notify=$shouldNotify versionUrl=${versionInfo.versionUrl}"
                )
                if (shouldNotify) {
                    notifyUpdateReady(versionInfo)
                }
            } catch (error: Exception) {
                Log.w(TAG, "Zephyr OTA refresh failed: ${error.message}")
            } finally {
                refreshInFlight = false
            }
        }
    }

    fun startPolling(context: Context) {
        val appContext = context.applicationContext
        val config = readRuntimeConfig(appContext) ?: return
        if (!config.enabled || !config.pollingEnabled) {
            stopPolling()
            return
        }

        val delayMs = config.pollingIntervalMs.coerceAtLeast(MIN_POLL_DELAY_MS)
        synchronized(this) {
            if (pollingContext === appContext && pollingRunnable != null) {
                return
            }
            stopPollingLocked()
            pollingContext = appContext
            pollingRunnable = object : Runnable {
                override fun run() {
                    refreshIfNeeded(appContext)
                    mainHandler.postDelayed(this, delayMs)
                }
            }.also { runnable ->
                mainHandler.post(runnable)
            }
        }
    }

    fun stopPolling() {
        synchronized(this) {
            stopPollingLocked()
        }
    }

    internal fun readRuntimeConfig(context: Context): ZephyrOtaRuntimeConfig? {
        val jsonString = try {
            context.assets.open(CONFIG_FILE).bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            return null
        }

        return try {
            val json = JSONObject(jsonString)
            ZephyrOtaRuntimeConfig(
                enabled = json.optBoolean("enabled", false),
                versionUrl = normalizeVersionUrl(json.optString("versionUrl").takeIf { it.isNotBlank() }),
                pollingEnabled = json.optJSONObject("polling")?.optBoolean("enabled", true) ?: true,
                pollingIntervalMs = json.optJSONObject("polling")?.optLong("intervalMs", DEFAULT_INTERVAL_MS)
                    ?: DEFAULT_INTERVAL_MS,
            )
        } catch (error: Exception) {
            Log.w(TAG, "Invalid Zephyr OTA config: ${error.message}")
            null
        }
    }

    internal fun fetchVersionInfo(baseUrl: String): ZephyrVersionInfo? {
        val endpoint = toVersionInfoEndpoint(baseUrl)
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 3_000
            readTimeout = 3_000
            setRequestProperty("Accept", "application/json")
        }

        return try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                Log.w(TAG, "Version info request failed: $responseCode")
                return null
            }

            val body = connection.inputStream.bufferedReader().use { stream -> stream.readText() }
            parseVersionInfo(body, baseUrl)
        } finally {
            connection.disconnect()
        }
    }

    internal fun parseVersionInfo(body: String, fallbackBaseUrl: String): ZephyrVersionInfo? {
        val json = JSONObject(body)
        val snapshotId = json.optString("snapshot_id", "").trim()
        if (snapshotId.isEmpty()) {
            return null
        }

        val versionUrl = normalizeVersionUrl(json.optString("version_url", fallbackBaseUrl))
            ?: normalizeVersionUrl(fallbackBaseUrl)
            ?: return null

        return ZephyrVersionInfo(
            snapshotId = snapshotId,
            versionUrl = versionUrl,
        )
    }

    internal fun normalizeVersionUrl(value: String?): String? {
        val trimmed = value?.trim()
        if (trimmed.isNullOrEmpty()) {
            return null
        }

        val withoutTrailingSlash = trimmed.replace(Regex("/+$"), "")
        if (withoutTrailingSlash.startsWith("http://", ignoreCase = true) ||
            withoutTrailingSlash.startsWith("https://", ignoreCase = true)
        ) {
            return withoutTrailingSlash
        }

        return "https://${withoutTrailingSlash.trimStart('/')}"
    }

    internal fun toVersionInfoEndpoint(baseUrl: String): String {
        return "${baseUrl.trimEnd('/')}/$VERSION_INFO_PATH"
    }

    internal fun joinBundleUrl(versionUrl: String?, bundle: String?): String? {
        val normalizedBase = normalizeVersionUrl(versionUrl) ?: return null
        val normalizedBundle = bundle
            ?.trim()
            ?.removePrefix("./")
            ?.trimStart('/')
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        return "${normalizedBase.trimEnd('/')}/$normalizedBundle"
    }

    private fun isRemoteUrl(value: String): Boolean {
        return value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true)
    }

    internal fun shouldMarkUpdateReady(previousSnapshotId: String?, nextSnapshotId: String): Boolean {
        val normalizedPrevious = previousSnapshotId?.trim()?.takeIf { it.isNotEmpty() } ?: return false
        return normalizedPrevious != nextSnapshotId.trim()
    }

    private fun notifyUpdateReady(info: ZephyrVersionInfo) {
        if (listeners.isEmpty()) {
            return
        }
        mainHandler.post {
            listeners.forEach { listener ->
                listener.onUpdateReady(info)
            }
        }
    }

    private fun stopPollingLocked() {
        pollingRunnable?.let(mainHandler::removeCallbacks)
        pollingRunnable = null
        pollingContext = null
    }
}
