// Copyright 2025 TikTok Inc.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.media.uploadimage

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.tiktok.sparkling.method.media.utils.AppFileUtils
import com.tiktok.sparkling.method.media.utils.UploadImageParamsOptionConstant
import com.tiktok.sparkling.method.media.utils.UploadImageResponse
import com.tiktok.sparkling.method.registry.api.util.ThreadPool
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.model.idl.CompletionBlock
import com.tiktok.sparkling.method.registry.core.utils.IDLMethodHelper
import com.tiktok.sparkling.method.registry.core.utils.JsonUtils
import com.tiktok.sparkling.method.registry.core.utils.createXModel
import com.tiktok.sparkling.method.runtime.depend.CommonDependsProvider
import com.tiktok.sparkling.method.runtime.depend.common.IHostNetworkDepend
import com.tiktok.sparkling.method.runtime.depend.common.IHostPermissionDepend
import com.tiktok.sparkling.method.runtime.depend.common.OnPermissionsGrantCallback
import com.tiktok.sparkling.method.runtime.depend.common.OnPermissionsGrantResult
import com.tiktok.sparkling.method.runtime.depend.utils.BridgeAPIRequestUtils
import com.tiktok.sparkling.method.runtime.depend.utils.IResponseCallback
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ExecutorService

/**
 */
class UploadImageMethod : AbsUploadImageMethodIDL() {
    private final val tag = "UploadImageMethod"

    private fun getExecutorService(): ExecutorService = ThreadPool.getExecutorService()

    private fun getPermissionDependInstance(): IHostPermissionDepend? = CommonDependsProvider.hostPermissionDepend

    private fun getNetworkDependInstance(): IHostNetworkDepend? = CommonDependsProvider.hostNetworkDepend

    override fun handle(
        params: UploadImageParamModel,
        callback: CompletionBlock<UploadImageResultModel>,
        type: BridgePlatformType,
    ) {
        val context =
            getSDKContext()?.context ?: return callback.onFailure(
                IDLBridgeMethod.FAIL,
                "Context not provided in host",
            )
        val activity =
            IDLMethodHelper.getActivity(context) ?: return callback.onFailure(
                IDLBridgeMethod.FAIL,
                "context can not convert to activity",
            )

        if (params.filePath.isNullOrEmpty() && params.formDataBody == null) {
            return callback.onFailure(
                IDLBridgeMethod.INVALID_PARAM,
                "Invalid params: no filepath or formDataBody",
            )
        }

        val hasPermission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getPermissionDependInstance()?.hasPermission(
                    activity,
                    Manifest.permission.READ_MEDIA_IMAGES,
                ) ?: false
            } else {
                getPermissionDependInstance()?.hasPermission(
                    activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                ) ?: false
            }

        if (hasPermission) {
            handleUploadFile(context, params, callback)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            getPermissionDependInstance()?.hasPermission(
                activity,
                Manifest.permission.READ_MEDIA_IMAGES,
            ) == true
        ) {
            handleUploadFile(context, params, callback)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            getPermissionDependInstance()?.hasPermission(
                activity,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            ) == true
        ) {
            handleUploadFile(context, params, callback)
        } else {
            val realPermissions =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED else null,
                    ).filterNotNull().toTypedArray()
                } else {
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }

            getPermissionDependInstance()?.requestPermissions(
                activity,
                object :
                    OnPermissionsGrantCallback {
                    override fun onResult(onPermissionsGrantResults: Array<OnPermissionsGrantResult>) {
                        if (onPermissionsGrantResults.isNotEmpty() && onPermissionsGrantResults[0].result == PackageManager.PERMISSION_GRANTED) {
                            handleUploadFile(context, params, callback)
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                            onPermissionsGrantResults.size == 2 &&
                            getPermissionDependInstance()?.hasPermission(
                                activity,
                                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                            ) == true
                        ) {
                            handleUploadFile(context, params, callback)
                        } else {
                            callback.onFailure(IDLBridgeMethod.FAIL, "request permission denied")
                        }
                    }
                },
                realPermissions,
            ) ?: run {
                callback.onFailure(IDLBridgeMethod.FAIL, "uploadImageDepend is null")
            }
        }
    }

    private fun handleUploadFile(
        context: Context,
        uploadParams: UploadImageParamModel,
        callback: CompletionBlock<UploadImageResultModel>,
    ) {
        val postFilePart = getPostFilePart(context, uploadParams, callback) ?: return
        getExecutorService().execute {
            val headers = BridgeAPIRequestUtils.filterHeaderEmptyValue(uploadParams.header)
            val params = BridgeAPIRequestUtils.convertParamValueToString(uploadParams.params)
            val paramsOption = uploadParams.paramsOption

            val responseCallback =
                object : IResponseCallback {
                    override fun onSuccess(
                        body: JSONObject,
                        responseHeader: LinkedHashMap<String, String>,
                        statusCode: Int?,
                        clientCode: Int?,
                    ) {
                        try {
                            val uploadImageResponse =
                                JsonUtils.fromJson(
                                    body.toString(),
                                    UploadImageResponse::class.java,
                                )
                            val avatarUri = uploadImageResponse.data
                            val response =
                                mutableMapOf<String, Any>().apply {
                                    body.keys().forEach { key ->
                                        this[key] = body.get(key)
                                    }
                                }
                            val url = avatarUri?.urlList?.takeIf { it.isNotEmpty() }?.firstOrNull()
                            callback.onSuccess(
                                UploadImageResultModel::class.createXModel().apply {
                                    this.url = url
                                    uri = avatarUri?.uri
                                    this.response = response
                                    this.clientCode = clientCode
                                },
                            )
                        } catch (throwable: Throwable) {
                            Log.e(tag, "parse post response body failed", throwable)
                        }
                    }

                    override fun onFailed(
                        errorCode: Int?,
                        clientCode: Int?,
                        throwable: Throwable,
                    ) {
                        callback.onFailure(
                            IDLBridgeMethod.FAIL,
                            throwable.message ?: "",
                            UploadImageResultModel::class.createXModel().apply {
                                this.url = url
                                this.clientCode = clientCode
                            },
                        )
                    }
                }
            val networkDepend = getNetworkDependInstance()
            if (networkDepend == null) {
                callback.onFailure(IDLBridgeMethod.FAIL, "networkDepend is null")
                return@execute
            }
            if (paramsOption == UploadImageParamsOptionConstant.DEFAULT ||
                paramsOption == UploadImageParamsOptionConstant.ADD_TO_REQUEST
            ) {
                BridgeAPIRequestUtils.post(
                    uploadParams.url,
                    headers,
                    postFilePart,
                    params,
                    responseCallback,
                    networkDepend,
                )
            } else {
                BridgeAPIRequestUtils.post(
                    uploadParams.url,
                    headers,
                    postFilePart,
                    HashMap<String, String>(),
                    responseCallback,
                    networkDepend,
                )
            }
        }
    }

    private fun getPostFilePart(
        context: Context,
        uploadParams: UploadImageParamModel,
        callback: CompletionBlock<UploadImageResultModel>,
    ): LinkedHashMap<String, File>? {
        val dataBody = uploadParams.formDataBody
        when {
            dataBody !== null -> {
                val pairList =
                    dataBody.map {
                        val file = checkPath(context, it.value, callback, it.key) ?: return null
                        Pair(it.key, file)
                    }
                val map = linkedMapOf<String, File>()
                pairList.forEach {
                    map[it.first] = it.second
                }
                return map
            }

            !uploadParams.filePath.isNullOrEmpty() -> {
                val file =
                    checkPath(context, uploadParams.filePath, callback, "filePath") ?: return null
                return linkedMapOf("file" to file)
            }

            else -> {
                callback.onFailure(
                    IDLBridgeMethod.INVALID_PARAM,
                    "filePath or formDataBody can not be null.",
                )
                return null
            }
        }
    }

    private fun checkPath(
        context: Context,
        url: String?,
        callback: CompletionBlock<UploadImageResultModel>,
        key: String,
    ): File? {
        if (url.isNullOrEmpty()) {
            callback.onFailure(
                IDLBridgeMethod.INVALID_PARAM,
                "The file path should not be empty.The key is $key",
            )
            return null
        }
        val path = AppFileUtils.getOrCopiedFilePath(context, url)
        if (path.isNullOrEmpty()) {
            callback.onFailure(IDLBridgeMethod.NOT_FOUND, "File is not exist.The key is $key")
            return null
        }
        val file = File(path)
        if (!file.exists()) {
            callback.onFailure(IDLBridgeMethod.NOT_FOUND, "File is not exist.The key is $key")
            return null
        }
        if (!file.isFile) {
            callback.onFailure(IDLBridgeMethod.NOT_FOUND, "File is not file.The key is $key")
            return null
        }
        return file
    }
}
