// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core.annotation

import androidx.annotation.Keep
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import kotlin.reflect.KClass

/**
 * Describes one field of a param or result model: where it sits in the payload, whether it is
 * required, and what to fall back to when the caller leaves it out.
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

/** Which of [MethodParamDefaultValue]'s value slots carries the default, if any. */
@Keep
enum class DefaultType {
    STRING,
    DOUBLE,
    INT,
    LONG,
    BOOL,
    NONE,
}

/**
 * A default held as one slot per primitive type, because an annotation member cannot be typed as a
 * union. [type] says which slot to read.
 */
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
