// Copyright 2025 TikTok Inc.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.media.uploadfile

import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodIntEnum
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodName
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamField
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamModel
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodResultModel
import com.tiktok.sparkling.method.registry.core.base.AbsSparklingIDLMethod
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseParamModel
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseResultModel

abstract class AbsUploadFileMethodIDL : AbsSparklingIDLMethod<AbsUploadFileMethodIDL.UploadFileParamModel, AbsUploadFileMethodIDL.UploadFileResultModel>() {
    @IDLMethodName(
        name = "media.uploadFile",
        params = ["url", "filePath", "params", "header", "paramsOption", "formDataBody"],
        results = ["url", "uri", "response", "clientCode"],
    )
    final override val name: String = "media.uploadFile"

    @IDLMethodParamModel
    interface UploadFileParamModel : IDLMethodBaseParamModel {
        companion object {
            const val UPLOAD_FILE_PARAMS_OPTION_0 = 0
            const val UPLOAD_FILE_PARAMS_OPTION_1 = 1
            const val UPLOAD_FILE_PARAMS_OPTION_2 = 2
        }

        @get:IDLMethodParamField(required = true, isGetter = true, keyPath = "url")
        val url: String

        @get:IDLMethodParamField(required = false, isGetter = true, keyPath = "filePath")
        val filePath: String?

        @get:IDLMethodParamField(required = false, isGetter = true, keyPath = "params")
        val params: Map<String, Any>?

        @get:IDLMethodParamField(required = false, isGetter = true, keyPath = "header")
        val header: Map<String, Any>?

        @get:IDLMethodIntEnum(UPLOAD_FILE_PARAMS_OPTION_0, UPLOAD_FILE_PARAMS_OPTION_1, UPLOAD_FILE_PARAMS_OPTION_2)
        @get:IDLMethodParamField(required = false, isGetter = true, keyPath = "paramsOption", isEnum = true)
        val paramsOption: Number?

        @get:IDLMethodParamField(required = false, isGetter = true, keyPath = "formDataBody", nestedClassType = BridgeBeanUploadFileFormDataBody::class)
        val formDataBody: List<BridgeBeanUploadFileFormDataBody>?
    }

    @IDLMethodResultModel
    interface UploadFileResultModel : IDLMethodBaseResultModel {
        @get:IDLMethodParamField(required = false, isGetter = true, keyPath = "url")
        @set:IDLMethodParamField(required = false, isGetter = false, keyPath = "url")
        var url: String?

        @get:IDLMethodParamField(required = false, isGetter = true, keyPath = "uri")
        @set:IDLMethodParamField(required = false, isGetter = false, keyPath = "uri")
        var uri: String?

        @get:IDLMethodParamField(required = false, isGetter = true, keyPath = "response")
        @set:IDLMethodParamField(required = false, isGetter = false, keyPath = "response")
        var response: Map<String, Any>?

        @get:IDLMethodParamField(required = false, isGetter = true, keyPath = "clientCode")
        @set:IDLMethodParamField(required = false, isGetter = false, keyPath = "clientCode")
        var clientCode: Number?
    }

    interface BridgeBeanUploadFileFormDataBody : IDLMethodBaseModel {
        @get:IDLMethodParamField(required = true, isGetter = true, keyPath = "key")
        val key: String

        @get:IDLMethodParamField(required = true, isGetter = true, keyPath = "value")
        val value: String
    }
}
