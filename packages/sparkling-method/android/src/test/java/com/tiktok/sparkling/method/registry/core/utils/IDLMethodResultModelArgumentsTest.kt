package com.tiktok.sparkling.method.registry.core.utils

import com.tiktok.sparkling.method.registry.core.annotation.DefaultType
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodIntEnum
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamField
import com.tiktok.sparkling.method.registry.core.annotation.MethodParamDefaultValue
import com.tiktok.sparkling.method.registry.core.exception.IllegalOutputParamException
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IDLMethodResultModelArgumentsTest {
    @Test
    fun createModelSupportsSetterGetterConvertAndAssignDirInNoCachePath() {
        val model = IDLMethodResultModelArguments.createModel(NoCacheResultModel::class.java)
        model.name = "sparkling"
        model.count = 1
        model.dynamic = IAssignDir.Creator.create("wrapped")

        assertEquals("sparkling", model.name)
        assertEquals(1, model.count)

        val converted = model.convert() ?: emptyMap()
        assertEquals("sparkling", converted["name"])
        assertEquals(1, converted["count"])
        assertEquals("wrapped", converted["dynamic"])
        assertTrue(model.toString().contains("name=sparkling"))
    }

    @Test
    fun createModelConvertThrowsWhenRequiredFieldMissing() {
        val model = IDLMethodResultModelArguments.createModel(NoCacheResultModel::class.java)

        val throwable = runCatching { model.convert() }.exceptionOrNull()

        assertTrue(throwable is IllegalOutputParamException)
    }

    @Test
    fun createModelConvertThrowsWhenEnumValueInvalid() {
        val model = IDLMethodResultModelArguments.createModel(NoCacheResultModel::class.java)
        model.name = "sparkling"
        model.count = 3

        val throwable = runCatching { model.convert() }.exceptionOrNull()

        assertTrue(throwable is IllegalOutputParamException)
    }

    private interface NoCacheResultModel : IDLMethodBaseModel {
        @get:IDLMethodParamField(required = true, keyPath = "name")
        @set:IDLMethodParamField(isGetter = false, keyPath = "name")
        var name: String?

        @get:IDLMethodParamField(
            keyPath = "count",
            isEnum = true,
            defaultValue = MethodParamDefaultValue(type = DefaultType.INT, intValue = 1),
        )
        @get:IDLMethodIntEnum(1, 2)
        @set:IDLMethodParamField(isGetter = false, keyPath = "count")
        var count: Number?

        @get:IDLMethodParamField(keyPath = "dynamic")
        @set:IDLMethodParamField(isGetter = false, keyPath = "dynamic")
        var dynamic: Any?
    }
}
