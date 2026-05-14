// Copyright 2025 TikTok Inc.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.media.savedataurl

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Base64
import com.tiktok.sparkling.method.media.utils.BDMediaFileUtils
import com.tiktok.sparkling.method.registry.api.util.ThreadPool
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IBridgeContext
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.model.idl.CompletionBlock
import com.tiktok.sparkling.method.registry.core.utils.IDLMethodHelper
import com.tiktok.sparkling.method.registry.core.utils.createXModel
import com.tiktok.sparkling.method.runtime.depend.CommonDependsProvider
import com.tiktok.sparkling.method.runtime.depend.common.IHostPermissionDepend
import com.tiktok.sparkling.method.runtime.depend.common.OnPermissionsGrantCallback
import com.tiktok.sparkling.method.runtime.depend.common.OnPermissionsGrantResult
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ExecutorService

class SaveDataURLMethod : AbsSaveDataURLMethodIDL() {
    private val FORMAT_JPG = "jpg"
    private val FORMAT_PNG = "png"
    private val FORMAT_NONSUPPORT_TYPE = "nonsupportType"
    private val MIME_TYPE_IMAGE_JPEG = "image/jpeg"
    private val MIME_TYPE_IMAGE_PNG = "image/png"

    private fun getPermissionDependInstance(bridgeContext: IBridgeContext): IHostPermissionDepend? = CommonDependsProvider.hostPermissionDepend

    private fun getExecutorService(): ExecutorService = ThreadPool.getExecutorService()

    override fun handle(
        params: SaveDataURLInputModel,
        callback: CompletionBlock<SaveDataURLResultModel>,
        type: BridgePlatformType,
    ) {
        val bridgeContext = getSDKContext()
        val context =
            bridgeContext?.context ?: return callback.onFailure(
                IDLBridgeMethod.FAIL,
                "Context not provided in host",
            )
        val activity =
            IDLMethodHelper.getActivity(context) ?: return callback.onFailure(
                IDLBridgeMethod.FAIL,
                "context can not convert to activity",
            )
        if (params.dataURL.isEmpty()) {
            return callback.onFailure(IDLBridgeMethod.INVALID_PARAM, "The dataURL key is required.")
        }
        if (params.extension.isEmpty()) {
            return callback.onFailure(
                IDLBridgeMethod.INVALID_PARAM,
                "The extension key is required.",
            )
        }
        if (params.filename.isEmpty()) {
            return callback.onFailure(
                IDLBridgeMethod.INVALID_PARAM,
                "The filename key is required.",
            )
        }
        if (!params.dataURL.startsWith("data:")) {
            return callback.onFailure(IDLBridgeMethod.INVALID_PARAM, "dataURL invalid")
        }

        val realPermission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

        if (getPermissionDependInstance(bridgeContext)?.hasPermission(
                activity,
                realPermission,
            ) == true
        ) {
            handleSaveDataURL(context, params, callback)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            getPermissionDependInstance(bridgeContext)?.hasPermission(
                activity,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            ) == true
        ) {
            handleSaveDataURL(context, params, callback)
        } else {
            getPermissionDependInstance(bridgeContext)?.requestPermissions(
                activity,
                object : OnPermissionsGrantCallback {
                    override fun onResult(onPermissionsGrantResults: Array<OnPermissionsGrantResult>) {
                        if (onPermissionsGrantResults.isNotEmpty() &&
                            onPermissionsGrantResults[0].result == PackageManager.PERMISSION_GRANTED
                        ) {
                            handleSaveDataURL(context, params, callback)
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                            onPermissionsGrantResults.size == 2 &&
                            getPermissionDependInstance(bridgeContext)?.hasPermission(
                                activity,
                                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                            ) == true
                        ) {
                            handleSaveDataURL(context, params, callback)
                        } else {
                            callback.onFailure(IDLBridgeMethod.FAIL, "request permission denied")
                        }
                    }
                },
                arrayOf(
                    realPermission,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED else null,
                ).filterNotNull().toTypedArray(),
            )
        }
    }

    fun handleSaveDataURL(
        context: Context,
        params: SaveDataURLInputModel,
        callback: CompletionBlock<SaveDataURLResultModel>,
    ) {
        val dataURL = params.dataURL
        var base64 = ""
        if (dataURL.contains(",")) {
            base64 = dataURL.split(",")[1]
        } else {
            return callback.onFailure(IDLBridgeMethod.INVALID_PARAM)
        }
        if (base64.isEmpty()) {
            return callback.onFailure(IDLBridgeMethod.INVALID_PARAM)
        }
        val extension = params.extension
        val fileName = "${params.filename}.$extension"
        val saveFolder =
            BDMediaFileUtils.getCacheDir(context)?.absolutePath ?: return callback.onFailure(
                IDLBridgeMethod.FAIL,
                "cacheDir is null",
            )
        val file = File(saveFolder, fileName)
        val filePath = file.absolutePath
        if (file.exists()) {
            callback.onFailure(IDLBridgeMethod.FAIL, "file path already exist")
            return
        }
        val mimeType = getMimeTypeByExtension(extension)
        if (mimeType == FORMAT_NONSUPPORT_TYPE) {
            callback.onFailure(IDLBridgeMethod.INVALID_PARAM)
        } else {
            getExecutorService().execute {
                var fileOutputStream: FileOutputStream? = null
                try {
                    val bitmap = base64ToBitmap(base64)
                    if (bitmap == null) {
                        ThreadPool.runInMain {
                            callback.onFailure(IDLBridgeMethod.FAIL, "data generate failed")
                        }
                        return@execute
                    }
                    val compressFormat =
                        if (mimeType == MIME_TYPE_IMAGE_JPEG) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
                    fileOutputStream = FileOutputStream(filePath)
                    bitmap.compress(compressFormat, 100, fileOutputStream)
                    fileOutputStream.flush()
                    if (params.saveToAlbum != null) {
                        if (params.saveToAlbum == "image") {
                            val uri =
                                BDMediaFileUtils.copyFileToGallery(
                                    context,
                                    filePath,
                                    true,
                                    mimeType,
                                )
                            if (uri != null) {
                                ThreadPool.runInMain {
                                    callback.onSuccess(SaveDataURLResultModel::class.createXModel())
                                }
                            } else {
                                BDMediaFileUtils.removeFile(filePath)
                                ThreadPool.runInMain {
                                    callback.onFailure(IDLBridgeMethod.FAIL, "saveToAlbum error")
                                }
                            }
                        } else {
                            BDMediaFileUtils.removeFile(filePath)
                            ThreadPool.runInMain {
                                callback.onFailure(IDLBridgeMethod.INVALID_PARAM)
                            }
                        }
                    } else {
                        ThreadPool.runInMain {
                            callback.onSuccess(
                                SaveDataURLResultModel::class.createXModel().apply {
                                    this.filePath = filePath
                                },
                            )
                        }
                    }
                } catch (e: Exception) {
                    ThreadPool.runInMain {
                        callback.onFailure(
                            IDLBridgeMethod.FAIL,
                            e.message ?: "store file exception",
                        )
                    }
                } finally {
                    try {
                        fileOutputStream?.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun base64ToBitmap(dataUrl: String): Bitmap? {
        val decode = Base64.decode(dataUrl, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decode, 0, decode.size)
    }

    private fun getMimeTypeByExtension(extension: String): String =
        when (extension) {
            FORMAT_JPG -> MIME_TYPE_IMAGE_JPEG
            FORMAT_PNG -> MIME_TYPE_IMAGE_PNG
            else -> FORMAT_NONSUPPORT_TYPE
        }
}
