package com.tiktok.sparkling.method.registry.api.processor

import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IBridgeContext
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.model.context.ContextProviderFactory
import com.tiktok.sparkling.method.registry.core.annotation.DefaultType
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamField
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodStringEnum
import com.tiktok.sparkling.method.registry.core.annotation.MethodParamDefaultValue
import com.tiktok.sparkling.method.registry.core.exception.IllegalInputParamException
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
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy

@RunWith(RobolectricTestRunner::class)
class WebPlatformDataProcessorTest {
    @Test
    fun matchPlatformTypeAndTransformMapToPlatformDataWork() {
        val processor = WebPlatformDataProcessor()

        assertTrue(processor.matchPlatformType(BridgePlatformType.WEB))
        assertTrue(!processor.matchPlatformType(BridgePlatformType.LYNX))

        val result = processor.transformMapToPlatformData(mapOf("a" to 1, "b" to "x"), FakeBridgeMethod::class.java)
        assertEquals(1, result.getInt("a"))
        assertEquals("x", result.getString("b"))
    }

    @Test
    fun getJsonObjectParamsAppliesDefaultAndChecksEnum() {
        val processor = WebPlatformDataProcessor()
        val method =
            WebPlatformDataProcessor::class.java.getDeclaredMethod(
                "getJsonObjectParams",
                JSONObject::class.java,
                Class::class.java,
            )
        method.isAccessible = true

        val ok = JSONObject("{\"name\":\"sparkling\",\"mode\":\"A\"}")
        val map = method.invoke(processor, ok, DemoParamModel::class.java) as Map<*, *>
        assertEquals("sparkling", map["name"])
        assertEquals(2, (map["count"] as Number).toInt())

        val invalidEnum = JSONObject("{\"name\":\"sparkling\",\"mode\":\"C\"}")
        val ex =
            runCatching { method.invoke(processor, invalidEnum, DemoParamModel::class.java) }
                .exceptionOrNull() as InvocationTargetException
        assertTrue(ex.targetException is IllegalInputParamException)
    }

    @Test
    fun convertValueWithAnnotationBuildsNestedProxy() {
        val processor = WebPlatformDataProcessor()
        val convertMethod =
            WebPlatformDataProcessor::class.java.getDeclaredMethod(
                "convertValueWithAnnotation",
                Any::class.java,
                IDLMethodParamField::class.java,
            )
        convertMethod.isAccessible = true

        val annotation = ParentParamModel::class.java.getMethod("getNested").getAnnotation(IDLMethodParamField::class.java)
        val nestedJson = JSONObject("{\"id\":\"n1\"}")
        val proxyObj = convertMethod.invoke(processor, nestedJson, annotation)
        assertNotNull(proxyObj)
        assertTrue(Proxy.isProxyClass(proxyObj.javaClass))

        val nestedValue = (proxyObj as NestedParamModel).id
        assertEquals("n1", nestedValue)
    }

    @Test
    fun convertValueWithAnnotationHandlesJsonArrayAndNull() {
        val processor = WebPlatformDataProcessor()
        val convertMethod =
            WebPlatformDataProcessor::class.java.getDeclaredMethod(
                "convertValueWithAnnotation",
                Any::class.java,
                IDLMethodParamField::class.java,
            )
        convertMethod.isAccessible = true

        val safeAnnotation =
            DemoParamModel::class.java
                .getMethod("getCount")
                .getAnnotation(IDLMethodParamField::class.java)

        val convertedArray = convertMethod.invoke(processor, JSONArray("[1,{\"k\":\"v\"}]"), safeAnnotation)
        assertTrue(convertedArray is List<*>)

        val convertedNull = convertMethod.invoke(processor, JSONObject.NULL, null)
        assertNull(convertedNull)
    }

    private interface DemoParamModel : IDLMethodBaseParamModel {
        @get:IDLMethodParamField(required = true, keyPath = "name")
        val name: String?

        @get:IDLMethodParamField(
            keyPath = "count",
            defaultValue = MethodParamDefaultValue(type = DefaultType.INT, intValue = 2),
        )
        val count: Number?

        @get:IDLMethodParamField(keyPath = "mode", isEnum = true)
        @get:IDLMethodStringEnum("A", "B")
        val mode: String?

        override fun toJSON(): JSONObject

        override fun convert(): Map<String, Any>?
    }

    private interface NestedParamModel : IDLMethodBaseModel {
        @get:IDLMethodParamField(required = true, keyPath = "id")
        val id: String?

        override fun toJSON(): JSONObject

        override fun convert(): Map<String, Any>?
    }

    private interface ParentParamModel : IDLMethodBaseModel {
        @get:IDLMethodParamField(keyPath = "nested", nestedClassType = NestedParamModel::class)
        val nested: Map<String, Any>?

        override fun toJSON(): JSONObject

        override fun convert(): Map<String, Any>?
    }

    private class FakeBridgeMethod : IDLBridgeMethod {
        override val name: String = "fake"

        override fun realHandle(
            params: Map<String, Any?>,
            callback: IDLBridgeMethod.Callback,
            type: BridgePlatformType,
        ) = Unit

        override fun setProviderFactory(contextProviderFactory: ContextProviderFactory?) = Unit

        override fun setBridgeContext(bridgeContext: IBridgeContext) = Unit
    }
}
