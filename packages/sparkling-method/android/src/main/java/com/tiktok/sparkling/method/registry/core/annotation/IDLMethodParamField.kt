// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core.annotation

import androidx.annotation.Keep
import com.tiktok.sparkling.method.registry.core.IDLDefaultValue
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import kotlin.reflect.KClass

/**
 * Desc:
 */

@Target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class IDLMethodParamField(
    val required: Boolean = false,
    val keyPath: String = "",
    val nestedClassType: KClass<out IDLMethodBaseModel> = IDLMethodBaseModel.Default::class,
    val primitiveClassType: KClass<out Any> = Any::class,
    val isEnum: Boolean = false,
    val isGetter: Boolean = true,
    val defaultValue: MethodParamDefaultValue = MethodParamDefaultValue(),
)

@Keep
enum class DefaultType {
    STRING,
    DOUBLE,
    INT,
    LONG,
    BOOL,
    NONE,
}

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class MethodParamDefaultValue(
    val type: DefaultType = DefaultType.NONE,
    val doubleValue: Double = 0.0,
    val stringValue: String = "",
    val intValue: Int = 0,
    val boolValue: Boolean = false,
    val longValue: Long = 0L,
)
