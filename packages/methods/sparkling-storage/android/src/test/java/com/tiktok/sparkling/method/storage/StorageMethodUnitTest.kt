package com.tiktok.sparkling.method.storage

import android.app.Application
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.model.idl.CompletionBlock
import com.tiktok.sparkling.method.runtime.depend.BridgeBaseRuntime
import com.tiktok.sparkling.method.storage.getItem.AbsStorageGetItemMethodIDL
import com.tiktok.sparkling.method.storage.getItem.StorageGetItemMethod
import com.tiktok.sparkling.method.storage.removeItem.AbsStorageRemoveItemMethodIDL
import com.tiktok.sparkling.method.storage.removeItem.StorageRemoveItemMethod
import com.tiktok.sparkling.method.storage.setItem.AbsStorageSetItemMethodIDL
import com.tiktok.sparkling.method.storage.setItem.StorageSetItemMethod
import com.tiktok.sparkling.method.storage.utils.NativeProviderFactory
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class StorageMethodUnitTest {
    private lateinit var app: Application

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        BridgeBaseRuntime.applicationContext = app
    }

    @After
    fun tearDown() {
        BridgeBaseRuntime.applicationContext = null
    }

    @Test
    fun setGetRemoveFlowWorksForStringValue() {
        val setMethod = StorageSetItemMethod()
        val getMethod = StorageGetItemMethod()
        val removeMethod = StorageRemoveItemMethod()

        val setParams = mockk<AbsStorageSetItemMethodIDL.IDLMethodSetStorageItemParamModel>(relaxed = true)
        every { setParams.key } returns "case-key"
        every { setParams.data } returns "case-value"
        every { setParams.biz } returns null
        every { setParams.validDuration } returns null

        val setCallback = SetCallbackRecorder()
        setMethod.handle(setParams, setCallback, BridgePlatformType.LYNX)
        assertNotNull(setCallback.successResult)

        val getParams = mockk<AbsStorageGetItemMethodIDL.IDLMethodGetStorageItemParamModel>(relaxed = true)
        every { getParams.key } returns "case-key"
        every { getParams.biz } returns null

        val getCallback = GetCallbackRecorder()
        getMethod.handle(getParams, getCallback, BridgePlatformType.LYNX)
        assertNotNull(getCallback.successResult)
        assertEquals("case-value", getCallback.successResult?.data)

        val removeParams = mockk<AbsStorageRemoveItemMethodIDL.IDLMethodRemoveStorageItemParamModel>(relaxed = true)
        every { removeParams.key } returns "case-key"
        every { removeParams.biz } returns null

        val removeCallback = RemoveCallbackRecorder()
        removeMethod.handle(removeParams, removeCallback, BridgePlatformType.LYNX)
        assertNotNull(removeCallback.successResult)

        val afterRemove = NativeProviderFactory.providerNativeStorage(app).getStorageItem("case-key")
        assertNull(afterRemove)
    }

    @Test
    fun setMethodFailsForUnsupportedType() {
        val setMethod = StorageSetItemMethod()
        val setParams = mockk<AbsStorageSetItemMethodIDL.IDLMethodSetStorageItemParamModel>(relaxed = true)
        every { setParams.key } returns "unsupported"
        every { setParams.data } returns Any()
        every { setParams.biz } returns "biz"
        every { setParams.validDuration } returns null

        val callback = SetCallbackRecorder()
        setMethod.handle(setParams, callback, BridgePlatformType.LYNX)

        assertEquals(IDLBridgeMethod.INVALID_PARAM, callback.failureCode)
        assertTrue(callback.failureMsg?.contains("Illegal value type") == true)
    }

    @Test
    fun getMethodFailsWhenKeyEmpty() {
        val getMethod = StorageGetItemMethod()
        val params = mockk<AbsStorageGetItemMethodIDL.IDLMethodGetStorageItemParamModel>(relaxed = true)
        every { params.key } returns ""
        every { params.biz } returns null

        val callback = GetCallbackRecorder()
        getMethod.handle(params, callback, BridgePlatformType.LYNX)

        assertEquals(IDLBridgeMethod.INVALID_PARAM, callback.failureCode)
        assertTrue(callback.failureMsg?.contains("Key in the params is empty") == true)
    }

    private class SetCallbackRecorder : CompletionBlock<AbsStorageSetItemMethodIDL.IDLMethodSetStorageItemResultModel> {
        var successResult: AbsStorageSetItemMethodIDL.IDLMethodSetStorageItemResultModel? = null
        var failureCode: Int? = null
        var failureMsg: String? = null

        override fun onSuccess(
            result: AbsStorageSetItemMethodIDL.IDLMethodSetStorageItemResultModel,
            msg: String,
        ) {
            successResult = result
        }

        override fun onFailure(
            code: Int,
            msg: String,
            data: AbsStorageSetItemMethodIDL.IDLMethodSetStorageItemResultModel?,
        ) {
            failureCode = code
            failureMsg = msg
        }

        override fun onRawSuccess(data: AbsStorageSetItemMethodIDL.IDLMethodSetStorageItemResultModel?) = Unit
    }

    private class GetCallbackRecorder : CompletionBlock<AbsStorageGetItemMethodIDL.IDLMethodGetStorageItemResultModel> {
        var successResult: AbsStorageGetItemMethodIDL.IDLMethodGetStorageItemResultModel? = null
        var failureCode: Int? = null
        var failureMsg: String? = null

        override fun onSuccess(
            result: AbsStorageGetItemMethodIDL.IDLMethodGetStorageItemResultModel,
            msg: String,
        ) {
            successResult = result
        }

        override fun onFailure(
            code: Int,
            msg: String,
            data: AbsStorageGetItemMethodIDL.IDLMethodGetStorageItemResultModel?,
        ) {
            failureCode = code
            failureMsg = msg
        }

        override fun onRawSuccess(data: AbsStorageGetItemMethodIDL.IDLMethodGetStorageItemResultModel?) = Unit
    }

    private class RemoveCallbackRecorder : CompletionBlock<AbsStorageRemoveItemMethodIDL.IDLMethodRemoveStorageItemResultModel> {
        var successResult: AbsStorageRemoveItemMethodIDL.IDLMethodRemoveStorageItemResultModel? = null

        override fun onSuccess(
            result: AbsStorageRemoveItemMethodIDL.IDLMethodRemoveStorageItemResultModel,
            msg: String,
        ) {
            successResult = result
        }

        override fun onFailure(
            code: Int,
            msg: String,
            data: AbsStorageRemoveItemMethodIDL.IDLMethodRemoveStorageItemResultModel?,
        ) = Unit

        override fun onRawSuccess(data: AbsStorageRemoveItemMethodIDL.IDLMethodRemoveStorageItemResultModel?) = Unit
    }
}
