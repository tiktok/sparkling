// Copyright 2025 TikTok Inc.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.media.choosemedia

import com.tiktok.sparkling.method.media.depend.ChooseMediaParams
import com.tiktok.sparkling.method.media.depend.ChooseMediaResults
import com.tiktok.sparkling.method.media.depend.IChooseMediaResultCallback
import com.tiktok.sparkling.method.media.depend.IHostMediaDepend
import com.tiktok.sparkling.method.media.utils.MediaProvider
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.model.idl.CompletionBlock
import com.tiktok.sparkling.method.registry.core.utils.createXModel
import java.util.Locale

/**
 */
class ChooseMediaMethod : AbsChooseMediaMethodIDL() {
    class FileInfo(
        val tempFilePath: String,
        val size: Long,
        val mediaType: String,
        val binaryData: ByteArray? = null,
    ) {
        var base64Data: String? = null
        var mimeType: String? = null
    }

    override fun handle(
        params: ChooseMediaInputModel,
        callback: CompletionBlock<ChooseMediaResultModel>,
        type: BridgePlatformType,
    ) {
        val sourceType = params.sourceType
        val cameraType = params.cameraType

        if (params.mediaTypes.isEmpty() || sourceType.isEmpty()) {
            return callback.onFailure(IDLBridgeMethod.INVALID_PARAM, "Invalid params: mediaTypes or sourceType is empty")
        }

        if (sourceType.lowercase(Locale.ROOT) == "camera" && cameraType.isNullOrEmpty()) {
            callback.onFailure(
                IDLBridgeMethod.INVALID_PARAM,
                "CameraType not provided with sourceType specified as camera in params",
            )
            return
        }

        val context =
            getSDKContext()?.context
                ?: return callback.onFailure(IDLBridgeMethod.FAIL, "Context not provided in host")

        val chooseMediaParams =
            ChooseMediaParams(
                params.mediaTypes,
                params.sourceType,
                params.maxCount.toInt(),
                params.compressImage,
                params.saveToPhotoAlbum,
                params.cameraType ?: "",
                params.compressWidth.toInt(),
                params.compressHeight.toInt(),
                params.compressQuality.toInt(),
            ).apply {
                isNeedCut = params.isNeedCut
                cropRatioHeight = params.cropRatioHeight.toInt()
                cropRatioWidth = params.cropRatioWidth.toInt()
                needBase64Data = params.needBase64Data
                compressOption = params.compressOption.toInt()
                permissionDenyAction = params.permissionDenyAction.toInt()
                isMultiSelect = params.isMultiSelect
                useNewCompressSolution = params.useNewCompressSolution
            }

        val chooseMediaCallback =
            object : IChooseMediaResultCallback {
                override fun onSuccess(
                    result: ChooseMediaResults,
                    msg: String,
                ) {
                    val tempFiles = mutableListOf<BridgeBeanChooseMediaTempFiles>()
                    result.tempFiles?.forEach {
                        tempFiles.add(
                            BridgeBeanChooseMediaTempFiles::class.java.createXModel().apply {
                                this.tempFilePath = it.tempFilePath
                                this.size = it.size
                                this.mediaType = it.mediaType
                                this.base64Data = it.base64Data
                                this.mimeType = it.mimeType
                            },
                        )
                    }

                    callback.onSuccess(
                        ChooseMediaResultModel::class.java.createXModel().apply {
                            this.tempFiles = tempFiles
                        },
                    )
                }

                override fun onFailure(
                    code: Int,
                    msg: String,
                ) {
                    callback.onFailure(code, msg)
                }
            }

        getMediaDependInstance()?.handleJsInvoke(context, chooseMediaParams, chooseMediaCallback)
            ?: callback.onFailure(IDLBridgeMethod.FAIL, "hostMediaDepend is null")
    }

    private fun getMediaDependInstance() = MediaProvider.hostMediaDepend
}
