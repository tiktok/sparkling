// Copyright 2025 TikTok Inc.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.media.utils

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object BdFileUtils {
    fun convertUriToPath(
        context: Context,
        uri: Uri?,
    ): String? {
        if (uri == null) {
            return null
        }
        val schema = uri.getScheme()
        if (TextUtils.isEmpty(schema) || ContentResolver.SCHEME_FILE == schema) {
            return uri.getPath()
        }
        if (FConstants.HTTP_SCHEMA == schema) {
            return uri.toString()
        }
        if (ContentResolver.SCHEME_CONTENT == schema && "media" == uri.getAuthority()) {
            return getDataColumn(context, uri, null, null)
        }
        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (TextUtils.equals(FConstants.EXTERNAL_DOCUMENTS, uri.getAuthority())) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split: Array<String?> =
                    docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
            } else if (TextUtils.equals(FConstants.DOWNLOADS_DOCUMENTS, uri.getAuthority())) {
                // DownloadsProvider
                val id = DocumentsContract.getDocumentId(uri)
                if (id != null && id.startsWith(FConstants.URI_RAW)) {
                    return id.substring(4)
                }
                var contentUri: Uri? = uri
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    contentUri =
                        ContentUris.withAppendedId(
                            Uri.parse(FConstants.URI_DOWNLOADS),
                            id!!.toLong(),
                        )
                }
                val path = getDataColumn(context, contentUri!!, null, null)

                // cant get file path by contentProvider, then we use stream to copy the file to cache
                if (path == null) {
                    val input: FileInputStream?
                    val output: FileOutputStream?
                    try {
                        val file = File(context.getCacheDir(), FConstants.URI_TEMP_FILE)
                        val filePath = file.getAbsolutePath()
                        val pfd = context.getContentResolver().openFileDescriptor(uri, "r")
                        if (pfd == null) {
                            return null
                        }
                        val fd = pfd.getFileDescriptor()
                        input = FileInputStream(fd)
                        output = FileOutputStream(filePath)
                        var read: Int
                        val bytes = ByteArray(FConstants.BUFF_SIZE)
                        while ((input.read(bytes).also { read = it }) != -1) {
                            output.write(bytes, 0, read)
                        }
                        input.close()
                        output.close()
                        return File(filePath).getAbsolutePath()
                    } catch (e: IOException) {
                    }
                }
                return path
            } else if (TextUtils.equals(FConstants.MEDIA_DOCUMENTS, uri.getAuthority())) {
                // MediaProvider
                val docId = DocumentsContract.getDocumentId(uri)
                val split: Array<String?> =
                    docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                var contentUri: Uri? = null
                if (TextUtils.equals(FConstants.URI_IMAGE, type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if (TextUtils.equals(FConstants.URI_VIDEO, type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if (TextUtils.equals(FConstants.URI_AUDIO, type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                val selectionArgs = arrayOf<String?>(split[1])
                return getDataColumn(
                    context,
                    contentUri!!,
                    FConstants.SELECTION,
                    selectionArgs,
                )
            }
        }
        if (ContentResolver.SCHEME_CONTENT == schema) {
            val contentResolver = context.getContentResolver()
            if ("com.google.android.apps.photos.contentprovider" == uri.getAuthority()) {
                val uriString = uri.getPathSegments().get(2)
                if (uriString.startsWith(ContentResolver.SCHEME_CONTENT)) {
                    return getDataColumn(context, Uri.parse(uriString), null, null)
                }
            } else if ("com.android.fileexplorer.myprovider" == uri.getAuthority() ||
                "com.mi.android.globalFileexplorer.myprovider" == uri.getAuthority()
            ) {
                val vPath = uri.getPath()
                if (!TextUtils.isEmpty(vPath)) {
                    val paths = ArrayList<String?>(uri.getPathSegments())
                    paths.removeAt(0)
                    val temp =
                        StringBuilder(Environment.getExternalStorageDirectory().getAbsolutePath())
                    for (item in paths) {
                        temp.append("/").append(item)
                    }
                    return temp.toString()
                }
            }

            var id = uri.getLastPathSegment()
            if (!TextUtils.isEmpty(id) && id!!.startsWith("/storage/emulated/")) {
                return id
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !TextUtils.isEmpty(id) &&
                id!!.contains(
                    ":",
                )
            ) {
                id = id.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
            }
            val filePath =
                getDataColumn(
                    context,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Images.Media._ID + FConstants.ID_SELECTION,
                    arrayOf<String?>(id),
                )
            return filePath
        }
        return null
    }

    private fun getDataColumn(
        context: Context,
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String?>?,
    ): String? {
        var cursor: Cursor? = null
        val projection = arrayOf<String?>(FConstants.DATA_COLUMN)

        try {
            cursor =
                context.getContentResolver().query(
                    uri,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                )
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(FConstants.DATA_COLUMN)
                return cursor.getString(index)
            }
        } catch (e: IllegalArgumentException) {
        } finally {
            if (cursor != null) {
                cursor.close()
            }
        }
        return null
    }
}
