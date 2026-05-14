// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core.utils

import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import kotlin.reflect.KClass

@Deprecated(
    "This method has no containerID info. " +
        "Please use createXModel(containerID: String) to create.",
)
fun <T : IDLMethodBaseModel> Class<T>.createXModel(): T = IDLMethodResultModelArguments.createModel(this)

@Deprecated(
    "This method has no containerID info. " +
        "Please use createXModel(containerID: String) to create.",
)
fun <T : IDLMethodBaseModel> KClass<T>.createXModel(): T = IDLMethodResultModelArguments.createModel(this.java)

fun <T : IDLMethodBaseModel> Class<T>.createXModel(containerID: String?): T = IDLMethodResultModelArguments.createModel(this, containerID)

fun <T : IDLMethodBaseModel> KClass<T>.createXModel(containerID: String?): T = IDLMethodResultModelArguments.createModel(this.java, containerID)

/**
 * when the data is a pure map or list(every item is basic type)
 * use this to optimize performance
 */
fun Any.assignX(): IAssignDir<Any> = IAssignDir.Creator.create(this)
