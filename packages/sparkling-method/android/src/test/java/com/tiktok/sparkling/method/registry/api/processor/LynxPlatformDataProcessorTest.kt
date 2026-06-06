package com.tiktok.sparkling.method.registry.api.processor

import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IBridgeContext
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.annotation.DefaultType
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodParamField
import com.tiktok.sparkling.method.registry.core.annotation.IDLMethodStringEnum
import com.tiktok.sparkling.method.registry.core.annotation.MethodParamDefaultValue
import com.tiktok.sparkling.method.registry.core.exception.IllegalInputParamException
import com.tiktok.sparkling.method.registry.core.model.context.ContextProviderFactory
import com.tiktok.sparkling.method.registry.core.model.idl.IDLMethodBaseParamModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.InvocationTargetException

@RunWith(RobolectricTestRunner::class)
class LynxPlatformDataProcessorTest {
    @Test
    fun matchPlatformTypeAndTransformMapToPlatformDataWork() {
        val processor = LynxPlatformDataProcessor()
        assertTrue(processor.matchPlatformType(BridgePlatformType.LYNX))
        assertTrue(!processor.matchPlatformType(BridgePlatformType.WEB))

        val platformData =
            processor.transformMapToPlatformData(
                mapOf("name" to "sparkling", "count" to 3),
                FakeBridgeMethod::class.java,
            )
        assertEquals("sparkling", platformData.getString("name"))
        assertEquals(3, platformData.getInt("count"))
    }

    @Test
    fun getJavaOnlyMapParamsAppliesDefaultAndChecksEnum() {
        val processor = LynxPlatformDataProcessor()
        val method =
            LynxPlatformDataProcessor::class.java.getDeclaredMethod(
                "getJavaOnlyMapParams",
                HashMap::class.java,
                Class::class.java,
            )
        method.isAccessible = true

        val ok =
            hashMapOf<String, Any>(
                "name" to "sparkling",
                "mode" to "A",
            )
        val result = method.invoke(processor, ok, DemoParamModel::class.java) as Map<*, *>
        assertEquals("sparkling", result["name"])
        assertEquals(2, (result["count"] as Number).toInt())

        val invalid =
            hashMapOf<String, Any>(
                "name" to "sparkling",
                "mode" to "C",
            )
        val ex =
            runCatching { method.invoke(processor, invalid, DemoParamModel::class.java) }
                .exceptionOrNull() as InvocationTargetException
        assertTrue(ex.targetException is IllegalInputParamException)
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
