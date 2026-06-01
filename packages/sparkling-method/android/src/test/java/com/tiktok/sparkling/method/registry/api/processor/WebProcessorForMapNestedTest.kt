// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.registry.api.processor

import com.tiktok.sparkling.method.registry.core.IDLAnnotationData
import com.tiktok.sparkling.method.registry.core.IDLAnnotationModel
import com.tiktok.sparkling.method.registry.core.IDLDefaultValue
import com.tiktok.sparkling.method.registry.core.IDLParamField
import com.tiktok.sparkling.method.registry.core.annotation.DefaultType
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import org.json.JSONArray
import org.json.JSONObject
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
class WebProcessorForMapNestedTest {
    interface NestedModel : IDLMethodBaseModel {
        fun getName(): String

        fun getCount(): Int
    }

    @Test
    fun proxyForNestedJsonObjectImplementsToJsonAndToString() {
        val data = nestedAnnotationData()
        val nestedJson = JSONObject().put("name", "alice").put("count", 7)
        val params = JSONObject().put("nested", nestedJson)

        val res = WebProcessorForMap.getJavaOnlyMapParams(params, data)
        val proxy = res?.get("nested") as NestedModel

        assertEquals("alice", proxy.getName())
        assertEquals(7, proxy.getCount())

        val toJson = proxy.javaClass.getMethod("toJSON").invoke(proxy)
        // toJSON should return JSONObject equivalent to the nested map
        val asString = (toJson as Any).toString()
        assertNotNull(asString)
    }

    @Test
    fun proxyForNestedJsonArrayMapsEachItemToProxy() {
        val data = nestedListAnnotationData()
        val arr =
            JSONArray()
                .put(JSONObject().put("name", "a").put("count", 1))
                .put(JSONObject().put("name", "b").put("count", 2))
        val params = JSONObject().put("items", arr)

        val res = WebProcessorForMap.getJavaOnlyMapParams(params, data)

        @Suppress("UNCHECKED_CAST")
        val list = res?.get("items") as List<NestedModel>
        assertEquals(2, list.size)
        assertEquals("a", list[0].getName())
        assertEquals("b", list[1].getName())
    }

    @Test
    fun proxyValueReturnsNullWhenNestedModelMissing() {
        // When the nested model has no IDLAnnotationModel mapping, proxyValue returns null.
        val nestedField =
            IDLParamField(
                keyPath = "nested",
                returnType = Map::class.java,
                nestedClassType = NestedModel::class,
            )
        val outerModel = IDLAnnotationModel(stringModel = hashMapOf("nested" to nestedField))
        val data =
            IDLAnnotationData(
                paramClass = Any::class.java,
                resultClass = Any::class.java,
                methodParamModel = outerModel,
                methodResultModel = IDLAnnotationModel(),
                // intentionally do not register NestedModel here
                models = mapOf(IDLMethodBaseModel.Default::class.java to IDLAnnotationModel()),
            )
        val params = JSONObject().put("nested", JSONObject().put("name", "x"))
        val res = WebProcessorForMap.getJavaOnlyMapParams(params, data)
        assertNull(res?.get("nested"))
    }

    @Test
    fun preCheckSeedsNestedDefaultValues() {
        val data = nestedAnnotationDataWithDefaults()
        val params = JSONObject().put("nested", JSONObject())
        val res = WebProcessorForMap.getJavaOnlyMapParams(params, data)
        val proxy = res?.get("nested") as NestedModel

        // Defaults from the nested model should be applied via preCheck.
        assertEquals("default-name", proxy.getName())
        assertEquals(99, proxy.getCount())

        // toJSON should also include defaults seeded by getMapWithDefault.
        val toJson = proxy.javaClass.getMethod("toJSON").invoke(proxy) as JSONObject
        assertEquals("default-name", toJson.optString("name"))
    }

    private fun nestedAnnotationData(): IDLAnnotationData {
        val getName: Method = NestedModel::class.java.getMethod("getName")
        val getCount: Method = NestedModel::class.java.getMethod("getCount")
        val nestedModel =
            IDLAnnotationModel(
                methodModel =
                    hashMapOf(
                        getName to IDLParamField(keyPath = "name", returnType = String::class.java),
                        getCount to IDLParamField(keyPath = "count", returnType = Number::class.java),
                    ),
                stringModel =
                    hashMapOf(
                        "name" to IDLParamField(keyPath = "name", returnType = String::class.java),
                        "count" to IDLParamField(keyPath = "count", returnType = Number::class.java),
                    ),
            )
        val outerModel =
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
            methodParamModel = outerModel,
            methodResultModel = IDLAnnotationModel(),
            models =
                mapOf(
                    IDLMethodBaseModel.Default::class.java to IDLAnnotationModel(),
                    NestedModel::class.java to nestedModel,
                ),
        )
    }

    private fun nestedListAnnotationData(): IDLAnnotationData {
        val getName: Method = NestedModel::class.java.getMethod("getName")
        val getCount: Method = NestedModel::class.java.getMethod("getCount")
        val nestedModel =
            IDLAnnotationModel(
                methodModel =
                    hashMapOf(
                        getName to IDLParamField(keyPath = "name", returnType = String::class.java),
                        getCount to IDLParamField(keyPath = "count", returnType = Number::class.java),
                    ),
                stringModel =
                    hashMapOf(
                        "name" to IDLParamField(keyPath = "name", returnType = String::class.java),
                        "count" to IDLParamField(keyPath = "count", returnType = Number::class.java),
                    ),
            )
        val outerModel =
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
            methodParamModel = outerModel,
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
        val outerModel =
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
            methodParamModel = outerModel,
            methodResultModel = IDLAnnotationModel(),
            models =
                mapOf(
                    IDLMethodBaseModel.Default::class.java to IDLAnnotationModel(),
                    NestedModel::class.java to nestedModel,
                ),
        )
    }
}
