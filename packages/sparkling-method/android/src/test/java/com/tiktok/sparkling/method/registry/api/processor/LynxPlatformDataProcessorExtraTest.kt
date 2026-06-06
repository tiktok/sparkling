// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.registry.api.processor

import com.lynx.react.bridge.JavaOnlyArray
import com.lynx.react.bridge.JavaOnlyMap
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
 * Drives the private branches of [LynxPlatformDataProcessor.checkValue] and
 * helpers via reflection so coverage exercises the type-validation paths.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LynxPlatformDataProcessorExtraTest {
    private val processor = LynxPlatformDataProcessor()

    private fun callPreCheck(
        clazz: Class<*>,
        params: HashMap<String, Any>,
    ): Any? {
        val m =
            LynxPlatformDataProcessor::class.java.getDeclaredMethod(
                "preCheck",
                Class::class.java,
                HashMap::class.java,
            )
        m.isAccessible = true
        return m.invoke(processor, clazz, params)
    }

    private fun callGetMapWithDefault(
        clazz: Class<*>?,
        map: HashMap<String, Any>,
    ): Any? {
        val m =
            LynxPlatformDataProcessor::class.java.getDeclaredMethod(
                "getMapWithDefault",
                Class::class.java,
                HashMap::class.java,
            )
        m.isAccessible = true
        return m.invoke(processor, clazz, map)
    }

    private fun callGetJavaOnlyMapParams(
        clazz: Class<out IDLMethodBaseParamModel>,
        params: HashMap<String, Any>,
    ): Map<*, *>? {
        val m =
            LynxPlatformDataProcessor::class.java.getDeclaredMethod(
                "getJavaOnlyMapParams",
                HashMap::class.java,
                Class::class.java,
            )
        m.isAccessible = true
        return m.invoke(processor, params, clazz) as Map<*, *>?
    }

    @Test
    fun checkValueRejectsMissingRequiredParam() {
        val ex =
            runCatching {
                callPreCheck(RequiredOnlyModel::class.java, hashMapOf())
            }.exceptionOrNull() as InvocationTargetException
        assertTrue(ex.targetException is IllegalInputParamException)
    }

    @Test
    fun checkValueRejectsWrongStringType() {
        val ex =
            runCatching {
                callPreCheck(StringTypedModel::class.java, hashMapOf<String, Any>("name" to 5))
            }.exceptionOrNull() as InvocationTargetException
        assertTrue(ex.targetException is IllegalInputParamException)
    }

    @Test
    fun checkValueRejectsWrongNumberType() {
        val ex =
            runCatching {
                callPreCheck(NumberTypedModel::class.java, hashMapOf<String, Any>("count" to "x"))
            }.exceptionOrNull() as InvocationTargetException
        assertTrue(ex.targetException is IllegalInputParamException)
    }

    @Test
    fun checkValueRejectsWrongBooleanType() {
        val ex =
            runCatching {
                callPreCheck(BoolTypedModel::class.java, hashMapOf<String, Any>("flag" to "no"))
            }.exceptionOrNull() as InvocationTargetException
        assertTrue(ex.targetException is IllegalInputParamException)
    }

    @Test
    fun checkValueRejectsWrongListType() {
        val ex =
            runCatching {
                callPreCheck(ListTypedModel::class.java, hashMapOf<String, Any>("items" to "no"))
            }.exceptionOrNull() as InvocationTargetException
        assertTrue(ex.targetException is IllegalInputParamException)
    }

    @Test
    fun checkValueRejectsWrongMapType() {
        val ex =
            runCatching {
                callPreCheck(MapTypedModel::class.java, hashMapOf<String, Any>("config" to "no"))
            }.exceptionOrNull() as InvocationTargetException
        assertTrue(ex.targetException is IllegalInputParamException)
    }

    @Test
    fun checkValueAcceptsValidIntEnum() {
        val params = hashMapOf<String, Any>("level" to 2)
        val res = callGetJavaOnlyMapParams(IntEnumModel::class.java, params)
        assertNotNull(res)
        assertEquals(2, (res!!["level"] as Number).toInt())
    }

    @Test
    fun checkValueRejectsInvalidIntEnum() {
        val params = hashMapOf<String, Any>("level" to 99)
        val ex =
            runCatching {
                callGetJavaOnlyMapParams(IntEnumModel::class.java, params)
            }.exceptionOrNull() as InvocationTargetException
        assertTrue(ex.targetException is IllegalInputParamException)
    }

    @Test
    fun preCheckSeedsDefaultsForAbsentValues() {
        val params = hashMapOf<String, Any>()
        callPreCheck(DefaultedModel::class.java, params)
        assertEquals(7, (params["count"] as Number).toInt())
        assertEquals("auto", params["name"])
        assertEquals(true, params["flag"])
    }

    @Test
    fun getMapWithDefaultPopulatesNestedDefaultsForGetters() {
        val map = hashMapOf<String, Any>()

        @Suppress("UNCHECKED_CAST")
        val out = callGetMapWithDefault(GetterModel::class.java, map) as Map<String, Any?>
        assertEquals(7, (out["count"] as Number).toInt())
        assertEquals("auto", out["name"])
    }

    @Test
    fun transformPlatformDataToMapReturnsNullForUnknownBridge() {
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
        val res = processor.transformPlatformDataToMap(JavaOnlyMap(), UnknownBridge::class.java)
        assertNull(res)
    }

    @Test
    fun transformMapToPlatformDataConvertsNestedShapes() {
        val res =
            processor.transformMapToPlatformData(
                mapOf(
                    "name" to "sparkling",
                    "list" to listOf(1, "two", true),
                    "nested" to mapOf("a" to 1),
                ),
                object : IDLBridgeMethod {
                    override val name: String = "x"

                    override fun realHandle(
                        params: Map<String, Any?>,
                        callback: IDLBridgeMethod.Callback,
                        type: BridgePlatformType,
                    ) = Unit

                    override fun setProviderFactory(contextProviderFactory: ContextProviderFactory?) = Unit

                    override fun setBridgeContext(bridgeContext: IBridgeContext) = Unit
                }.javaClass,
            )
        assertEquals("sparkling", res.getString("name"))
        val arr = res.getArray("list") as JavaOnlyArray
        assertEquals(3, arr.size)
    }

    // ----------------------------------------------------------------------
    // Helper interfaces
    // ----------------------------------------------------------------------

    interface RequiredOnlyModel : IDLMethodBaseParamModel {
        @get:IDLMethodParamField(required = true, keyPath = "name")
        val name: String?
    }

    interface StringTypedModel : IDLMethodBaseParamModel {
        @get:IDLMethodParamField(keyPath = "name")
        val name: String?
    }

    interface NumberTypedModel : IDLMethodBaseParamModel {
        @get:IDLMethodParamField(keyPath = "count")
        val count: Number?
    }

    interface BoolTypedModel : IDLMethodBaseParamModel {
        @get:IDLMethodParamField(keyPath = "flag")
        val flag: Boolean?
    }

    interface ListTypedModel : IDLMethodBaseParamModel {
        @get:IDLMethodParamField(keyPath = "items")
        val items: List<Any?>?
    }

    interface MapTypedModel : IDLMethodBaseParamModel {
        @get:IDLMethodParamField(keyPath = "config")
        val config: Map<String, Any?>?
    }

    interface IntEnumModel : IDLMethodBaseParamModel {
        @get:IDLMethodParamField(keyPath = "level", isEnum = true)
        @get:IDLMethodIntEnum(1, 2, 3)
        val level: Number?
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
    }
}
