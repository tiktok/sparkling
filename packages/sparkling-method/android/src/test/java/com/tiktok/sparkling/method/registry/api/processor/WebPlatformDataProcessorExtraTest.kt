// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.registry.api.processor

import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IBridgeContext
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.annotation.DefaultType
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodIntEnum
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamField
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodStringEnum
import com.tiktok.sparkling.method.registry.core.annotation.MethodParamDefaultValue
import com.tiktok.sparkling.method.registry.core.exception.IllegalInputParamException
import com.tiktok.sparkling.method.registry.core.model.context.ContextProviderFactory
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseModel
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseParamModel
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.InvocationTargetException

/**
 * Drives the private branches of [WebPlatformDataProcessor.checkValue] and
 * helpers via reflection to bring the class to a healthier coverage level.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class WebPlatformDataProcessorExtraTest {
    private val processor = WebPlatformDataProcessor()

    private fun callPreCheck(
        clazz: Class<*>,
        params: JSONObject,
    ): Any? {
        val m =
            WebPlatformDataProcessor::class.java.getDeclaredMethod(
                "preCheck",
                Class::class.java,
                JSONObject::class.java,
            )
        m.isAccessible = true
        return m.invoke(processor, clazz, params)
    }

    private fun callGetJsonObjectParams(
        clazz: Class<out IDLMethodBaseParamModel>,
        params: JSONObject,
    ): Map<*, *>? {
        val m =
            WebPlatformDataProcessor::class.java.getDeclaredMethod(
                "getJsonObjectParams",
                JSONObject::class.java,
                Class::class.java,
            )
        m.isAccessible = true
        return m.invoke(processor, params, clazz) as Map<*, *>?
    }

    private fun callGetMapWithDefault(
        clazz: Class<*>?,
        json: JSONObject,
    ): JSONObject {
        val m =
            WebPlatformDataProcessor::class.java.getDeclaredMethod(
                "getMapWithDefault",
                Class::class.java,
                JSONObject::class.java,
            )
        m.isAccessible = true
        return m.invoke(processor, clazz, json) as JSONObject
    }

    private fun callGetJsonWithDefault(
        clazz: Class<*>?,
        json: JSONObject,
    ): JSONObject? {
        val m =
            WebPlatformDataProcessor::class.java.getDeclaredMethod(
                "getJsonWithDefault",
                Class::class.java,
                JSONObject::class.java,
            )
        m.isAccessible = true
        return m.invoke(processor, clazz, json) as JSONObject?
    }

    @Test
    fun checkValueRejectsMissingRequiredParam() {
        val ex =
            runCatching {
                callPreCheck(RequiredOnlyModel::class.java, JSONObject())
            }.exceptionOrNull() as InvocationTargetException
        assertTrue(ex.targetException is IllegalInputParamException)
    }

    @Test
    fun checkValueRejectsWrongStringType() {
        val params = JSONObject().put("name", 5)
        val ex =
            runCatching {
                callPreCheck(StringTypedModel::class.java, params)
            }.exceptionOrNull() as InvocationTargetException
        assertTrue(ex.targetException is IllegalInputParamException)
    }

    @Test
    fun checkValueRejectsWrongNumberType() {
        val params = JSONObject().put("count", "not a number")
        val ex =
            runCatching {
                callPreCheck(NumberTypedModel::class.java, params)
            }.exceptionOrNull() as InvocationTargetException
        assertTrue(ex.targetException is IllegalInputParamException)
    }

    @Test
    fun checkValueRejectsWrongBooleanType() {
        val params = JSONObject().put("flag", "nope")
        val ex =
            runCatching {
                callPreCheck(BoolTypedModel::class.java, params)
            }.exceptionOrNull() as InvocationTargetException
        assertTrue(ex.targetException is IllegalInputParamException)
    }

    @Test
    fun checkValueRejectsWrongListType() {
        val params = JSONObject().put("items", "not a list")
        val ex =
            runCatching {
                callPreCheck(ListTypedModel::class.java, params)
            }.exceptionOrNull() as InvocationTargetException
        assertTrue(ex.targetException is IllegalInputParamException)
    }

    @Test
    fun checkValueRejectsWrongMapType() {
        val params = JSONObject().put("config", "not a map")
        val ex =
            runCatching {
                callPreCheck(MapTypedModel::class.java, params)
            }.exceptionOrNull() as InvocationTargetException
        assertTrue(ex.targetException is IllegalInputParamException)
    }

    @Test
    fun checkValueAcceptsNullForOptionalFields() {
        // Should not throw even though required:false and value missing entirely.
        val params = JSONObject()
        val classMap = callPreCheck(OptionalModel::class.java, params)
        assertNotNull(classMap)
    }

    @Test
    fun checkValueAcceptsValidIntEnum() {
        val params = JSONObject().put("level", 2)
        val res = callGetJsonObjectParams(IntEnumModel::class.java, params)
        assertNotNull(res)
        assertEquals(2, (res!!["level"] as Number).toInt())
    }

    @Test
    fun checkValueRejectsInvalidIntEnum() {
        val params = JSONObject().put("level", 99)
        val ex =
            runCatching {
                callGetJsonObjectParams(IntEnumModel::class.java, params)
            }.exceptionOrNull() as InvocationTargetException
        assertTrue(ex.targetException is IllegalInputParamException)
    }

    @Test
    fun checkValueAcceptsValidStringEnumWithMapValues() {
        val params = JSONObject().put("opts", JSONObject().put("k", "A").put("k2", "B"))
        val res = callGetJsonObjectParams(StringEnumMapModel::class.java, params)
        assertNotNull(res)
    }

    @Test
    fun checkValueRejectsInvalidStringEnumWithMapValues() {
        val params = JSONObject().put("opts", JSONObject().put("k", "C"))
        val ex =
            runCatching {
                callGetJsonObjectParams(StringEnumMapModel::class.java, params)
            }.exceptionOrNull() as InvocationTargetException
        assertTrue(ex.targetException is IllegalInputParamException)
    }

    @Test
    fun preCheckSeedsDefaultsForAbsentValues() {
        val params = JSONObject()
        val classMap = callPreCheck(DefaultedModel::class.java, params)
        assertNotNull(classMap)
        // Defaults populated through preCheck.
        assertEquals(7, params.optInt("count"))
        assertEquals("auto", params.optString("name"))
        assertEquals(true, params.optBoolean("flag"))
    }

    @Test
    fun getMapWithDefaultPopulatesNestedDefaultsForGetters() {
        // Use getterModel which only exposes isGetter=true methods.
        val json = JSONObject()
        val out = callGetMapWithDefault(GetterModel::class.java, json)
        assertEquals(7, out.optInt("count"))
        assertEquals("auto", out.optString("name"))
    }

    @Test
    fun getJsonWithDefaultReturnsEmptyWhenNoMethodsMatchAndPopulatesDefaults() {
        val out = callGetJsonWithDefault(GetterModel::class.java, JSONObject())
        assertNotNull(out)
        assertEquals(7, out!!.optInt("count"))
    }

    @Test
    fun transformPlatformDataToMapReturnsNullForUnknownBridge() {
        // No registered annotation pool and no proxy model: should be null.
        class UnknownBridge : IDLBridgeMethod {
            override val name: String = "unknown"

            override fun realHandle(
                params: Map<String, Any?>,
                callback: IDLBridgeMethod.Callback,
                type: BridgePlatformType,
            ) = Unit

            override fun setProviderFactory(contextProviderFactory: ContextProviderFactory?) = Unit

            override fun setBridgeContext(bridgeContext: IBridgeContext) = Unit
        }
        val res = processor.transformPlatformDataToMap(JSONObject(), UnknownBridge::class.java)
        assertNull(res)
    }

    // ----------------------------------------------------------------------
    // Helper model interfaces – they only need annotations to drive branches
    // ----------------------------------------------------------------------

    interface RequiredOnlyModel : IDLMethodBaseParamModel {
        @get:IDLMethodParamField(required = true, keyPath = "name")
        val name: String?

        override fun toJSON(): JSONObject

        override fun convert(): Map<String, Any>?
    }

    interface StringTypedModel : IDLMethodBaseParamModel {
        @get:IDLMethodParamField(keyPath = "name")
        val name: String?

        override fun toJSON(): JSONObject

        override fun convert(): Map<String, Any>?
    }

    interface NumberTypedModel : IDLMethodBaseParamModel {
        @get:IDLMethodParamField(keyPath = "count")
        val count: Number?

        override fun toJSON(): JSONObject

        override fun convert(): Map<String, Any>?
    }

    interface BoolTypedModel : IDLMethodBaseParamModel {
        @get:IDLMethodParamField(keyPath = "flag")
        val flag: Boolean?

        override fun toJSON(): JSONObject

        override fun convert(): Map<String, Any>?
    }

    interface ListTypedModel : IDLMethodBaseParamModel {
        @get:IDLMethodParamField(keyPath = "items")
        val items: List<Any?>?

        override fun toJSON(): JSONObject

        override fun convert(): Map<String, Any>?
    }

    interface MapTypedModel : IDLMethodBaseParamModel {
        @get:IDLMethodParamField(keyPath = "config")
        val config: Map<String, Any?>?

        override fun toJSON(): JSONObject

        override fun convert(): Map<String, Any>?
    }

    interface OptionalModel : IDLMethodBaseParamModel {
        @get:IDLMethodParamField(keyPath = "name")
        val name: String?

        override fun toJSON(): JSONObject

        override fun convert(): Map<String, Any>?
    }

    interface IntEnumModel : IDLMethodBaseParamModel {
        @get:IDLMethodParamField(keyPath = "level", isEnum = true)
        @get:IDLMethodIntEnum(1, 2, 3)
        val level: Number?

        override fun toJSON(): JSONObject

        override fun convert(): Map<String, Any>?
    }

    interface StringEnumMapModel : IDLMethodBaseParamModel {
        @get:IDLMethodParamField(keyPath = "opts", isEnum = true)
        @get:IDLMethodStringEnum("A", "B")
        val opts: Map<String, Any?>?

        override fun toJSON(): JSONObject

        override fun convert(): Map<String, Any>?
    }

    interface DefaultedModel : IDLMethodBaseParamModel {
        @get:IDLMethodParamField(
            keyPath = "count",
            defaultValue = MethodParamDefaultValue(type = DefaultType.INT, intValue = 7),
        )
        val count: Number?

        @get:IDLMethodParamField(
            keyPath = "name",
            defaultValue = MethodParamDefaultValue(type = DefaultType.STRING, stringValue = "auto"),
        )
        val name: String?

        @get:IDLMethodParamField(
            keyPath = "flag",
            defaultValue = MethodParamDefaultValue(type = DefaultType.BOOL, boolValue = true),
        )
        val flag: Boolean?

        override fun toJSON(): JSONObject

        override fun convert(): Map<String, Any>?
    }

    interface GetterModel : IDLMethodBaseModel {
        @get:IDLMethodParamField(
            keyPath = "count",
            isGetter = true,
            defaultValue = MethodParamDefaultValue(type = DefaultType.INT, intValue = 7),
        )
        val count: Number?

        @get:IDLMethodParamField(
            keyPath = "name",
            isGetter = true,
            defaultValue = MethodParamDefaultValue(type = DefaultType.STRING, stringValue = "auto"),
        )
        val name: String?

        override fun toJSON(): JSONObject

        override fun convert(): Map<String, Any>?
    }
}
