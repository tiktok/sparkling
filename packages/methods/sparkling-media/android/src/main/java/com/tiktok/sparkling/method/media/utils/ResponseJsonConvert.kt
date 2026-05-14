// Copyright 2025 TikTok Inc.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.media.utils

import com.google.gson.annotations.SerializedName

class AvatarUri {
    @SerializedName("uri")
    var uri: String? = null

    @SerializedName("url_list")
    var urlList: List<String>? = null
}

/**
 * @property data AvatarUri?
 */
class UploadImageResponse {
    @SerializedName("data")
    var data: AvatarUri? = null
}
