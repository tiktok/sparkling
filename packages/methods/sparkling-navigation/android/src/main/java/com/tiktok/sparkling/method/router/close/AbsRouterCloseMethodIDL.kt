// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.router.close

import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodName
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamField
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamModel
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodResultModel
import com.tiktok.sparkling.method.registry.core.base.AbsSparklingIDLMethod
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseParamModel
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseResultModel

abstract class AbsRouterCloseMethodIDL : AbsSparklingIDLMethod<AbsRouterCloseMethodIDL.IDLMethodCloseParamModel, AbsRouterCloseMethodIDL.IDLMethodCloseResultModel>() {
    @IDLMethodName(name = "router.close", params = ["containerID", "animated"])
    final override val name: String = "router.close"

    @IDLMethodParamModel
    interface IDLMethodCloseParamModel : IDLMethodBaseParamModel {
        @get:IDLMethodParamField(required = false, isGetter = true, keyPath = "containerID")
        val containerID: String

        @get:IDLMethodParamField(required = false, isGetter = true, keyPath = "animated")
        val animated: Boolean?
    }

    @IDLMethodResultModel
    interface IDLMethodCloseResultModel : IDLMethodBaseResultModel
}
