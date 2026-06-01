// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.registry.api.processor

import com.lynx.react.bridge.JavaOnlyArray
import com.lynx.react.bridge.JavaOnlyMap
import com.tiktok.sparkling.method.registry.core.IDLAnnotationData
import com.tiktok.sparkling.method.registry.core.IDLAnnotationModel
import com.tiktok.sparkling.method.registry.core.IDLDefaultValue
import com.tiktok.sparkling.method.registry.core.IDLParamField
import com.tiktok.sparkling.method.registry.core.annotation.DefaultType
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LynxDataProcessorForMapNestedTest {
    interface NestedModel : IDLMethodBaseModel {
        fun getName(): String

        fun getCount(): Int
    }

    @Test
    fun proxyForNestedReadableMapImplementsAccessors() {
        val data = nestedAnnotationData()
        val nestedMap =
            JavaOnlyMap().apply {
                putString("name", "alice")
                putInt("count", 7)
            }
        val params = hashMapOf<String, Any>("nested" to nestedMap)

        val res = LynxDataProcessorForMap.getJavaOnlyMapParams(params, data)
        val proxy = res?.get("nested") as NestedModel
        assertEquals("alice", proxy.getName())
        assertEquals(7, proxy.getCount())

        // toJSON returns a JSONObject view of the map.
        val toJson = proxy.javaClass.getMethod("toJSON").invoke(proxy)
        assertNotNull(toJson)
    }

    @Test
    fun proxyForNestedReadableArrayMapsEachItemToProxy() {
        val data = nestedListAnnotationData()
        val arr =
            JavaOnlyArray().apply {
                pushMap(
                    JavaOnlyMap().apply {
                        putString("name", "a")
                        putInt("count", 1)
                    },
                )
                pushMap(
                    JavaOnlyMap().apply {
                        putString("name", "b")
                        putInt("count", 2)
                    },
                )
            }
        val params = hashMapOf<String, Any>("items" to arr)

        val res = LynxDataProcessorForMap.getJavaOnlyMapParams(params, data)

        @Suppress("UNCHECKED_CAST")
        val list = res?.get("items") as List<NestedModel>
        assertEquals(2, list.size)
        assertEquals("a", list[0].getName())
        assertEquals("b", list[1].getName())
    }

    private fun nestedListAnnotationData(): IDLAnnotationData {
        val getName: Method = NestedModel::class.java.getMethod("getName")
        val getCount: Method = NestedModel::class.java.getMethod("getCount")
        val nameField = IDLParamField(keyPath = "name", returnType = String::class.java)
        val countField = IDLParamField(keyPath = "count", returnType = Number::class.java)
        val nestedModel =
            IDLAnnotationModel(
                methodModel =
                    hashMapOf(
                        getName to nameField,
                        getCount to countField,
                    ),
                stringModel =
                    hashMapOf(
                        "name" to nameField,
                        "count" to countField,
                    ),
            )
        val outer =
            IDLAnnotationModel(
                stringModel =
                    hashMapOf(
                        "items" to
                            IDLParamField(
                                keyPath = "items",
                                returnType = List::class.java,
                                nestedClassType = NestedModel::class,
                            ),
                    ),
            )
        return IDLAnnotationData(
            paramClass = Any::class.java,
            resultClass = Any::class.java,
            methodParamModel = outer,
            methodResultModel = IDLAnnotationModel(),
            models =
                mapOf(
                    IDLMethodBaseModel.Default::class.java to IDLAnnotationModel(),
                    NestedModel::class.java to nestedModel,
                ),
        )
    }

    @Test
    fun proxyValueReturnsNullWhenNestedAnnotationModelMissing() {
        val nestedField =
            IDLParamField(
                keyPath = "nested",
                returnType = Map::class.java,
                nestedClassType = NestedModel::class,
            )
        val outer = IDLAnnotationModel(stringModel = hashMapOf("nested" to nestedField))
        val data =
            IDLAnnotationData(
                paramClass = Any::class.java,
                resultClass = Any::class.java,
                methodParamModel = outer,
                methodResultModel = IDLAnnotationModel(),
                models = mapOf(IDLMethodBaseModel.Default::class.java to IDLAnnotationModel()),
            )
        val params =
            hashMapOf<String, Any>(
                "nested" to JavaOnlyMap().apply { putString("name", "x") },
            )
        val res = LynxDataProcessorForMap.getJavaOnlyMapParams(params, data)
        assertNull(res?.get("nested"))
    }

    @Test
    fun preCheckSeedsNestedDefaults() {
        val data = nestedAnnotationDataWithDefaults()
        val params = hashMapOf<String, Any>("nested" to JavaOnlyMap())
        val res = LynxDataProcessorForMap.getJavaOnlyMapParams(params, data)
        val proxy = res?.get("nested") as NestedModel
        assertEquals("default-name", proxy.getName())
        assertEquals(99, proxy.getCount())
    }

    private fun nestedAnnotationData(): IDLAnnotationData {
        val getName: Method = NestedModel::class.java.getMethod("getName")
        val getCount: Method = NestedModel::class.java.getMethod("getCount")
        val nameField = IDLParamField(keyPath = "name", returnType = String::class.java)
        val countField = IDLParamField(keyPath = "count", returnType = Number::class.java)
        val nestedModel =
            IDLAnnotationModel(
                methodModel =
                    hashMapOf(
                        getName to nameField,
                        getCount to countField,
                    ),
                stringModel =
                    hashMapOf(
                        "name" to nameField,
                        "count" to countField,
                    ),
            )
        val outer =
            IDLAnnotationModel(
                stringModel =
                    hashMapOf(
                        "nested" to
                            IDLParamField(
                                keyPath = "nested",
                                returnType = Map::class.java,
                                nestedClassType = NestedModel::class,
                            ),
                    ),
            )
        return IDLAnnotationData(
            paramClass = Any::class.java,
            resultClass = Any::class.java,
            methodParamModel = outer,
            methodResultModel = IDLAnnotationModel(),
            models =
                mapOf(
                    IDLMethodBaseModel.Default::class.java to IDLAnnotationModel(),
                    NestedModel::class.java to nestedModel,
                ),
        )
    }

    private fun nestedAnnotationDataWithDefaults(): IDLAnnotationData {
        val getName: Method = NestedModel::class.java.getMethod("getName")
        val getCount: Method = NestedModel::class.java.getMethod("getCount")
        val nameField =
            IDLParamField(
                keyPath = "name",
                returnType = String::class.java,
                defaultValue = IDLDefaultValue(type = DefaultType.STRING, stringValue = "default-name"),
            )
        val countField =
            IDLParamField(
                keyPath = "count",
                returnType = Number::class.java,
                defaultValue = IDLDefaultValue(type = DefaultType.INT, intValue = 99),
            )
        val nestedModel =
            IDLAnnotationModel(
                methodModel =
                    hashMapOf(
                        getName to nameField,
                        getCount to countField,
                    ),
                stringModel =
                    hashMapOf(
                        "name" to nameField,
                        "count" to countField,
                    ),
            )
        val outer =
            IDLAnnotationModel(
                stringModel =
                    hashMapOf(
                        "nested" to
                            IDLParamField(
                                keyPath = "nested",
                                returnType = Map::class.java,
                                nestedClassType = NestedModel::class,
                            ),
                    ),
            )
        return IDLAnnotationData(
            paramClass = Any::class.java,
            resultClass = Any::class.java,
            methodParamModel = outer,
            methodResultModel = IDLAnnotationModel(),
            models =
                mapOf(
                    IDLMethodBaseModel.Default::class.java to IDLAnnotationModel(),
                    NestedModel::class.java to nestedModel,
                ),
        )
    }
}
