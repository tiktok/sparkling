// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.hybridkit.service.api

interface IService {
    fun onRegister(bid: String)

    fun onUnRegister()

    fun getBid(): String
}
