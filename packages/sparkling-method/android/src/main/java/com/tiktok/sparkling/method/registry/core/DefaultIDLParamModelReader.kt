// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodIntEnum
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamField
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodStringEnum
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseParamModel
import java.lang.reflect.Method
import kotlin.reflect.KClass

/**
 * Reads param models through the annotations of `com.tiktok.sparkling.method.registry.core.annotation`
 * and the caches of [IDLMethodRegistryCacheManager].
 */
object DefaultIDLParamModelReader : IDLParamModelReader {

    override fun findAnnotationData(
        containerID: String?,
        clazz: Class<out IDLBridgeMethod>,
    ): IDLAnnotationData? {
        return IDLMethodRegistryCacheManager.provideIDLMethodRegistryCache(containerID)?.BRIDGE_ANNOTATION_MAP?.get(clazz)
    }

    override fun retrieveParamModel(clazz: Class<out IDLBridgeMethod>): Class<out IDLMethodBaseParamModel>? {
        return IDLProxyClient.retrieveParamModel(clazz)
    }

    override fun readParamField(method: Method): IDLParamField? {
        val annotation = method.getAnnotation(IDLMethodParamField::class.java) ?: return null
        return toIDLParamField(annotation, method.returnType)
    }

    override fun readStringEnumOptions(method: Method): Array<out String>? {
        return method.getAnnotation(IDLMethodStringEnum::class.java)?.option
    }

    override fun readIntEnumOptions(method: Method): IntArray? {
        return method.getAnnotation(IDLMethodIntEnum::class.java)?.option
    }

    override fun isDefaultNestedType(type: KClass<*>?): Boolean {
        return type == IDLMethodBaseModel.Default::class
    }

    /** [annotation] in the shape the cache and the processors share. [IDLMethodRegistryCache] passes the enum options. */
    internal fun toIDLParamField(
        annotation: IDLMethodParamField,
        returnType: Class<*>,
        stringEnum: List<String> = listOf(),
        intEnum: List<Int> = listOf(),
    ): IDLParamField {
        val defaultValue = annotation.defaultValue
        return IDLParamField(
            annotation.required,
            annotation.keyPath,
            annotation.nestedClassType,
            annotation.primitiveClassType,
            annotation.isEnum,
            annotation.isGetter,
            IDLDefaultValue(
                defaultValue.type,
                defaultValue.doubleValue,
                defaultValue.stringValue,
                defaultValue.intValue,
                defaultValue.boolValue,
                defaultValue.longValue,
            ),
            returnType,
            stringEnum,
            intEnum,
        )
    }
}
