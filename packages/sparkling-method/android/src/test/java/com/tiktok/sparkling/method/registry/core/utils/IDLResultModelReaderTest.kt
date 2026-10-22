// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core.utils

import com.tiktok.sparkling.method.registry.core.IDLAnnotationData
import com.tiktok.sparkling.method.registry.core.IDLAnnotationModel
import com.tiktok.sparkling.method.registry.core.IDLParamField
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodIntEnum
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamField
import com.tiktok.sparkling.method.registry.core.exception.IllegalOutputParamException
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import java.lang.reflect.Method
import kotlin.reflect.KClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies that an injected reader controls model binding while existing entry points keep working. */
class IDLResultModelReaderTest {
    @Test
    fun customReaderBindsGettersAndSettersToWireKeys() {
        val model = validHostResult()

        assertEquals("sparkling", model.name)
        assertEquals("ok", model.state)
        assertEquals(
            mapOf(
                "display_name" to "sparkling",
                "state" to "ok",
                "codes" to listOf(1, 2),
                "labels" to mapOf("first" to "ok"),
            ),
            model.convert(),
        )
    }

    @Test
    fun customReaderEnforcesRequiredFieldsWithoutBuiltInAnnotations() {
        val model = IDLMethodResultModelArguments.createModel(HostResult::class.java, null, HostReader())

        val failure = runCatching { model.convert() }.exceptionOrNull()

        assertTrue(failure is IllegalOutputParamException)
        assertTrue(failure?.message.orEmpty().contains("display_name"))
    }

    @Test
    fun customReaderRejectsInvalidEnumsInScalarsListsAndMaps() {
        val mutations =
            listOf<Pair<String, (HostResult) -> Unit>>(
                "state" to { model: HostResult -> model.state = "invalid" },
                "codes" to { model: HostResult -> model.codes = listOf(3) },
                "labels" to { model: HostResult -> model.labels = mapOf("first" to "invalid") },
            )
        mutations.forEach { (key, mutate) ->
            val model = validHostResult()
            mutate(model)

            val failure = runCatching { model.convert() }.exceptionOrNull()

            assertTrue("$key must be checked by the injected enum reader", failure is IllegalOutputParamException)
            assertTrue(failure?.message.orEmpty().contains(key))
        }
    }

    @Test
    fun customReaderRecognizesAndConvertsItsNestedModelType() {
        val reader = HostReader()
        val child = IDLMethodResultModelArguments.createModel(HostChild::class.java, null, reader)
        child.name = "nested"
        val parent = IDLMethodResultModelArguments.createModel(HostParent::class.java, null, reader)
        parent.child = child

        assertEquals(mapOf("child" to mapOf("display_name" to "nested")), parent.convert())
        assertTrue(reader.nestedModelChecks > 0)
    }

    @Test
    fun customReaderCacheHitUsesCachedKeysWithoutReadingFieldAnnotations() {
        val clazz = CachedResult::class.java
        val field = IDLParamField(required = true, keyPath = "cached_name", returnType = String::class.java)
        val resultFields =
            IDLAnnotationModel(
                methodModel =
                    hashMapOf(
                        clazz.getMethod("getName") to field,
                        clazz.getMethod("setName", String::class.java) to field.copy(isGetter = false),
                    ),
                stringModel = hashMapOf("cached_name" to field),
            )
        val cache = IDLAnnotationData(Any::class.java, clazz, IDLAnnotationModel(), resultFields)
        val reader =
            object : IDLResultModelReader by HostReader() {
                override fun findResultAnnotationData(
                    containerID: String?,
                    clazz: Class<*>,
                ): IDLAnnotationData {
                    assertEquals("reader-container", containerID)
                    assertEquals(CachedResult::class.java, clazz)
                    return cache
                }

                override fun readField(method: Method): IDLParamField = error("A cache hit must not read annotations")
            }
        val model = IDLMethodResultModelArguments.createModel(clazz, "reader-container", reader)
        model.name = "cached"

        assertEquals("cached", model.name)
        assertEquals(mapOf("cached_name" to "cached"), model.convert())
    }

    @Suppress("DEPRECATION")
    @Test
    fun existingCreationOverloadsMatchExplicitDefaultReader() {
        val legacy = IDLMethodResultModelArguments.createModel(DefaultResult::class.java)
        val container = IDLMethodResultModelArguments.createModel(DefaultResult::class.java, "default-reader-container")
        val explicit =
            IDLMethodResultModelArguments.createModel(
                DefaultResult::class.java,
                "default-reader-container",
                DefaultIDLResultModelReader,
            )
        val converted =
            listOf(legacy, container, explicit).map { model ->
                model.name = "same"
                model.count = 2
                model.convert()
            }

        val expected = mapOf("name" to "same", "count" to 2)
        converted.forEach { assertEquals(expected, it) }
    }

    private fun validHostResult(): HostResult =
        IDLMethodResultModelArguments.createModel(HostResult::class.java, null, HostReader()).apply {
            name = "sparkling"
            state = "ok"
            codes = listOf(1, 2)
            labels = mapOf("first" to "ok")
        }

    @Target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class HostField(
        val key: String,
        val getter: Boolean = true,
        val required: Boolean = false,
        val strings: Array<String> = [],
        val ints: IntArray = [],
        val primitive: KClass<*> = Any::class,
    )

    interface HostModel : IDLMethodBaseModel

    interface HostResult : HostModel {
        @get:HostField("display_name", required = true)
        @set:HostField("display_name", getter = false)
        var name: String?

        @get:HostField("state", strings = ["ok", "pending"])
        @set:HostField("state", getter = false)
        var state: String?

        @get:HostField("codes", ints = [1, 2], primitive = Number::class)
        @set:HostField("codes", getter = false)
        var codes: List<Number>?

        @get:HostField("labels", strings = ["ok", "pending"], primitive = String::class)
        @set:HostField("labels", getter = false)
        var labels: Map<String, String>?
    }

    interface HostChild : HostModel {
        @get:HostField("display_name", required = true)
        @set:HostField("display_name", getter = false)
        var name: String?
    }

    interface HostParent : HostModel {
        @get:HostField("child", required = true)
        @set:HostField("child", getter = false)
        var child: HostChild?
    }

    interface CachedResult : IDLMethodBaseModel {
        var name: String?
    }

    interface DefaultResult : IDLMethodBaseModel {
        @get:IDLMethodParamField(required = true, keyPath = "name")
        @set:IDLMethodParamField(isGetter = false, keyPath = "name")
        var name: String?

        @get:IDLMethodParamField(keyPath = "count", isEnum = true)
        @get:IDLMethodIntEnum(1, 2)
        @set:IDLMethodParamField(isGetter = false, keyPath = "count")
        var count: Number?
    }

    private class HostReader : IDLResultModelReader {
        var nestedModelChecks = 0

        override fun findResultAnnotationData(
            containerID: String?,
            clazz: Class<*>,
        ): IDLAnnotationData? = null

        override fun readField(method: Method): IDLParamField? {
            val annotation = method.getAnnotation(HostField::class.java) ?: return null
            return IDLParamField(
                keyPath = annotation.key,
                required = annotation.required,
                isGetter = annotation.getter,
                isEnum = annotation.strings.isNotEmpty() || annotation.ints.isNotEmpty(),
                primitiveClassType = annotation.primitive,
                returnType = method.returnType,
            )
        }

        override fun readStringEnumOptions(method: Method): Array<out String>? = method.getAnnotation(HostField::class.java)?.strings?.takeIf { it.isNotEmpty() }

        override fun readIntEnumOptions(method: Method): IntArray? = method.getAnnotation(HostField::class.java)?.ints?.takeIf { it.isNotEmpty() }

        override fun isNestedModel(value: Any): Boolean {
            nestedModelChecks += 1
            return value is HostModel
        }

        override val modelClass: Class<out IDLMethodBaseModel> = HostModel::class.java
    }
}
