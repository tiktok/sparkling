// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.registry.api.processor

import com.tiktok.sparkling.method.registry.core.IDLAnnotationData
import com.tiktok.sparkling.method.registry.core.IDLAnnotationModel
import com.tiktok.sparkling.method.registry.core.IDLDefaultValue
import com.tiktok.sparkling.method.registry.core.IDLParamField
import com.tiktok.sparkling.method.registry.core.annotation.DefaultType
import com.tiktok.sparkling.method.registry.core.exception.IllegalInputParamException
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LynxDataProcessorForMapExtraTest {
    @Test
    fun checkValueRejectsWrongStringType() {
        val data =
            annotationData(
                "v" to IDLParamField(keyPath = "v", returnType = String::class.java),
            )
        val ex =
            runCatching {
                LynxDataProcessorForMap.getJavaOnlyMapParams(hashMapOf("v" to 123), data)
            }.exceptionOrNull()
        assertTrue(ex is IllegalInputParamException)
    }

    @Test
    fun checkValueRejectsWrongNumberType() {
        val data =
            annotationData(
                "n" to IDLParamField(keyPath = "n", returnType = Number::class.java),
            )
        val ex =
            runCatching {
                LynxDataProcessorForMap.getJavaOnlyMapParams(hashMapOf("n" to "not"), data)
            }.exceptionOrNull()
        assertTrue(ex is IllegalInputParamException)
    }

    @Test
    fun checkValueRejectsWrongBooleanType() {
        val data =
            annotationData(
                "b" to IDLParamField(keyPath = "b", returnType = Boolean::class.java),
            )
        val ex =
            runCatching {
                LynxDataProcessorForMap.getJavaOnlyMapParams(hashMapOf("b" to "true"), data)
            }.exceptionOrNull()
        assertTrue(ex is IllegalInputParamException)
    }

    @Test
    fun checkValueRejectsWrongMapType() {
        val data =
            annotationData(
                "m" to IDLParamField(keyPath = "m", returnType = Map::class.java),
            )
        val ex =
            runCatching {
                LynxDataProcessorForMap.getJavaOnlyMapParams(hashMapOf("m" to "not"), data)
            }.exceptionOrNull()
        assertTrue(ex is IllegalInputParamException)
    }

    @Test
    fun checkValueRejectsWrongListType() {
        val data =
            annotationData(
                "l" to IDLParamField(keyPath = "l", returnType = List::class.java),
            )
        val ex =
            runCatching {
                LynxDataProcessorForMap.getJavaOnlyMapParams(hashMapOf("l" to "not"), data)
            }.exceptionOrNull()
        assertTrue(ex is IllegalInputParamException)
    }

    @Test
    fun preCheckSeedsBooleanAndStringDefaults() {
        val data =
            annotationData(
                "b" to
                    IDLParamField(
                        keyPath = "b",
                        returnType = Boolean::class.java,
                        defaultValue = IDLDefaultValue(type = DefaultType.BOOL, boolValue = true),
                    ),
                "s" to
                    IDLParamField(
                        keyPath = "s",
                        returnType = String::class.java,
                        defaultValue = IDLDefaultValue(type = DefaultType.STRING, stringValue = "x"),
                    ),
            )
        val res = LynxDataProcessorForMap.getJavaOnlyMapParams(hashMapOf(), data)
        assertEquals(true, res?.get("b"))
        assertEquals("x", res?.get("s"))
    }

    @Test
    fun preCheckSeedsAllNumberDefaultTypes() {
        for (type in listOf(DefaultType.INT, DefaultType.LONG, DefaultType.DOUBLE)) {
            val intValue = if (type == DefaultType.INT) 7 else 0
            val longValue = if (type == DefaultType.LONG) 9L else 0L
            val doubleValue = if (type == DefaultType.DOUBLE) 1.5 else 0.0
            val data =
                annotationData(
                    "v" to
                        IDLParamField(
                            keyPath = "v",
                            returnType = Number::class.java,
                            defaultValue =
                                IDLDefaultValue(
                                    type = type,
                                    intValue = intValue,
                                    longValue = longValue,
                                    doubleValue = doubleValue,
                                ),
                        ),
                )
            val res = LynxDataProcessorForMap.getJavaOnlyMapParams(hashMapOf(), data)
            val seed = res?.get("v")
            when (type) {
                DefaultType.INT -> assertEquals(7, (seed as Number).toInt())
                DefaultType.LONG -> assertEquals(9L, (seed as Number).toLong())
                DefaultType.DOUBLE -> assertEquals(1.5, (seed as Number).toDouble(), 0.0001)
                else -> Unit
            }
        }
    }

    @Test
    fun checkValueRejectsInvalidIntEnum() {
        val data =
            annotationData(
                "n" to
                    IDLParamField(
                        keyPath = "n",
                        returnType = Number::class.java,
                        isEnum = true,
                        intEnum = listOf(1, 2),
                    ),
            )
        val ex =
            runCatching {
                LynxDataProcessorForMap.getJavaOnlyMapParams(hashMapOf("n" to 5), data)
            }.exceptionOrNull()
        assertTrue(ex is IllegalInputParamException)
    }

    @Test
    fun checkValueAcceptsValidStringEnum() {
        val data =
            annotationData(
                "mode" to
                    IDLParamField(
                        keyPath = "mode",
                        returnType = String::class.java,
                        isEnum = true,
                        stringEnum = listOf("a", "b"),
                    ),
            )
        val res = LynxDataProcessorForMap.getJavaOnlyMapParams(hashMapOf("mode" to "a"), data)
        assertEquals("a", res?.get("mode"))
    }

    @Test
    fun checkValueRejectsMapEnumWithBadString() {
        val data =
            annotationData(
                "m" to
                    IDLParamField(
                        keyPath = "m",
                        returnType = Map::class.java,
                        isEnum = true,
                        stringEnum = listOf("on"),
                    ),
            )
        val ex =
            runCatching {
                LynxDataProcessorForMap.getJavaOnlyMapParams(
                    hashMapOf("m" to hashMapOf("k" to "off")),
                    data,
                )
            }.exceptionOrNull()
        assertTrue(ex is IllegalInputParamException)
    }

    @Test
    fun checkValueRejectsMapEnumWithBadInt() {
        val data =
            annotationData(
                "m" to
                    IDLParamField(
                        keyPath = "m",
                        returnType = Map::class.java,
                        isEnum = true,
                        intEnum = listOf(1),
                    ),
            )
        val ex =
            runCatching {
                LynxDataProcessorForMap.getJavaOnlyMapParams(
                    hashMapOf("m" to hashMapOf("k" to 9)),
                    data,
                )
            }.exceptionOrNull()
        assertTrue(ex is IllegalInputParamException)
    }

    @Test
    fun checkValueAcceptsMapEnumWithGoodValues() {
        val data =
            annotationData(
                "m" to
                    IDLParamField(
                        keyPath = "m",
                        returnType = Map::class.java,
                        isEnum = true,
                        stringEnum = listOf("on"),
                    ),
            )
        val res =
            LynxDataProcessorForMap.getJavaOnlyMapParams(
                hashMapOf("m" to hashMapOf("k" to "on")),
                data,
            )
        assertTrue(res?.get("m") is Map<*, *>)
    }

    private fun annotationData(vararg fields: Pair<String, IDLParamField>): IDLAnnotationData {
        val model =
            IDLAnnotationModel(
                stringModel = HashMap(fields.toMap()),
            )
        return IDLAnnotationData(
            paramClass = Any::class.java,
            resultClass = Any::class.java,
            methodParamModel = model,
            methodResultModel = IDLAnnotationModel(),
            models = mapOf(IDLMethodBaseModel.Default::class.java to IDLAnnotationModel()),
        )
    }
}
