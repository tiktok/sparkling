// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseParamModel
import java.lang.reflect.Method
import kotlin.reflect.KClass

/**
 * Where a platform processor reads a method's param model from: the annotation cache first, and the
 * annotations on the model class when the cache has no entry.
 *
 * It is the one place a processor touches annotations, because a host can carry method classes
 * annotated with its own set. TikTok does: its prebuilt method classes name
 * `com.bytedance.sdk.xbridge.registry.core.annotation` in their bytecode, so TikTok supplies its own
 * reader and keeps the processors themselves shared. [DefaultIDLParamModelReader] reads the
 * annotations of this package.
 */
interface IDLParamModelReader {

    /** What the annotation cache of [containerID] holds for [clazz], or null when it holds nothing. */
    fun findAnnotationData(containerID: String?, clazz: Class<out IDLBridgeMethod>): IDLAnnotationData?

    /** The param model [clazz] declares, or null when it declares none. */
    fun retrieveParamModel(clazz: Class<out IDLBridgeMethod>): Class<out IDLMethodBaseParamModel>?

    /**
     * The field annotation on [method], or null when it carries none.
     *
     * The enum options are left empty: a processor reads them through [readStringEnumOptions] and
     * [readIntEnumOptions], which keep "no enum annotation" apart from "an empty one".
     */
    fun readParamField(method: Method): IDLParamField?

    /** The options of the string enum annotation on [method], or null when it carries none. */
    fun readStringEnumOptions(method: Method): Array<out String>?

    /** The options of the int enum annotation on [method], or null when it carries none. */
    fun readIntEnumOptions(method: Method): IntArray?

    /** Whether [type] is the sentinel meaning "this field holds no nested model". */
    fun isDefaultNestedType(type: KClass<*>?): Boolean
}
