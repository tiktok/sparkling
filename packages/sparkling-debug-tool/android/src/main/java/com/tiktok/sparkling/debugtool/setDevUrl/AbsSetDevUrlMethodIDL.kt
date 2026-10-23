// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool.setDevUrl

import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodName
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamField
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamModel
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodResultModel
import com.tiktok.sparkling.method.registry.core.base.AbsSparklingIDLMethod
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseParamModel
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseResultModel

abstract class AbsSetDevUrlMethodIDL :
    AbsSparklingIDLMethod<
        AbsSetDevUrlMethodIDL.IDLMethodSetDevUrlParamModel,
        AbsSetDevUrlMethodIDL.IDLMethodSetDevUrlResultModel,
    >() {
    @IDLMethodName(name = "debugtool.setDevUrl", params = ["url"])
    final override val name: String = "debugtool.setDevUrl"

    @IDLMethodParamModel
    interface IDLMethodSetDevUrlParamModel : IDLMethodBaseParamModel {
        @get:IDLMethodParamField(required = false, isGetter = true, keyPath = "url")
        val url: String?
    }

    @IDLMethodResultModel
    interface IDLMethodSetDevUrlResultModel : IDLMethodBaseResultModel
}
