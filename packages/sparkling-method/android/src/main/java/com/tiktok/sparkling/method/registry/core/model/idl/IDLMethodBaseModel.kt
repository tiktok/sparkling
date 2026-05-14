// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core.model.idl

import androidx.annotation.Keep
import com.tiktok.sparkling.method.registry.core.exception.IDLMethodException
import org.json.JSONObject

/**
 * Desc:
 */
@Keep
interface IDLMethodBaseModel {
    /**
     * Serialize param model to JSONObject
     * @return JSONObject after performing serialization
     */
    @Keep
    fun toJSON(): JSONObject

    @Keep
    @Throws(IDLMethodException::class)
    fun convert(): Map<String, Any>?

    class Default : IDLMethodBaseModel {
        override fun toJSON(): JSONObject = JSONObject()

        override fun convert(): Map<String, Any>? = mapOf()
    }
}
