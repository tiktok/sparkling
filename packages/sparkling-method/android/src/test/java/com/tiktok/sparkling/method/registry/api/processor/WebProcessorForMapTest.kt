package com.tiktok.sparkling.method.registry.api.processor

import com.tiktok.sparkling.method.registry.core.IDLAnnotationData
import com.tiktok.sparkling.method.registry.core.IDLAnnotationModel
import com.tiktok.sparkling.method.registry.core.IDLDefaultValue
import com.tiktok.sparkling.method.registry.core.IDLParamField
import com.tiktok.sparkling.method.registry.core.annotation.DefaultType
import com.tiktok.sparkling.method.registry.core.exception.IllegalInputParamException
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WebProcessorForMapTest {
    @Test
    fun getJavaOnlyMapParamsAppliesDefaultAndParsesJsonTypes() {
        val data = createAnnotationData()
        val params =
            JSONObject(
                """
                {
                  "name": "sparkling",
                  "mode": "A",
                  "payload": {"k": 1},
                  "items": [1,2,3],
                  "nullable": null
                }
                """.trimIndent(),
            )

        val result = WebProcessorForMap.getJavaOnlyMapParams(params, data)

        assertEquals("sparkling", result?.get("name"))
        assertEquals(5, (result?.get("count") as Number).toInt())
        assertTrue(result?.get("payload") is Map<*, *>)
        assertTrue(result?.get("items") is List<*>)
        assertNull(result?.get("nullable"))
    }

    @Test
    fun getJavaOnlyMapParamsThrowsOnInvalidEnum() {
        val data = createAnnotationData()
        val invalid = JSONObject("{\"name\":\"sparkling\",\"mode\":\"C\"}")

        val throwable =
            runCatching {
                WebProcessorForMap.getJavaOnlyMapParams(invalid, data)
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
                        "nullable" to IDLParamField(keyPath = "nullable", returnType = String::class.java),
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
