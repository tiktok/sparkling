// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

import com.tiktok.sparkling.method.registry.core.annotation.DefaultType
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import java.lang.reflect.Method
import kotlin.reflect.KClass

/*
 * What [IDLMethodRegistryCache] reads off a method class once, so that every later call can bind
 * its payload without touching reflection again.
 *
 * Deviation from open source: upstream also declares an `IDLRegistryCache` singleton here holding
 * one process-wide cache. That singleton is not mirrored yet because the process-wide instance has
 * to be the TikTok subclass, which is the only one that can read the annotations prebuilt method
 * classes carry; a second singleton typed to this base would silently shadow it. TikTok therefore
 * keeps owning the global holder for now.
 */

/** Everything known about one method class: its two model classes and their fields. */
data class IDLAnnotationData(
    val paramClass: Class<*>,
    val resultClass: Class<*>,
    val methodParamModel: IDLAnnotationModel,
    val methodResultModel: IDLAnnotationModel,
    val models: Map<Class<out IDLMethodBaseModel>, IDLAnnotationModel> = hashMapOf(),
)

/** One model class's fields, indexed both by getter and by the key they sit under. */
data class IDLAnnotationModel(
    val methodModel: HashMap<Method, IDLParamField> = hashMapOf(),
    val stringModel: HashMap<String, IDLParamField> = hashMapOf(),
)

/** One field of a model class, read off its annotations. */
data class IDLParamField(
    val required: Boolean = false,
    val keyPath: String = "",
    val nestedClassType: KClass<out IDLMethodBaseModel> = IDLMethodBaseModel.Default::class,
    val primitiveClassType: KClass<out Any> = Any::class,
    val isEnum: Boolean = false,
    val isGetter: Boolean = true,
    val defaultValue: IDLDefaultValue = IDLDefaultValue(),
    val returnType: Class<*>,
    val stringEnum: List<String> = listOf(),
    val intEnum: List<Int> = listOf(),
)

/** A field's default, held as one slot per primitive type. [type] says which slot to read. */
data class IDLDefaultValue(
    val type: DefaultType = DefaultType.NONE,
    val doubleValue: Double = 0.0,
    val stringValue: String = "",
    val intValue: Int = 0,
    val boolValue: Boolean = false,
    val longValue: Long = 0L,
)
