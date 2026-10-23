// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamModel
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseParamModel

/**
 * Finds the param model a bridge method declares, so a platform processor knows which shape to bind
 * the incoming parameters to.
 *
 * The model is a nested interface on the method class, marked with an annotation. Which annotation
 * is the seam: a host whose method classes are marked with its own is expected to call the overload
 * that takes it, because this is an `object` and cannot be subclassed.
 */
object IDLProxyClient {

    /** The param model [clazz] declares, read through [IDLMethodParamModel]. */
    fun retrieveParamModel(clazz: Class<out IDLBridgeMethod>): Class<out IDLMethodBaseParamModel>? =
        retrieveParamModel(clazz, IDLMethodParamModel::class.java)

    /**
     * The param model [clazz] declares, read through [annotation].
     *
     * The method's own nested classes are searched before its superclass's, so a method that
     * declares a model of its own is not answered with the one it inherits.
     */
    fun retrieveParamModel(
        clazz: Class<out IDLBridgeMethod>,
        annotation: Class<out Annotation>,
    ): Class<out IDLMethodBaseParamModel>? {
        var paramModelList = clazz.declaredClasses.filter { it.getAnnotation(annotation) != null }
        if (paramModelList.isEmpty()) {
            paramModelList = clazz.superclass.declaredClasses.filter { it.getAnnotation(annotation) != null }
        }

        if (paramModelList.isEmpty()) {
            return null
        } else {
            @Suppress("UNCHECKED_CAST")
            return paramModelList.first() as Class<out IDLMethodBaseParamModel>
        }
    }
}
