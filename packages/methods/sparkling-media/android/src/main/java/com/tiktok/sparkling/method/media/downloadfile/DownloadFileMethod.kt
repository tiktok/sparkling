// Copyright 2025 TikTok Inc.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.media.downloadfile

import android.Manifest
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.tiktok.sparkling.method.media.utils.BDMediaFileUtils
import com.tiktok.sparkling.method.media.utils.BDMediaFileUtils.getCacheDir
import com.tiktok.sparkling.method.media.utils.Md5Utils
import com.tiktok.sparkling.method.media.utils.MediaProvider
import com.tiktok.sparkling.method.registry.api.util.ThreadPool
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.model.idl.CompletionBlock
import com.tiktok.sparkling.method.registry.core.utils.createXModel
import com.tiktok.sparkling.method.runtime.depend.CommonDependsProvider
import com.tiktok.sparkling.method.runtime.depend.common.IHostNetworkDepend
import com.tiktok.sparkling.method.runtime.depend.common.IHostPermissionDepend
import com.tiktok.sparkling.method.runtime.depend.common.OnPermissionsGrantCallback
import com.tiktok.sparkling.method.runtime.depend.common.OnPermissionsGrantResult
import com.tiktok.sparkling.method.runtime.depend.network.AbsStreamConnection
import com.tiktok.sparkling.method.runtime.depend.utils.BridgeAPIRequestUtils
import com.tiktok.sparkling.method.runtime.depend.utils.IStreamResponseCallback
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class DownloadFileMethod : AbsDownloadFileMethodIDL() {
    private val tag = "DownloadFileMethod"

    private fun getNetworkDependInstance(): IHostNetworkDepend? = CommonDependsProvider.hostNetworkDepend

    private fun getPermissionDepend(): IHostPermissionDepend? = CommonDependsProvider.hostPermissionDepend

    override fun handle(
        params: DownloadFileInputModel,
        callback: CompletionBlock<DownloadFileResultModel>,
        type: BridgePlatformType,
    ) {
        val context =
            getSDKContext()?.context ?: return callback.onFailure(
                IDLBridgeMethod.FAIL,
                "Context not provided in host",
            )
        if (params.url.isEmpty()) {
            return callback.onFailure(
                IDLBridgeMethod.INVALID_PARAM,
                "The url in params is empty.",
            )
        }
        if (params.extension.isEmpty()) {
            return callback.onFailure(
                IDLBridgeMethod.INVALID_PARAM,
                "The extension in params is empty.",
            )
        }
        handleDownloadFile(context, params, callback, type)
    }

    private fun handleDownloadFile(
        context: Context,
        downloadParams: DownloadFileInputModel,
        callback: CompletionBlock<DownloadFileResultModel>,
        type: BridgePlatformType,
    ) {
        val xBridgeType =
            when (type) {
                BridgePlatformType.WEB -> BridgePlatformType.WEB
                BridgePlatformType.LYNX -> BridgePlatformType.LYNX
                BridgePlatformType.ALL -> BridgePlatformType.ALL
                else -> BridgePlatformType.ALL
            }
        val prefix = Md5Utils.hexDigest(downloadParams.url) + System.currentTimeMillis()
        val extension = downloadParams.extension
        val fileName = "$prefix.$extension"
        val saveFolder =
            getCacheDir(context)?.absolutePath ?: return callback.onFailure(
                IDLBridgeMethod.FAIL,
                "cacheDir is null",
            )
        val filePath = "$saveFolder/$fileName"
        if (File(filePath).exists()) {
            ThreadPool.runInMain {
                callback.onFailure(IDLBridgeMethod.FAIL, "file path already exist")
            }
            return
        }

        ThreadPool.runInBackGround {
            val targetUrl =
                BridgeAPIRequestUtils.addParametersToUrl(
                    downloadParams.url,
                    downloadParams.params,
                    xBridgeType,
                )
            val headers = BridgeAPIRequestUtils.filterHeaderEmptyValue(downloadParams.header)

            val responseCallback =
                object : IStreamResponseCallback {
                    override fun handleConnection(connection: AbsStreamConnection?) {
                        if (connection == null) {
                            Log.d(tag, "connection is null")
                            ThreadPool.runInMain {
                                callback.onFailure(IDLBridgeMethod.FAIL, "connection failed")
                            }
                            return
                        }

                        val errorMsg: String? =
                            connection.getErrorMsg().takeIf { it.isNotEmpty() }
                                ?: connection.getException()?.message?.takeIf { it.isNotEmpty() }
                        val body = connection.getInputStreamResponseBody()

                        if (body == null) {
                            Log.d(tag, "body is null")
                            ThreadPool.runInMain {
                                callback.onFailure(IDLBridgeMethod.FAIL, errorMsg ?: "body is null")
                            }
                            return
                        }
                        val respCode = connection.getResponseCode()
                        val respHeader = connection.getResponseHeader()
                        val clientCode = connection.getClientCode()
                        var bufferedInputStream: BufferedInputStream? = null
                        val outputStream: FileOutputStream
                        var bufferedOutputStream: BufferedOutputStream? = null

                        try {
                            bufferedInputStream = BufferedInputStream(body)
                            outputStream = FileOutputStream(filePath)
                            bufferedOutputStream = BufferedOutputStream(outputStream)

                            var len: Int
                            val bytes = ByteArray(4096)

                            while (bufferedInputStream.read(bytes).also { len = it } != -1) {
                                bufferedOutputStream.write(bytes, 0, len)
                            }
                            bufferedOutputStream.flush()
                            if (downloadParams.saveToAlbum != null) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(
                                        context,
                                        WRITE_EXTERNAL_STORAGE,
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    handleAndCallbackSuccess(
                                        context,
                                        filePath,
                                        downloadParams.saveToAlbum,
                                        respCode,
                                        clientCode,
                                        respHeader,
                                        callback,
                                    )
                                    return
                                } else {
                                    val act =
                                        getSDKContext()?.ownerActivity ?: return callback.onFailure(
                                            IDLBridgeMethod.FAIL,
                                            "activity is null",
                                        )
                                    val permissionDepend = getPermissionDepend()
                                    permissionDepend?.let {
                                        permissionDepend.requestPermissions(
                                            act,
                                            object : OnPermissionsGrantCallback {
                                                override fun onResult(onPermissionsGrantResults: Array<OnPermissionsGrantResult>) {
                                                    if (onPermissionsGrantResults.isNotEmpty() &&
                                                        onPermissionsGrantResults[0].result == PackageManager.PERMISSION_GRANTED
                                                    ) {
                                                        handleWhenGranted()
                                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                                                        onPermissionsGrantResults.size == 2 &&
                                                        getPermissionDepend()?.hasPermission(
                                                            act,
                                                            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                                                        ) == true
                                                    ) {
                                                        handleWhenGranted()
                                                    } else {
                                                        ThreadPool.runInMain {
                                                            callback.onFailure(
                                                                IDLBridgeMethod.UNAUTHORIZED_ACCESS,
                                                                "no permission for album",
                                                            )
                                                        }
                                                    }
                                                }

                                                fun handleWhenGranted() {
                                                    try {
                                                        handleAndCallbackSuccess(
                                                            context,
                                                            filePath,
                                                            downloadParams.saveToAlbum,
                                                            respCode,
                                                            clientCode,
                                                            respHeader,
                                                            callback,
                                                        )
                                                        return
                                                    } catch (e: Exception) {
                                                        ThreadPool.runInMain {
                                                            callback.onFailure(
                                                                IDLBridgeMethod.FAIL,
                                                                e.message ?: "store file exception",
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            arrayOf(WRITE_EXTERNAL_STORAGE),
                                        )
                                        return
                                    }
                                    ThreadPool.runInMain {
                                        callback.onFailure(
                                            IDLBridgeMethod.UNAUTHORIZED_ACCESS,
                                            "no permission for album",
                                        )
                                    }
                                }
                            }
                            ThreadPool.runInMain {
                                callback.onSuccess(
                                    DownloadFileResultModel::class.java.createXModel().apply {
                                        this.httpCode = respCode
                                        this.clientCode = clientCode
                                        this.header = respHeader
                                        this.filePath = filePath
                                    },
                                )
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
                                connection.cancel()
                                bufferedInputStream?.close()
                                bufferedOutputStream?.close()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }

            val hostNetworkDepend = getNetworkDependInstance()
            if (hostNetworkDepend == null) {
                Log.d(tag, "hostNetworkDepend is null")
                ThreadPool.runInMain {
                    callback.onFailure(IDLBridgeMethod.FAIL, "hostNetworkDepend is null")
                }
            } else {
                BridgeAPIRequestUtils.downloadFile(
                    targetUrl,
                    headers,
                    downloadParams.needCommonParams,
                    responseCallback,
                    hostNetworkDepend,
                )
            }
        }
    }

    private fun handleAndCallbackSuccess(
        context: Context,
        filePath: String,
        saveToAlbum: String?,
        respCode: Int,
        clientCode: Int,
        respHeader: Map<String, String>,
        callback: CompletionBlock<DownloadFileResultModel>,
    ) {
        val uri = BDMediaFileUtils.copyFileToGallery(context, filePath, saveToAlbum == "image")
        ThreadPool.runInMain {
            callback.onSuccess(
                DownloadFileResultModel::class.java.createXModel().apply {
                    this.httpCode = respCode
                    this.clientCode = clientCode
                    this.header = respHeader
                    this.filePath = uri.toString()
                },
            )
        }
    }
}
