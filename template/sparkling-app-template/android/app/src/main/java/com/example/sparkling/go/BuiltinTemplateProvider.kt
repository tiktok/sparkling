// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.example.sparkling.go

import android.content.Context
import com.lynx.tasm.provider.AbsTemplateProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.IOException

class BuiltinTemplateProvider(context: Context) : AbsTemplateProvider() {

    private var mContext: Context = context.applicationContext
    private val httpClient = OkHttpClient()

    override fun loadTemplate(uri: String, callback: Callback) {
        Thread {
            try {
                // Debug mode supports remote HTTP bundle URLs as well as local asset bundles.
                // Release paths use bundle=... and therefore stay on assets only.
                if (isRemoteUrl(uri)) {
                    val request = Request.Builder().url(uri).get().build()
                    httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            callback.onFailed("HTTP ${response.code}")
                            return@Thread
                        }
                        val bytes = response.body?.bytes()
                        if (bytes == null) {
                            callback.onFailed("empty response body")
                            return@Thread
                        }
                        callback.onSuccess(bytes)
                    }
                    return@Thread
                }

                mContext.assets.open(uri).use { inputStream ->
                    ByteArrayOutputStream().use { byteArrayOutputStream ->
                        val buffer = ByteArray(1024)
                        var length: Int
                        while ((inputStream.read(buffer).also { length = it }) != -1) {
                            byteArrayOutputStream.write(buffer, 0, length)
                        }
                        callback.onSuccess(byteArrayOutputStream.toByteArray())
                    }
                }
            } catch (e: IOException) {
                callback.onFailed(e.message)
            }
        }.start()
    }

    private fun isRemoteUrl(uri: String): Boolean {
        val value = uri.trim().lowercase()
        return value.startsWith("http://") || value.startsWith("https://")
    }
}
