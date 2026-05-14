// Copyright 2025 TikTok Inc.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.media.depend

class ChooseMediaParams(
    val mediaTypes: List<String>,
    val sourceType: String,
    val maxCount: Int = 1,
    val compressImage: Boolean = false,
    val saveToPhotoAlbum: Boolean = false,
    val cameraType: String = "",
    val compressWidth: Int = 216, // image witdth after compress, default 1080 / 5
    val compressHeight: Int = 384, // image height after compress, default 1920 / 5
    val compressQuality: Int = 0,
) {
    var isNeedCut: Boolean = false
    var cropRatioWidth: Int = 0
    var cropRatioHeight: Int = 0
    var needBase64Data: Boolean = false
    var compressOption: Int = CompressOptionsConstants.DEFAULT
    var permissionDenyAction: Int = PermissionDenyActionConstants.DEFAULT
    var isMultiSelect = false
    var useNewCompressSolution = false
}
