// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core.utils

import com.tiktok.sparkling.method.registry.core.DefaultIDLParamModelReader
import com.tiktok.sparkling.method.registry.core.IDLAnnotationData
import com.tiktok.sparkling.method.registry.core.IDLMethodRegistryCacheManager
import com.tiktok.sparkling.method.registry.core.IDLParamField
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import java.lang.reflect.Method

/**
 * Where [IDLMethodResultModelArguments] reads a result model from when the annotation cache has no
 * entry, and which cache it asks first.
 *
 * Hosts can carry result models annotated with their own set. [DefaultIDLResultModelReader]
 * reads the Sparkling annotations and [IDLMethodRegistryCacheManager].
 */
interface IDLResultModelReader {
    /** What the annotation cache of [containerID] holds for the method that produces [clazz]. */
    fun findResultAnnotationData(
        containerID: String?,
        clazz: Class<*>,
    ): IDLAnnotationData?

    /** The field annotation on [method], or null when it carries none. */
    fun readField(method: Method): IDLParamField?

    /** The options of the string enum annotation on [method], or null when it carries none. */
    fun readStringEnumOptions(method: Method): Array<out String>?

    /** The options of the int enum annotation on [method], or null when it carries none. */
    fun readIntEnumOptions(method: Method): IntArray?

    /** Whether [value] is a nested result model rather than a plain value. */
    fun isNestedModel(value: Any): Boolean

    /** The model type [IDLMethodResultModelArguments] casts a nested value to before converting it. */
    val modelClass: Class<out IDLMethodBaseModel>
}

/** Reads result models through the Sparkling annotations and cache. */
object DefaultIDLResultModelReader : IDLResultModelReader {
    override fun findResultAnnotationData(
        containerID: String?,
        clazz: Class<*>,
    ): IDLAnnotationData? =
        IDLMethodRegistryCacheManager
            .provideIDLMethodRegistryCache(containerID)
            ?.getAnnotationDataByResultClass(clazz)

    override fun readField(method: Method): IDLParamField? = DefaultIDLParamModelReader.readParamField(method)

    override fun readStringEnumOptions(method: Method): Array<out String>? = DefaultIDLParamModelReader.readStringEnumOptions(method)

    override fun readIntEnumOptions(method: Method): IntArray? = DefaultIDLParamModelReader.readIntEnumOptions(method)

    override fun isNestedModel(value: Any): Boolean = value is IDLMethodBaseModel

    override val modelClass: Class<out IDLMethodBaseModel> = IDLMethodBaseModel::class.java
}
