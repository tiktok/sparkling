// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.router.setBackPressIntercept

import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodName
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamField
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamModel
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodResultModel
import com.tiktok.sparkling.method.registry.core.base.AbsSparklingIDLMethod
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseParamModel
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseResultModel

abstract class AbsRouterSetBackPressInterceptMethodIDL :
    AbsSparklingIDLMethod<
        AbsRouterSetBackPressInterceptMethodIDL.IDLMethodSetBackPressInterceptParamModel,
        AbsRouterSetBackPressInterceptMethodIDL.IDLMethodSetBackPressInterceptResultModel,
    >() {
    @IDLMethodName(
        name = "router.setBackPressIntercept",
        params = ["intercept", "containerID"],
        results = ["supported"],
    )
    final override val name: String = "router.setBackPressIntercept"

    @IDLMethodParamModel
    interface IDLMethodSetBackPressInterceptParamModel : IDLMethodBaseParamModel {
        @get:IDLMethodParamField(required = true, isGetter = true, keyPath = "intercept")
        val intercept: Boolean

        @get:IDLMethodParamField(required = false, isGetter = true, keyPath = "containerID")
        val containerID: String?
    }

    @IDLMethodResultModel
    interface IDLMethodSetBackPressInterceptResultModel : IDLMethodBaseResultModel {
        /** False on a platform with no hardware back button, so a page can tell. */
        @get:IDLMethodParamField(required = true, isGetter = true, keyPath = "supported")
        @set:IDLMethodParamField(required = true, isGetter = false, keyPath = "supported")
        var supported: Boolean?
    }
}
