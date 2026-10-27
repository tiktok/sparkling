// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core.model.idl

import androidx.annotation.Keep

/**
 * Callback a bridge method uses to report success or failure.
 */
@Keep
interface CompletionBlock<S> where S : IDLMethodBaseResultModel {
    fun onSuccess(
        result: S,
        msg: String = "",
    )

    fun onFailure(
        code: Int,
        msg: String = "",
        data: S? = null,
    )
}
