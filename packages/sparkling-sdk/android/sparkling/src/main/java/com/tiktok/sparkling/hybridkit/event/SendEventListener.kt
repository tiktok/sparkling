// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.event

import com.tiktok.sparkling.hybridkit.api.AbsDependencyIterator
import com.tiktok.sparkling.hybridkit.base.IKitView

typealias SendEventListener = ((kitView: IKitView, eventName: String, params: Any?) -> Unit)

abstract class AbsSendEventListener :
    AbsDependencyIterator<AbsSendEventListener>(),
    SendEventListener {
    abstract override fun invoke(
        kitView: IKitView,
        eventName: String,
        params: Any?,
    )
}
