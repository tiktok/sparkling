// Copyright 2025 TikTok Inc.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.media.utils

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel

object AppFileUtils {
    /**
     * @param context Context
     * @param filePath String
     * @return String?
     */
    fun getOrCopiedFilePath(
        context: Context,
        filePath: String,
    ): String? {
        val uri = Uri.parse(filePath)
        uri ?: return null
        val scheme = uri.scheme
        if (scheme.isNullOrEmpty() || scheme == ContentResolver.SCHEME_FILE) {
            return uri.path
        }

        var resultUri: Uri? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri) && FConstants.MEDIA_DOCUMENTS == uri.authority) {
            val split = DocumentsContract.getDocumentId(uri).split(":")
            val type = split[0]
            var contentUri: Uri? = null
            if (TextUtils.equals(FConstants.URI_IMAGE, type)) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                if (isAndroidQOrLater()) {
                    contentUri = MediaStore.Images.Media.getContentUri("external_primary")
                }
            } else if (TextUtils.equals(FConstants.URI_VIDEO, type)) {
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                if (isAndroidQOrLater()) {
                    contentUri = MediaStore.Video.Media.getContentUri("external_primary")
                }
            } else if (TextUtils.equals(FConstants.URI_AUDIO, type)) {
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                if (isAndroidQOrLater()) {
                    contentUri = MediaStore.Audio.Media.getContentUri("external_primary")
                }
            }

            resultUri =
                if (TextUtils.isEmpty(split[1])) {
                    return null
                } else {
                    contentUri?.let {
                        ContentUris.withAppendedId(
                            it,
                            split[1].toLong(),
                        )
                    }
                }
        }
        if (scheme == ContentResolver.SCHEME_CONTENT && "media" == uri.authority) {
            resultUri = uri
        }
        if (resultUri != null) {
            var temPath = context.cacheDir.absolutePath + "/tools/temMedia/" + System.currentTimeMillis()
            if (!checkFileExists(temPath)) {
                createFile(temPath, true)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                var fileInputStream: FileInputStream? = null
                var fileOutputStream: FileOutputStream? = null
                var inChannel: FileChannel? = null
                var outChannel: FileChannel? = null
                try {
                    val fd = context.contentResolver.openAssetFileDescriptor(resultUri, "r", null)
                    fileInputStream = fd?.createInputStream()
                    fileOutputStream = FileOutputStream(temPath)
                    inChannel = fileInputStream?.channel
                    outChannel = fileOutputStream.channel
                    inChannel?.transferTo(0, inChannel.size(), outChannel)
                } catch (e: Exception) {
                    temPath = ""
                } finally {
                    try {
                        fileInputStream?.close()
                        fileOutputStream?.close()
                        inChannel?.close()
                        outChannel?.close()
                    } catch (e: IOException) {
                        temPath = ""
                    }
                }
            } else {
                var bufferedInputStream: BufferedInputStream? = null
                var bufferedOutputStream: BufferedOutputStream? = null
                try {
                    bufferedInputStream = BufferedInputStream(context.contentResolver?.openInputStream(resultUri))
                    bufferedOutputStream = BufferedOutputStream(FileOutputStream(temPath))

                    var len: Int
                    val bytes = ByteArray(4096)

                    while (bufferedInputStream.read(bytes).also { len = it } != -1) {
                        bufferedOutputStream.write(bytes, 0, len)
                    }
                } catch (e: Exception) {
                    temPath = ""
                } finally {
                    try {
                        bufferedInputStream?.close()
                        bufferedOutputStream?.close()
                    } catch (e: IOException) {
                        temPath = ""
                    }
                }
            }
            return temPath
        }
        return BdFileUtils.convertUriToPath(context, uri)
    }

    private fun checkFileExists(path: String): Boolean = if (path.isEmpty()) false else File(path).exists()

    private fun createFile(
        path: String,
        isFile: Boolean,
    ): File? {
        if (path.isNotEmpty()) {
            val file = File(path)
            if (!file.exists()) {
                if (!isFile) {
                    file.mkdirs()
                } else {
                    runCatching {
                        val parent = file.parentFile
                        if (!parent.exists()) {
                            parent.mkdirs()
                        }
                        file.createNewFile()
                    }
                }
            }
            return file
        } else {
            return null
        }
    }

    private fun isAndroidQOrLater(): Boolean = Build.VERSION.SDK_INT >= 29
}
