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

@RunWith(RobolectricTestRunner::class)
class LynxDataProcessorForMapTest {
    @Test
    fun getJavaOnlyMapParamsAppliesDefaultAndConvertsValues() {
        val data = createAnnotationData()
        val params =
            hashMapOf<String, Any>(
                "name" to "sparkling",
                "mode" to "A",
                "payload" to hashMapOf("k" to 1),
                "items" to listOf(1, 2, 3),
            )

        val result = LynxDataProcessorForMap.getJavaOnlyMapParams(params, data)

        assertEquals("sparkling", result?.get("name"))
        assertEquals(5, (result?.get("count") as Number).toInt())
        assertTrue(result?.get("payload") is Map<*, *>)
        assertTrue(result?.get("items") is List<*>)
    }

    @Test
    fun getJavaOnlyMapParamsThrowsOnInvalidEnum() {
        val data = createAnnotationData()
        val invalid =
            hashMapOf<String, Any>(
                "name" to "sparkling",
                "mode" to "C",
            )

        val throwable =
            runCatching {
                LynxDataProcessorForMap.getJavaOnlyMapParams(invalid, data)
            }.exceptionOrNull()

        assertTrue(throwable is IllegalInputParamException)
    }

    @Test
    fun getJavaOnlyMapParamsThrowsOnRequiredMissing() {
        val data = createAnnotationData()
        val missing = hashMapOf<String, Any>("mode" to "A")

        val throwable =
            runCatching {
                LynxDataProcessorForMap.getJavaOnlyMapParams(missing, data)
            }.exceptionOrNull()

        assertTrue(throwable is IllegalInputParamException)
    }

    private fun createAnnotationData(): IDLAnnotationData {
        val model =
            IDLAnnotationModel(
                stringModel =
                    hashMapOf(
                        "name" to IDLParamField(required = true, keyPath = "name", returnType = String::class.java),
                        "count" to
                            IDLParamField(
                                keyPath = "count",
                                returnType = Number::class.java,
                                defaultValue = IDLDefaultValue(type = DefaultType.INT, intValue = 5),
                            ),
                        "mode" to
                            IDLParamField(
                                keyPath = "mode",
                                isEnum = true,
                                returnType = String::class.java,
                                stringEnum = listOf("A", "B"),
                            ),
                        "payload" to IDLParamField(keyPath = "payload", returnType = Map::class.java),
                        "items" to IDLParamField(keyPath = "items", returnType = List::class.java),
                    ),
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
