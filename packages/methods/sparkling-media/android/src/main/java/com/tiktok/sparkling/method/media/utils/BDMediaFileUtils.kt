// Copyright 2025 TikTok Inc.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.media.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object BDMediaFileUtils {
    fun createImageUri(
        context: Context?,
        name: String?,
        mimeType: String?,
    ): Uri? {
        val file =
            File(
                Environment
                    .getExternalStorageDirectory()
                    .getPath() + "/" + Environment.DIRECTORY_DCIM + "/Camera",
            )
        if (!file.exists()) {
            file.mkdirs()
        }

        return createImageUri(context, name, mimeType, Environment.DIRECTORY_DCIM + "/Camera/")
    }

    @JvmOverloads
    fun createImageUri(
        context: Context?,
        name: String?,
        mimeType: String?,
        dirName: String?,
        value: ContentValues? = null as ContentValues?,
    ): Uri? {
        var dirName = dirName
        if (context != null && !TextUtils.isEmpty(name) && !TextUtils.isEmpty(dirName)) {
            if (!dirName!!.endsWith("/")) {
                dirName = dirName + "/"
            }

            var values = value
            if (value == null) {
                values = ContentValues()
            }

            values?.put("_display_name", name)
            values?.put("datetaken", System.currentTimeMillis())
            values?.put("mime_type", mimeType)
            val imageCollection: Uri
            if (isAndroidQOrLater) {
                imageCollection = MediaStore.Images.Media.getContentUri("external_primary")
                values?.put("relative_path", dirName)
            } else {
                imageCollection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                values?.put(
                    "_data",
                    removeDupSlash(
                        Environment
                            .getExternalStorageDirectory()
                            .getPath() + "/" + dirName + "/" + name,
                    ),
                )
            }

            val imageUri = context.getContentResolver().insert(imageCollection, values)
            return imageUri
        } else {
            return null
        }
    }

    fun createVideoUri(
        context: Context?,
        name: String?,
    ): Uri? = createVideoUri(context, name, "video/mp4")

    fun createVideoUri(
        context: Context?,
        name: String?,
        mimeType: String?,
    ): Uri? {
        val file =
            File(
                Environment
                    .getExternalStorageDirectory()
                    .getPath() + "/" + Environment.DIRECTORY_DCIM + "/Camera",
            )
        if (!file.exists()) {
            file.mkdirs()
        }

        return createVideoUri(context, name, mimeType, Environment.DIRECTORY_DCIM + "/Camera/")
    }

    fun createVideoUri(
        context: Context?,
        name: String?,
        mimeType: String?,
        dirName: String?,
    ): Uri? = createVideoUri(context, name, mimeType, dirName, null as ContentValues?)

    fun createVideoUri(
        context: Context?,
        name: String?,
        mimeType: String?,
        dirName: String?,
        value: ContentValues?,
    ): Uri? {
        var dirName = dirName
        if (context != null && !TextUtils.isEmpty(name) && !TextUtils.isEmpty(dirName)) {
            if (!dirName!!.endsWith("/")) {
                dirName = dirName + "/"
            }

            var values = value
            if (value == null) {
                values = ContentValues()
            }

            values?.put("_display_name", name)
            values?.put("datetaken", System.currentTimeMillis())
            values?.put("mime_type", mimeType)
            val videoCollection: Uri
            if (isAndroidQOrLater) {
                videoCollection = MediaStore.Video.Media.getContentUri("external_primary")
                values?.put("relative_path", dirName)
            } else {
                videoCollection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                values?.put(
                    "_data",
                    removeDupSlash(
                        Environment
                            .getExternalStorageDirectory()
                            .getPath() + "/" + dirName + "/" + name,
                    ),
                )
            }

            val videoUri = context.getContentResolver().insert(videoCollection, values)
            return videoUri
        } else {
            return null
        }
    }

    val isAndroidQOrLater: Boolean
        get() = Build.VERSION.SDK_INT >= 29

    fun removeDupSlash(pathname: String): String {
        val n = pathname.length
        val realpath = pathname.toCharArray()
        var index = 0
        var prevChar = 0.toChar()

        for (i in 0 until n) {
            val current = realpath[i]
            if (current != '/' || prevChar != '/') {
                realpath[index++] = current
            }

            prevChar = current
        }

        if (prevChar == '/' && n > 1) {
            --index
        }

        return if (index != n) String(realpath, 0, index) else pathname
    }

    fun removeFile(path: String?): Boolean {
        if (!TextUtils.isEmpty(path) && isSdcardWritable) {
            val file = File(path)
            return file.exists() && file.delete()
        } else {
            return false
        }
    }

    val isSdcardWritable: Boolean
        get() {
            try {
                return "mounted" == Environment.getExternalStorageState()
            } catch (var1: Exception) {
                return false
            }
        }

    fun isUriExists(
        context: Context?,
        uri: Uri?,
    ): Boolean {
        if (null != context && null != uri) {
            val cr = context.getContentResolver()

            try {
                val afd = cr.openAssetFileDescriptor(uri, "r")
                if (null == afd) {
                    return false
                } else {
                    try {
                        afd.close()
                    } catch (var5: IOException) {
                    }

                    return true
                }
            } catch (var6: FileNotFoundException) {
                return false
            }
        } else {
            return false
        }
    }

    @Throws(IOException::class)
    fun copyFile(
        inputStream: InputStream,
        outputStream: OutputStream,
    ) {
        try {
            val buffer = ByteArray(4096)
            if (inputStream == null || outputStream == null) {
                throw IOException("Failed to copy input:" + inputStream + "output:" + outputStream)
            } else {
                while (true) {
                    val readByteCount = inputStream.read(buffer)
                    if (readByteCount <= 0) {
                        outputStream.flush()
                        return
                    }
                    outputStream.write(buffer, 0, readByteCount)
                }
            }
        } finally {
            closeQuietly(inputStream)
            closeQuietly(outputStream)
        }
    }

    fun closeQuietly(`is`: Closeable?) {
        if (`is` != null) {
            try {
                `is`.close()
            } catch (var2: IOException) {
            }
        }
    }

    @JvmOverloads
    fun copyFileToGallery(
        context: Context,
        path: String,
        isImage: Boolean,
        mimeType: String? = "image/jpeg",
    ): Uri? {
        val file = File(path)
        val uri: Uri?
        if (isImage) {
            uri = createImageUri(context, file.getName(), mimeType)
        } else {
            uri = createVideoUri(context, file.getName())
        }
        if (uri == null) return null
        try {
            val outputStream = context.getContentResolver().openOutputStream(uri)
            val inputStream: InputStream = FileInputStream(path)
            copyFile(inputStream, outputStream!!)
            removeFile(path)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (isUriExists(context, uri)) {
            context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
        }
        return uri
    }

    fun getCacheDir(context: Context): File? {
        var cacheDir = context.getExternalCacheDir()
        if (cacheDir == null) {
            cacheDir = context.getCacheDir()
        }
        return cacheDir
    }
}
