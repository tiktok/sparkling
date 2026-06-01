package com.tiktok.sparkling.method.router

import android.content.Context
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IBridgeContext
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.model.context.ContextProviderFactory
import com.tiktok.sparkling.method.registry.core.model.idl.CompletionBlock
import com.tiktok.sparkling.method.router.close.AbsRouterCloseMethodIDL
import com.tiktok.sparkling.method.router.close.RouterCloseMethod
import com.tiktok.sparkling.method.router.open.AbsRouterOpenMethodIDL
import com.tiktok.sparkling.method.router.open.ReplaceType
import com.tiktok.sparkling.method.router.open.RouterOpenMethod
import com.tiktok.sparkling.method.router.utils.AbsRouteOpenHandler
import com.tiktok.sparkling.method.router.utils.IHostRouterDepend
import com.tiktok.sparkling.method.router.utils.RouterProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Extra unit tests for the sparkling-navigation module to cover edge cases not exercised
 * by [RouterMethodUnitTest], including replace-type branches, exception handling,
 * the default [IHostRouterDepend.openScheme] handler chain, and [AbsRouteOpenHandler] wiring.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class RouterCoverageTest {
    private lateinit var bridgeContext: IBridgeContext
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        bridgeContext = mockk(relaxed = true)
        every { bridgeContext.context } returns context
        RouterProvider.hostRouterDepend = null
    }

    @After
    fun tearDown() {
        RouterProvider.hostRouterDepend = null
    }

    // region RouterOpenMethod ------------------------------------------------------------------

    @Test
    fun openMethodFailsWhenSDKContextNull() {
        val method = RouterOpenMethod()
        val params = openParams(scheme = "hybrid://lynx?bundle=a")
        val callback = OpenCallbackRecorder()

        method.handle(params, callback, BridgePlatformType.LYNX)

        assertEquals(IDLBridgeMethod.FAIL, callback.failureCode)
        assertTrue(callback.failureMsg?.contains("Context not provided") == true)
    }

    @Test
    fun openMethodFailsWhenInnerContextNull() {
        val ctxLessBridge = mockk<IBridgeContext>(relaxed = true)
        every { ctxLessBridge.context } returns null

        val method = RouterOpenMethod().apply { setBridgeContext(ctxLessBridge) }
        val params = openParams(scheme = "hybrid://lynx?bundle=a")
        val callback = OpenCallbackRecorder()

        method.handle(params, callback, BridgePlatformType.LYNX)

        assertEquals(IDLBridgeMethod.FAIL, callback.failureCode)
        assertTrue(callback.failureMsg?.contains("Context not provided") == true)
    }

    @Test
    fun openMethodFailsWhenRouterDependNotRegistered() {
        // RouterProvider.hostRouterDepend stays null
        val method = RouterOpenMethod().apply { setBridgeContext(bridgeContext) }
        val params = openParams(scheme = "hybrid://lynx?bundle=a")
        val callback = OpenCallbackRecorder()

        method.handle(params, callback, BridgePlatformType.LYNX)

        assertEquals(IDLBridgeMethod.FAIL, callback.failureCode)
        assertTrue(callback.failureMsg?.contains("Router service not available") == true)
    }

    @Test
    fun openMethodFailsWhenOpenSchemeReturnsFalse() {
        val hostRouter = mockk<IHostRouterDepend>(relaxed = true)
        every { hostRouter.openScheme(any(), any(), any(), any(), any()) } returns false
        RouterProvider.hostRouterDepend = hostRouter

        val method = RouterOpenMethod().apply { setBridgeContext(bridgeContext) }
        val callback = OpenCallbackRecorder()

        method.handle(openParams(scheme = "hybrid://lynx?bundle=a"), callback, BridgePlatformType.LYNX)

        assertEquals(IDLBridgeMethod.FAIL, callback.failureCode)
        assertTrue(callback.failureMsg?.contains("Failed to open scheme") == true)
    }

    @Test
    fun openMethodSwallowsExceptionAndReportsFailure() {
        val hostRouter = mockk<IHostRouterDepend>(relaxed = true)
        every {
            hostRouter.openScheme(any(), any(), any(), any(), any())
        } throws RuntimeException("boom")
        RouterProvider.hostRouterDepend = hostRouter

        val method = RouterOpenMethod().apply { setBridgeContext(bridgeContext) }
        val callback = OpenCallbackRecorder()

        method.handle(openParams(scheme = "hybrid://lynx?bundle=a"), callback, BridgePlatformType.LYNX)

        assertEquals(IDLBridgeMethod.FAIL, callback.failureCode)
    }

    @Test
    fun openMethodReplaceAlwaysCloseBeforeOpenInvokesCloseFirst() {
        val hostRouter = mockk<IHostRouterDepend>(relaxed = true)
        every { hostRouter.closeView(any(), any(), any(), any()) } returns true
        every { hostRouter.openScheme(any(), any(), any(), any(), any()) } returns true
        RouterProvider.hostRouterDepend = hostRouter

        val method = RouterOpenMethod().apply { setBridgeContext(bridgeContext) }
        val callback = OpenCallbackRecorder()

        method.handle(
            openParams(
                scheme = "hybrid://lynx?bundle=a",
                replace = true,
                replaceType = ReplaceType.alwaysCloseBeforeOpen.name,
            ),
            callback,
            BridgePlatformType.LYNX,
        )

        assertNotNull(callback.successResult)
        verifyOrder {
            hostRouter.closeView(bridgeContext, BridgePlatformType.LYNX, null, false)
            hostRouter.openScheme(any(), any(), any(), BridgePlatformType.LYNX, any())
        }
    }

    @Test
    fun openMethodReplaceAlwaysCloseAfterOpenAlwaysClosesEvenWhenOpenFails() {
        val hostRouter = mockk<IHostRouterDepend>(relaxed = true)
        every { hostRouter.openScheme(any(), any(), any(), any(), any()) } returns false
        every { hostRouter.closeView(any(), any(), any(), any()) } returns true
        RouterProvider.hostRouterDepend = hostRouter

        val method = RouterOpenMethod().apply { setBridgeContext(bridgeContext) }
        val callback = OpenCallbackRecorder()

        method.handle(
            openParams(
                scheme = "hybrid://lynx?bundle=a",
                replace = true,
                replaceType = ReplaceType.alwaysCloseAfterOpen.name,
            ),
            callback,
            BridgePlatformType.LYNX,
        )

        // open returned false so overall is failure, but close must still have been invoked
        assertEquals(IDLBridgeMethod.FAIL, callback.failureCode)
        verify(exactly = 1) {
            hostRouter.closeView(bridgeContext, BridgePlatformType.LYNX, null, false)
        }
    }

    @Test
    fun openMethodReplaceOnlyCloseAfterOpenSucceedSkipsCloseOnFailure() {
        val hostRouter = mockk<IHostRouterDepend>(relaxed = true)
        every { hostRouter.openScheme(any(), any(), any(), any(), any()) } returns false
        RouterProvider.hostRouterDepend = hostRouter

        val method = RouterOpenMethod().apply { setBridgeContext(bridgeContext) }
        val callback = OpenCallbackRecorder()

        method.handle(
            openParams(
                scheme = "hybrid://lynx?bundle=a",
                replace = true,
                // default ReplaceType.onlyCloseAfterOpenSucceed when replaceType is null
                replaceType = null,
            ),
            callback,
            BridgePlatformType.LYNX,
        )

        assertEquals(IDLBridgeMethod.FAIL, callback.failureCode)
        verify(exactly = 0) { hostRouter.closeView(any(), any(), any(), any()) }
    }

    @Test
    fun openMethodReplaceOnlyCloseAfterOpenSucceedClosesOnSuccess() {
        val hostRouter = mockk<IHostRouterDepend>(relaxed = true)
        every { hostRouter.openScheme(any(), any(), any(), any(), any()) } returns true
        every { hostRouter.closeView(any(), any(), any(), any()) } returns true
        RouterProvider.hostRouterDepend = hostRouter

        val method = RouterOpenMethod().apply { setBridgeContext(bridgeContext) }
        val callback = OpenCallbackRecorder()

        method.handle(
            openParams(
                scheme = "hybrid://lynx?bundle=a",
                replace = true,
                replaceType = ReplaceType.onlyCloseAfterOpenSucceed.name,
            ),
            callback,
            BridgePlatformType.LYNX,
        )

        assertNotNull(callback.successResult)
        verify(exactly = 1) {
            hostRouter.closeView(bridgeContext, BridgePlatformType.LYNX, null, false)
        }
    }

    @Test
    fun openMethodReplaceSwallowsExceptionAndReportsFailure() {
        val hostRouter = mockk<IHostRouterDepend>(relaxed = true)
        every {
            hostRouter.openScheme(any(), any(), any(), any(), any())
        } throws IllegalStateException("kaboom")
        RouterProvider.hostRouterDepend = hostRouter

        val method = RouterOpenMethod().apply { setBridgeContext(bridgeContext) }
        val callback = OpenCallbackRecorder()

        method.handle(
            openParams(
                scheme = "hybrid://lynx?bundle=a",
                replace = true,
                replaceType = ReplaceType.alwaysCloseBeforeOpen.name,
            ),
            callback,
            BridgePlatformType.LYNX,
        )

        assertEquals(IDLBridgeMethod.FAIL, callback.failureCode)
    }

    // endregion

    // region RouterCloseMethod -----------------------------------------------------------------

    @Test
    fun closeMethodReportsCurrentContainerErrorWhenIdBlank() {
        val hostRouter = mockk<IHostRouterDepend>(relaxed = true)
        every { hostRouter.closeView(any(), any(), any(), any()) } returns false
        RouterProvider.hostRouterDepend = hostRouter

        val method = RouterCloseMethod().apply { setBridgeContext(bridgeContext) }
        val params = mockk<AbsRouterCloseMethodIDL.IDLMethodCloseParamModel>(relaxed = true)
        every { params.containerID } returns "  "
        every { params.animated } returns null

        val callback = CloseCallbackRecorder()
        method.handle(params, callback, BridgePlatformType.LYNX)

        assertEquals(IDLBridgeMethod.FAIL, callback.failureCode)
        assertEquals("Failed to close current container", callback.failureMsg)
        // animated default is true when null
        verify(exactly = 1) {
            hostRouter.closeView(bridgeContext, BridgePlatformType.LYNX, "  ", true)
        }
    }

    @Test
    fun closeMethodReportsContainerSpecificErrorWhenIdProvided() {
        val hostRouter = mockk<IHostRouterDepend>(relaxed = true)
        every { hostRouter.closeView(any(), any(), any(), any()) } returns false
        RouterProvider.hostRouterDepend = hostRouter

        val method = RouterCloseMethod().apply { setBridgeContext(bridgeContext) }
        val params = mockk<AbsRouterCloseMethodIDL.IDLMethodCloseParamModel>(relaxed = true)
        every { params.containerID } returns "container-x"
        every { params.animated } returns true

        val callback = CloseCallbackRecorder()
        method.handle(params, callback, BridgePlatformType.LYNX)

        assertEquals(IDLBridgeMethod.FAIL, callback.failureCode)
        assertEquals("Failed to close container: container-x", callback.failureMsg)
    }

    @Test
    fun closeMethodSwallowsExceptionAndReportsFailure() {
        val hostRouter = mockk<IHostRouterDepend>(relaxed = true)
        every {
            hostRouter.closeView(any(), any(), any(), any())
        } throws RuntimeException("close-boom")
        RouterProvider.hostRouterDepend = hostRouter

        val method = RouterCloseMethod().apply { setBridgeContext(bridgeContext) }
        val params = mockk<AbsRouterCloseMethodIDL.IDLMethodCloseParamModel>(relaxed = true)
        every { params.containerID } returns "container-y"
        every { params.animated } returns true

        val callback = CloseCallbackRecorder()
        method.handle(params, callback, BridgePlatformType.LYNX)

        assertEquals(IDLBridgeMethod.FAIL, callback.failureCode)
    }

    // endregion

    // region AbsRouteOpenHandler ---------------------------------------------------------------

    @Test
    fun absRouteOpenHandlerStoresNextAndExceptionHandlers() {
        val handler = StubRouteOpenHandler()
        val next = StubRouteOpenHandler()
        val errorHandler = StubRouteOpenHandler()

        assertNull(handler.nextHandler)
        assertNull(handler.exceptionHandler)

        handler.setNextHandler(next)
        handler.setExceptionHandler(errorHandler)

        assertSame(next, handler.nextHandler)
        assertSame(errorHandler, handler.exceptionHandler)
    }

    // endregion

    // region IHostRouterDepend default openScheme ----------------------------------------------

    @Test
    fun defaultOpenSchemeReturnsFalseWhenHandlerListEmpty() {
        val depend =
            object : IHostRouterDepend {
                override fun closeView(
                    bridgeContext: IBridgeContext?,
                    type: BridgePlatformType,
                    containerID: String?,
                    animated: Boolean?,
                ) = true
                // empty handler list (default impl)
            }

        val result =
            depend.openScheme(
                bridgeContext,
                "hybrid://lynx?bundle=a",
                emptyMap(),
                BridgePlatformType.LYNX,
                context,
            )
        assertFalse(result)
    }

    @Test
    fun defaultOpenSchemeReturnsTrueWhenFirstHandlerHandles() {
        val handler =
            StubRouteOpenHandler(
                supported = listOf(BridgePlatformType.LYNX),
                opener = { _, _, _ -> true },
            )
        val depend = makeDependWithHandlers(listOf(handler))

        val result =
            depend.openScheme(
                bridgeContext,
                "hybrid://lynx?bundle=a",
                emptyMap(),
                BridgePlatformType.LYNX,
                context,
            )

        assertTrue(result)
        assertEquals(1, handler.callCount)
    }

    @Test
    fun defaultOpenSchemeFallsThroughWhenHandlerReturnsFalse() {
        val first =
            StubRouteOpenHandler(
                supported = listOf(BridgePlatformType.LYNX),
                opener = { _, _, _ -> false },
            )
        val second =
            StubRouteOpenHandler(
                supported = listOf(BridgePlatformType.LYNX),
                opener = { _, _, _ -> true },
            )
        val depend = makeDependWithHandlers(listOf(first, second))

        val result =
            depend.openScheme(
                bridgeContext,
                "hybrid://lynx?bundle=a",
                emptyMap(),
                BridgePlatformType.LYNX,
                context,
            )

        assertTrue(result)
        assertEquals(1, first.callCount)
        assertEquals(1, second.callCount)
    }

    @Test
    fun defaultOpenSchemeSkipsHandlerWithUnsupportedPlatform() {
        val webOnly =
            StubRouteOpenHandler(
                supported = listOf(BridgePlatformType.WEB),
                opener = { _, _, _ -> true },
            )
        val lynxAll =
            StubRouteOpenHandler(
                supported = listOf(BridgePlatformType.ALL),
                opener = { _, _, _ -> true },
            )
        val depend = makeDependWithHandlers(listOf(webOnly, lynxAll))

        val result =
            depend.openScheme(
                bridgeContext,
                "hybrid://lynx?bundle=a",
                emptyMap(),
                BridgePlatformType.LYNX,
                context,
            )

        assertTrue(result)
        assertEquals(0, webOnly.callCount)
        assertEquals(1, lynxAll.callCount)
    }

    @Test
    fun defaultOpenSchemeRoutesToExceptionHandlerWhenHandlerThrows() {
        val throwing =
            StubRouteOpenHandler(
                supported = listOf(BridgePlatformType.LYNX),
                opener = { _, _, _ -> throw RuntimeException("failed") },
            )
        val errorHandler =
            StubRouteOpenHandler(
                supported = listOf(BridgePlatformType.LYNX),
                opener = { _, _, _ -> true },
            )
        val depend =
            object : IHostRouterDepend {
                override fun closeView(
                    bridgeContext: IBridgeContext?,
                    type: BridgePlatformType,
                    containerID: String?,
                    animated: Boolean?,
                ) = true

                override fun provideRouteOpenHandlerList(
                    contextProviderFactory: ContextProviderFactory?,
                ) = listOf<AbsRouteOpenHandler>(throwing)

                override fun provideRouteOpenExceptionHandler(
                    contextProviderFactory: ContextProviderFactory?,
                ): AbsRouteOpenHandler = errorHandler
            }

        val result =
            depend.openScheme(
                bridgeContext,
                "hybrid://lynx?bundle=a",
                emptyMap(),
                BridgePlatformType.LYNX,
                context,
            )

        assertTrue(result)
        assertEquals(1, throwing.callCount)
        assertEquals(1, errorHandler.callCount)
    }

    // endregion

    // region helpers ---------------------------------------------------------------------------

    private fun openParams(
        scheme: String,
        replace: Boolean? = false,
        replaceType: String? = null,
        useSysBrowser: Boolean? = null,
    ): AbsRouterOpenMethodIDL.IDLMethodOpenParamModel {
        val params = mockk<AbsRouterOpenMethodIDL.IDLMethodOpenParamModel>(relaxed = true)
        every { params.scheme } returns scheme
        every { params.replace } returns replace
        every { params.replaceType } returns replaceType
        every { params.useSysBrowser } returns useSysBrowser
        every { params.extra } returns null
        return params
    }

    private fun makeDependWithHandlers(handlers: List<AbsRouteOpenHandler>) =
        object : IHostRouterDepend {
            override fun closeView(
                bridgeContext: IBridgeContext?,
                type: BridgePlatformType,
                containerID: String?,
                animated: Boolean?,
            ) = true

            override fun provideRouteOpenHandlerList(
                contextProviderFactory: ContextProviderFactory?,
            ) = handlers
        }

    private class StubRouteOpenHandler(
        private val supported: List<BridgePlatformType> = listOf(BridgePlatformType.ALL),
        private val opener: (String, Map<String, Any>, Context?) -> Boolean = { _, _, _ -> false },
    ) : AbsRouteOpenHandler() {
        var callCount: Int = 0
            private set

        override fun openScheme(
            scheme: String,
            extraInfo: Map<String, Any>,
            context: Context?,
        ): Boolean {
            callCount++
            return opener(scheme, extraInfo, context)
        }

        override fun getSupportPlatformTypeList(): List<BridgePlatformType> = supported
    }

    private class OpenCallbackRecorder : CompletionBlock<AbsRouterOpenMethodIDL.IDLMethodOpenResultModel> {
        var successResult: AbsRouterOpenMethodIDL.IDLMethodOpenResultModel? = null
        var failureCode: Int? = null
        var failureMsg: String? = null

        override fun onSuccess(
            result: AbsRouterOpenMethodIDL.IDLMethodOpenResultModel,
            msg: String,
        ) {
            successResult = result
        }

        override fun onFailure(
            code: Int,
            msg: String,
            data: AbsRouterOpenMethodIDL.IDLMethodOpenResultModel?,
        ) {
            failureCode = code
            failureMsg = msg
        }

        override fun onRawSuccess(data: AbsRouterOpenMethodIDL.IDLMethodOpenResultModel?) = Unit
    }

    private class CloseCallbackRecorder : CompletionBlock<AbsRouterCloseMethodIDL.IDLMethodCloseResultModel> {
        var successResult: AbsRouterCloseMethodIDL.IDLMethodCloseResultModel? = null
        var failureCode: Int? = null
        var failureMsg: String? = null

        override fun onSuccess(
            result: AbsRouterCloseMethodIDL.IDLMethodCloseResultModel,
            msg: String,
        ) {
            successResult = result
        }

        override fun onFailure(
            code: Int,
            msg: String,
            data: AbsRouterCloseMethodIDL.IDLMethodCloseResultModel?,
        ) {
            failureCode = code
            failureMsg = msg
        }

        override fun onRawSuccess(data: AbsRouterCloseMethodIDL.IDLMethodCloseResultModel?) = Unit
    }

    // endregion
}
