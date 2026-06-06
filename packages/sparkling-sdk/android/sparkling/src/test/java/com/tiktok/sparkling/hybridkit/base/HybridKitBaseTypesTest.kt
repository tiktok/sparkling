package com.tiktok.sparkling.hybridkit.base

import com.tiktok.sparkling.hybridkit.HybridContext
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Coverage for the simple data/value classes in [com.tiktok.sparkling.hybridkit.base]:
 * [HybridKitError], [LoadUrlCheck], the [AbsHybridKitLifeCycle] linked-list contract,
 * and the default no-op implementations of [IHybridKitLifeCycle].
 */
class HybridKitBaseTypesTest {
    @Test
    fun hybridKitErrorPrintsErrorMessage() {
        val err =
            HybridKitError().apply {
                errorCode = 210
                errorReason = "lynx load failure"
                originCode = 500
                originReason = "underlying"
            }
        assertEquals(210, err.errorCode)
        assertEquals("lynx load failure", err.errorReason)
        assertEquals(500, err.originCode)
        assertEquals("underlying", err.originReason)
        val printed = err.printErrorMsg()
        assertTrue(printed.contains("210"))
        assertTrue(printed.contains("lynx load failure"))
    }

    @Test
    fun hybridErrorConstantCodeExposesLynxErrorCode() {
        assertEquals(210, HybridErrorConstantCode.LynxLoadError)
    }

    @Test
    fun loadUrlCheckHoldsModifiedAndChangedUrl() {
        val check = LoadUrlCheck(modified = true, changedUrl = "spark://changed")
        assertTrue(check.modified)
        assertEquals("spark://changed", check.changedUrl)
    }

    @Test
    fun absHybridKitLifeCycleNextChainCanBeReadAndWritten() {
        val first = TestLifeCycle()
        val second = TestLifeCycle()

        assertNull(first.next())
        first.next(second)
        assertSame(second, first.next())
    }

    @Test
    fun iHybridKitLifeCycleDefaultMethodsAreNoOpsAndReturnNullCheck() {
        val lifecycle = TestLifeCycle()
        val view = mockk<IKitView>(relaxed = true)

        // exercise default no-op overrides
        lifecycle.onPreKitCreate()
        lifecycle.onPostKitCreated(view)
        lifecycle.onLoadStart(view, "url")
        lifecycle.onLoadFinish(view)
        lifecycle.onDestroy(view)
        lifecycle.onClearContext()
        lifecycle.onResourceLoadFinish(null, null)
        lifecycle.onEffectiveDrawCallback()
        lifecycle.onRuntimeReady(HybridKitType.LYNX)

        // the 2-arg onLoadFailed delegates to the 1-arg form
        lifecycle.onLoadFailed(view, "u")
        // the 3-arg-with-error delegates through onLoadFailed(view, url, reason)
        lifecycle.onLoadFailed(view, "u", HybridKitError().apply { errorReason = "boom" })

        assertNull(lifecycle.onLoadUrlCheck(view, "ignored"))
    }

    private class TestLifeCycle : AbsHybridKitLifeCycle()
}
