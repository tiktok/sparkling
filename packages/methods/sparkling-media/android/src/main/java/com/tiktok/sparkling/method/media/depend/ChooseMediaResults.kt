// Copyright 2025 TikTok Inc.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.media.depend

class ChooseMediaResults {
    class FileInfo(
        val tempFilePath: String,
        val size: Long,
        val mediaType: String,
        val binaryData: ByteArray? = null,
    ) {
        var base64Data: String? = null
        var mimeType: String? = null
    }

    var tempFiles: List<FileInfo>? = null
}
