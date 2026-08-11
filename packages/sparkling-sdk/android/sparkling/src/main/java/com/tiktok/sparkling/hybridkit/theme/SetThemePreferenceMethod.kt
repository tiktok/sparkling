// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.hybridkit.theme

import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodName
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamField
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamModel
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodResultModel
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodStringEnum
import com.tiktok.sparkling.method.registry.core.base.AbsSparklingIDLMethod
import com.tiktok.sparkling.method.registry.core.model.idl.CompletionBlock
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseParamModel
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseResultModel
import com.tiktok.sparkling.method.registry.core.utils.createXModel

abstract class AbsSetThemePreferenceMethod :
    AbsSparklingIDLMethod<
        AbsSetThemePreferenceMethod.ParamModel,
        AbsSetThemePreferenceMethod.ResultModel,
    >() {
    @IDLMethodName(
        name = METHOD_NAME,
        params = ["preference"],
        results = ["preference"],
    )
    final override val name: String = METHOD_NAME

    @IDLMethodParamModel
    interface ParamModel : IDLMethodBaseParamModel {
        @get:IDLMethodStringEnum(
            ThemePreference.FOLLOW_SYSTEM_VALUE,
            ThemePreference.LIGHT_VALUE,
            ThemePreference.DARK_VALUE,
        )
        @get:IDLMethodParamField(
            required = true,
            isGetter = true,
            keyPath = "preference",
            isEnum = true,
        )
        val preference: String
    }

    @IDLMethodResultModel
    interface ResultModel : IDLMethodBaseResultModel {
        @get:IDLMethodParamField(
            required = true,
            isGetter = true,
            keyPath = "preference",
        )
        @set:IDLMethodParamField(
            required = true,
            isGetter = false,
            keyPath = "preference",
        )
        var preference: String
    }

    companion object {
        const val METHOD_NAME = "sparkling.setThemePreference"
    }
}

class SetThemePreferenceMethod : AbsSetThemePreferenceMethod() {
    override fun handle(
        params: ParamModel,
        callback: CompletionBlock<ResultModel>,
        type: BridgePlatformType,
    ) {
        val preference =
            ThemePreference.fromValue(params.preference)
                ?: return callback.onFailure(
                    IDLBridgeMethod.INVALID_PARAM,
                    "preference must be follow-system, light, or dark",
                )
        val context =
            getSDKContext()?.context
                ?: return callback.onFailure(
                    IDLBridgeMethod.FAIL,
                    "Context not provided in host",
                )

        ThemePreferenceManager.setPreference(context, preference)
        callback.onSuccess(
            ResultModel::class.java
                .createXModel(getSDKContext()?.containerID)
                .apply { this.preference = preference.value },
        )
    }
}
