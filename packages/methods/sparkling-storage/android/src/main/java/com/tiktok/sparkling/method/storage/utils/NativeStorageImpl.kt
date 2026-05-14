// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.storage.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils

import com.tiktok.sparkling.method.registry.core.XReadableType
import com.tiktok.sparkling.method.registry.core.utils.JsonUtils

internal const val PREFERENCE_NAME = "sparkling-storage"

internal class NativeStorageImpl private constructor(
    val context: Context,
) : IBizNativeStorage {
    private val sharedPreferences: SharedPreferences? =
        context.getSharedPreferences(
            PREFERENCE_NAME,
            Context.MODE_PRIVATE,
        )

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: NativeStorageImpl? = null

        /**
         * provide a singleInstance
         */
        fun getInstance(context: Context): NativeStorageImpl {
            val i = instance
            if (i != null) {
                return i
            }

            return synchronized(this) {
                var temp = instance
                if (temp != null) {
                    temp
                } else {
                    temp = NativeStorageImpl(context)
                    instance = temp
                    temp
                }
            }
        }
    }

    private fun getSharedPreferencesInternal(): SharedPreferences? = sharedPreferences

    private fun getBizSharedPreferencesInternal(biz: String): SharedPreferences? =
        context?.getSharedPreferences(
            biz + "-sparkling-storage",
            Context.MODE_PRIVATE,
        )

    private fun getEditorInternal(): SharedPreferences.Editor? = sharedPreferences?.edit()

    private fun getBizEditorInternal(biz: String): SharedPreferences.Editor? = getBizSharedPreferencesInternal(biz)?.edit()

    private fun wrapValueWithType(value: Any): String =
        mutableMapOf<String, Any>().let {
            when (value) {
                is Boolean -> {
                    JsonUtils.toJson(StorageValue(XReadableType.Boolean.name, value.toString()))
                }

                is Int -> {
                    JsonUtils.toJson(StorageValue(XReadableType.Int.name, value.toString()))
                }

                is Double -> {
                    JsonUtils.toJson(StorageValue(XReadableType.Number.name, value.toString()))
                }

                is String -> {
                    JsonUtils.toJson(StorageValue(XReadableType.String.name, value.toString()))
                }

                is List<*> -> {
                    JsonUtils.toJson(
                        StorageValue(
                            XReadableType.Array.name,
                            JsonUtils.toJson(value),
                        ),
                    )
                }

                is Map<*, *> -> {
                    JsonUtils.toJson(StorageValue(XReadableType.Map.name, JsonUtils.toJson(value)))
                }

                else -> {
                    "" // support later, we can return JsonUtils.toJson(StorageValue(XReadableType.Map.name, JsonUtils.toJson(value)))
                }
            }
        }

    private fun unwrapValue(value: String): Any? {
        if (value.isBlank()) {
            return null
        }

        return try {
            val storageVal = JsonUtils.fromJson(value, StorageValue::class.java)
            if (storageVal == null || storageVal.type.isNullOrBlank()) {
                return null
            }

            val wrappedValue = storageVal.value ?: return null

            val typeEnum =
                try {
                    XReadableType.valueOf(storageVal.type)
                } catch (e: IllegalArgumentException) {
                    return null
                }

            when (typeEnum) {
                XReadableType.Boolean -> wrappedValue.toBooleanStrictOrNull()
                XReadableType.Int -> wrappedValue.toIntOrNull()
                XReadableType.Number -> wrappedValue.toDoubleOrNull()
                XReadableType.String -> wrappedValue
                XReadableType.Array -> JsonUtils.fromJson(wrappedValue, List::class.java)
                XReadableType.Map -> JsonUtils.fromJson(wrappedValue, Map::class.java)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun setStorageItem(
        key: String?,
        data: Any?,
    ): Boolean {
        key?.let { k ->
            data?.let { v ->
                getEditorInternal()?.putString(k, wrapValueWithType(v))?.apply() ?: return false
                return true
            }
        }

        return false
    }

    override fun setBizStorageItem(
        biz: String,
        key: String?,
        data: Any?,
    ): Boolean {
        key?.let { k ->
            data?.let { v ->
                getBizEditorInternal(biz)?.putString(k, wrapValueWithType(v))?.apply()
                    ?: return false
                return true
            }
        }

        return false
    }

    override fun removeStorageItem(key: String?): Boolean {
        key?.let {
            getEditorInternal()?.remove(it)?.apply() ?: return false
            return true
        }

        return false
    }

    override fun removeBizStorageItem(
        biz: String,
        key: String?,
    ): Boolean {
        key?.let {
            getBizEditorInternal(biz)?.remove(it)?.apply() ?: return false
            return true
        }

        return false
    }

    override fun getStorageItem(key: String?): Any? {
        key ?: return null
        val sharedPreferencesInternal = getSharedPreferencesInternal() ?: return null
        if (!sharedPreferencesInternal.contains(key)) {
            return null
        }

        val wrappedValue = sharedPreferencesInternal.getString(key, "")
        if (TextUtils.isEmpty(wrappedValue)) {
            return null
        }
        return wrappedValue?.let { unwrapValue(wrappedValue) }
    }

    override fun getBizStorageItem(
        biz: String,
        key: String?,
    ): Any? {
        key ?: return null
        val bizSharedPreferencesInternal = getBizSharedPreferencesInternal(biz) ?: return null
        if (!bizSharedPreferencesInternal.contains(key)) {
            return null
        }

        val wrappedValue = bizSharedPreferencesInternal.getString(key, "")
        if (TextUtils.isEmpty(wrappedValue)) {
            return null
        }
        return wrappedValue?.let { unwrapValue(wrappedValue) }
    }

    override fun getStorageInfo(): Set<String> = getSharedPreferencesInternal()?.all?.keys ?: setOf()

    override fun getBizStorageInfo(biz: String): Set<String> = getBizSharedPreferencesInternal(biz)?.all?.keys ?: setOf()
}
