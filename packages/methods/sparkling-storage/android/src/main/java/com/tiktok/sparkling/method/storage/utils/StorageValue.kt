// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.storage.utils

import com.google.gson.annotations.SerializedName

/**
 * data saved in sp
 * e.g. {type: BOOL, value: {age: 1}}
 */
internal data class StorageValue(
    @SerializedName("type") val type: String = "",
    @SerializedName("value") val value: String = "",
)
