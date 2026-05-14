// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamModel
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseParamModel

/**
 * Desc:
 */
object IDLProxyClient {
    fun retrieveParamModel(clazz: Class<out IDLBridgeMethod>): Class<out IDLMethodBaseParamModel>? {
        var paramModelList = clazz.declaredClasses.filter { it.getAnnotation(IDLMethodParamModel::class.java) != null }
        if (paramModelList.isEmpty()) {
            paramModelList = clazz.superclass.declaredClasses.filter { it.getAnnotation(IDLMethodParamModel::class.java) != null }
        }

        if (paramModelList.isEmpty()) {
            return null
        } else {
            return paramModelList.first() as Class<out IDLMethodBaseParamModel>
        }
    }
}
