package com.tiktok.sparkling.method.registry.core.utils

import com.tiktok.sparkling.method.registry.core.IDLAnnotationData
import com.tiktok.sparkling.method.registry.core.IDLAnnotationModel
import com.tiktok.sparkling.method.registry.core.IDLDefaultValue
import com.tiktok.sparkling.method.registry.core.IDLParamField
import com.tiktok.sparkling.method.registry.core.annotation.DefaultType
import com.tiktok.sparkling.method.registry.core.exception.IllegalOutputParamException
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IDLMethodResultModelHelperTest {
    @Test
    fun convertToMapByCacheValidatesAndTransformsValues() {
        val data = createAnnotationData()
        val content =
            mutableMapOf<String, Any?>(
                "name" to "sparkling",
                "count" to 2,
                "dynamic" to IAssignDir.Creator.create("wrapped"),
            )

        IDLMethodResultModelHelper.convertToMapByCache(data, content)

        assertEquals("sparkling", content["name"])
        assertEquals(2, content["count"])
        assertEquals("wrapped", content["dynamic"])
    }

    @Test
    fun convertToMapByCacheThrowsWhenEnumInvalidOrRequiredMissing() {
        val data = createAnnotationData()
        val missingRequired = mutableMapOf<String, Any?>("count" to 1)
        val invalidEnum = mutableMapOf<String, Any?>("name" to "sparkling", "count" to 9)

        val missErr = runCatching { IDLMethodResultModelHelper.convertToMapByCache(data, missingRequired) }.exceptionOrNull()
        val enumErr = runCatching { IDLMethodResultModelHelper.convertToMapByCache(data, invalidEnum) }.exceptionOrNull()

        assertTrue(missErr is IllegalOutputParamException)
        assertTrue(enumErr is IllegalOutputParamException)
    }

    @Test
    fun getterAndSetterOperateViaCacheMethodModel() {
        val data = createAnnotationData()
        val content = mutableMapOf<String, Any?>()

        val setNameMethod = CacheResultModel::class.java.getMethod("setName", String::class.java)
        val getNameMethod = CacheResultModel::class.java.getMethod("getName")

        IDLMethodResultModelHelper.getterAndSetter(data, content, setNameMethod, arrayOf("sparkling"))
        val got = IDLMethodResultModelHelper.getterAndSetter(data, content, getNameMethod, null)

        assertEquals("sparkling", got)
    }

    @Test
    fun getterAndSetterSetterWithNoArgsWritesNull() {
        val data = createAnnotationData()
        val content = mutableMapOf<String, Any?>("name" to "x")
        val setNameMethod = CacheResultModel::class.java.getMethod("setName", String::class.java)

        IDLMethodResultModelHelper.getterAndSetter(data, content, setNameMethod, null)

        assertNull(content["name"])
    }

    private fun createAnnotationData(): IDLAnnotationData {
        val getName = CacheResultModel::class.java.getMethod("getName")
        val setName = CacheResultModel::class.java.getMethod("setName", String::class.java)
        val getCount = CacheResultModel::class.java.getMethod("getCount")
        val setCount = CacheResultModel::class.java.getMethod("setCount", Number::class.java)
        val getDynamic = CacheResultModel::class.java.getMethod("getDynamic")
        val setDynamic = CacheResultModel::class.java.getMethod("setDynamic", Any::class.java)

        val nameField = IDLParamField(required = true, keyPath = "name", returnType = String::class.java)
        val countField =
            IDLParamField(
                keyPath = "count",
                isEnum = true,
                returnType = Number::class.java,
                intEnum = listOf(1, 2),
                defaultValue = IDLDefaultValue(type = DefaultType.INT, intValue = 1),
            )
        val dynamicField = IDLParamField(keyPath = "dynamic", returnType = Any::class.java)

        val model =
            IDLAnnotationModel(
                methodModel =
                    hashMapOf(
                        getName to nameField,
                        setName to nameField.copy(isGetter = false),
                        getCount to countField,
                        setCount to countField.copy(isGetter = false),
                        getDynamic to dynamicField,
                        setDynamic to dynamicField.copy(isGetter = false),
                    ),
                stringModel =
                    hashMapOf(
                        "name" to nameField,
                        "count" to countField,
                        "dynamic" to dynamicField,
                    ),
            )

        return IDLAnnotationData(
            paramClass = Any::class.java,
            resultClass = CacheResultModel::class.java,
            methodParamModel = IDLAnnotationModel(),
            methodResultModel = model,
            models = mapOf(IDLMethodBaseModel.Default::class.java to IDLAnnotationModel()),
        )
    }

    private interface CacheResultModel : IDLMethodBaseModel {
        var name: String?
        var count: Number?
        var dynamic: Any?
    }
}
