// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool.getDevUrl

import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodName
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamField
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamModel
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodResultModel
import com.tiktok.sparkling.method.registry.core.base.AbsSparklingIDLMethod
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseParamModel
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseResultModel

abstract class AbsGetDevUrlMethodIDL :
    AbsSparklingIDLMethod<
        AbsGetDevUrlMethodIDL.IDLMethodGetDevUrlParamModel,
        AbsGetDevUrlMethodIDL.IDLMethodGetDevUrlResultModel,
    >() {
    @IDLMethodName(name = "debugtool.getDevUrl", results = ["url"])
    final override val name: String = "debugtool.getDevUrl"

    @IDLMethodParamModel
    interface IDLMethodGetDevUrlParamModel : IDLMethodBaseParamModel

    @IDLMethodResultModel
    interface IDLMethodGetDevUrlResultModel : IDLMethodBaseResultModel {
        @get:IDLMethodParamField(required = true, isGetter = true, keyPath = "url")
        @set:IDLMethodParamField(required = true, isGetter = false, keyPath = "url")
        var url: String
    }
}
