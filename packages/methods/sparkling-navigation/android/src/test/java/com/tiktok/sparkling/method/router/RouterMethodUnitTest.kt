package com.tiktok.sparkling.method.router

import android.content.Context
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IBridgeContext
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.model.idl.CompletionBlock
import com.tiktok.sparkling.method.router.close.AbsRouterCloseMethodIDL
import com.tiktok.sparkling.method.router.close.RouterCloseMethod
import com.tiktok.sparkling.method.router.open.AbsRouterOpenMethodIDL
import com.tiktok.sparkling.method.router.open.RouterOpenMethod
import com.tiktok.sparkling.method.router.utils.IHostRouterDepend
import com.tiktok.sparkling.method.router.utils.RouterProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class RouterMethodUnitTest {
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

    @Test
    fun openMethodFailsWhenSchemeBlank() {
        val method = RouterOpenMethod().apply { setBridgeContext(bridgeContext) }
        val params = mockk<AbsRouterOpenMethodIDL.IDLMethodOpenParamModel>(relaxed = true)
        every { params.scheme } returns "   "
        every { params.replace } returns false

        val callback = OpenCallbackRecorder()
        method.handle(params, callback, BridgePlatformType.LYNX)

        assertTrue(callback.successResult == null)
        assertEquals(IDLBridgeMethod.INVALID_PARAM, callback.failureCode)
        assertTrue(callback.failureMsg?.contains("scheme") == true)
    }

    @Test
    fun openMethodSucceedsWithValidScheme() {
        val hostRouter = mockk<IHostRouterDepend>(relaxed = true)
        every {
            hostRouter.openScheme(any(), any(), any(), any(), any())
        } returns true
        RouterProvider.hostRouterDepend = hostRouter

        val method = RouterOpenMethod().apply { setBridgeContext(bridgeContext) }
        val params = mockk<AbsRouterOpenMethodIDL.IDLMethodOpenParamModel>(relaxed = true)
        every { params.scheme } returns "hybrid://lynxview?bundle=main.lynx.bundle"
        every { params.replace } returns false
        every { params.useSysBrowser } returns true
        every { params.replaceType } returns null
        every { params.extra } returns null

        val callback = OpenCallbackRecorder()
        method.handle(params, callback, BridgePlatformType.LYNX)

        assertNotNull(callback.successResult)
        assertNull(callback.failureCode)
        verify(exactly = 1) {
            hostRouter.openScheme(
                bridgeContext,
                "hybrid://lynxview?bundle=main.lynx.bundle",
                any(),
                BridgePlatformType.LYNX,
                context,
            )
        }
    }

    @Test
    fun openMethodFailsWhenReplaceTypeInvalid() {
        val hostRouter = mockk<IHostRouterDepend>(relaxed = true)
        RouterProvider.hostRouterDepend = hostRouter

        val method = RouterOpenMethod().apply { setBridgeContext(bridgeContext) }
        val params = mockk<AbsRouterOpenMethodIDL.IDLMethodOpenParamModel>(relaxed = true)
        every { params.scheme } returns "hybrid://lynxview?bundle=main.lynx.bundle"
        every { params.replace } returns true
        every { params.replaceType } returns "not-a-valid-type"

        val callback = OpenCallbackRecorder()
        method.handle(params, callback, BridgePlatformType.LYNX)

        assertEquals(IDLBridgeMethod.INVALID_PARAM, callback.failureCode)
        assertTrue(callback.failureMsg?.contains("Invalid replaceType") == true)
    }

    @Test
    fun closeMethodFailsWhenRouterNotRegistered() {
        val method = RouterCloseMethod().apply { setBridgeContext(bridgeContext) }
        val params = mockk<AbsRouterCloseMethodIDL.IDLMethodCloseParamModel>(relaxed = true)
        every { params.containerID } returns "container-1"
        every { params.animated } returns true

        val callback = CloseCallbackRecorder()
        method.handle(params, callback, BridgePlatformType.LYNX)

        assertEquals(IDLBridgeMethod.FAIL, callback.failureCode)
        assertTrue(callback.failureMsg?.contains("Router service not available") == true)
    }

    @Test
    fun closeMethodSucceedsWhenRouterClosesView() {
        val hostRouter = mockk<IHostRouterDepend>(relaxed = true)
        every {
            hostRouter.closeView(any(), any(), any(), any())
        } returns true
        RouterProvider.hostRouterDepend = hostRouter

        val method = RouterCloseMethod().apply { setBridgeContext(bridgeContext) }
        val params = mockk<AbsRouterCloseMethodIDL.IDLMethodCloseParamModel>(relaxed = true)
        every { params.containerID } returns "container-2"
        every { params.animated } returns false

        val callback = CloseCallbackRecorder()
        method.handle(params, callback, BridgePlatformType.LYNX)

        assertNotNull(callback.successResult)
        assertNull(callback.failureCode)
        verify(exactly = 1) {
            hostRouter.closeView(bridgeContext, BridgePlatformType.LYNX, "container-2", false)
        }
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
}
