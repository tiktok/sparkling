// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

import com.tiktok.sparkling.method.registry.core.annotation.DefaultType
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Desc:
 */

object IDLRegistryCache {
    const val TAG = "IDLRegistryCache"

    private val IDLMethodRegistryCache = IDLMethodRegistryCache()
    val BRIDGE_ANNOTATION_MAP: ConcurrentHashMap<Class<out IDLBridgeMethod>, IDLAnnotationData>
        get() = IDLMethodRegistryCache.BRIDGE_ANNOTATION_MAP
    val BRIDGE_RESULT_MAP: ConcurrentHashMap<Class<*>, Class<*>>
        get() = IDLMethodRegistryCache.BRIDGE_RESULT_MAP

    fun getRealIDLRegistryCache(): IDLMethodRegistryCache = IDLMethodRegistryCache

    @JvmStatic
    fun find(clazz: Class<out IDLBridgeMethod>): String = IDLMethodRegistryCache.find(clazz)

    fun getAnnotationDataByResultClass(resultModelClazz: Class<*>): IDLAnnotationData? {
        val clazz = BRIDGE_RESULT_MAP[resultModelClazz] ?: return null
        return BRIDGE_ANNOTATION_MAP[clazz]
    }
}

data class IDLAnnotationData(
    val paramClass: Class<*>,
    val resultClass: Class<*>,
    val methodParamModel: IDLAnnotationModel,
    val methodResultModel: IDLAnnotationModel,
    val models: Map<Class<out IDLMethodBaseModel>, IDLAnnotationModel> = hashMapOf(),
)

data class IDLAnnotationModel(
    val methodModel: HashMap<Method, IDLParamField> = hashMapOf(),
    val stringModel: HashMap<String, IDLParamField> = hashMapOf(),
)

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

data class IDLDefaultValue(
    val type: DefaultType = DefaultType.NONE,
    val doubleValue: Double = 0.0,
    val stringValue: String = "",
    val intValue: Int = 0,
    val boolValue: Boolean = false,
    val longValue: Long = 0L,
)
