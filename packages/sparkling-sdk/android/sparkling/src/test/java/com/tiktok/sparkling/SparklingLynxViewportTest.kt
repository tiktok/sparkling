// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import android.os.Parcel
import android.view.View
import com.lynx.tasm.LynxViewBuilder
import com.tiktok.sparkling.hybridkit.lynx.LynxKitInitParams
import com.tiktok.sparkling.hybridkit.scheme.HybridSchemeParam
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SparklingLynxViewportTest {
    @Test
    fun viewportRejectsUnsafeDimensions() {
        assertThrows<IllegalArgumentException> { SparklingLynxViewport(0, 100) }
        assertThrows<IllegalArgumentException> { SparklingLynxViewport(100, -1) }
        assertThrows<IllegalArgumentException> { SparklingLynxViewport(Int.MAX_VALUE, 100) }
    }

    @Test
    fun rawDimensionsRequireCompletePositiveIntegerPair() {
        assertEquals(
            SparklingLynxViewport(320, 640),
            SparklingLynxViewport.fromRawDimensions("320", "640"),
        )
        assertNull(SparklingLynxViewport.fromRawDimensions("320", null))
        assertNull(SparklingLynxViewport.fromRawDimensions(null, "640"))
        assertNull(SparklingLynxViewport.fromRawDimensions("0", "640"))
        assertNull(SparklingLynxViewport.fromRawDimensions("320.5", "640"))
        assertNull(SparklingLynxViewport.fromRawDimensions("2147483647", "640"))
    }

    @Test
    fun viewportRoundTripsThroughParcel() {
        val parcel = Parcel.obtain()
        val expected = SparklingLynxViewport(375, 812)
        expected.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val actual = SparklingLynxViewport.CREATOR.createFromParcel(parcel)

        assertEquals(expected, actual)
        parcel.recycle()
    }

    @Test
    fun schemeViewportRoundTripsThroughParcel() {
        val parcel = Parcel.obtain()
        val expected =
            HybridSchemeParam().apply {
                lynxViewport = SparklingLynxViewport(375, 812)
            }
        expected.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val actual = HybridSchemeParam.CREATOR.createFromParcel(parcel)

        assertEquals(expected.lynxViewport, actual.lynxViewport)
        parcel.recycle()
    }

    @Test
    fun initParamsOverrideContextAndSchemeViewport() {
        val context =
            SparklingContext().apply {
                lynxViewport = SparklingLynxViewport(200, 300)
                hybridSchemeParam =
                    HybridSchemeParam().apply {
                        lynxViewport = SparklingLynxViewport(100, 150)
                    }
                hybridParams =
                    LynxKitInitParams(loadUri = null).apply {
                        lynxViewport = SparklingLynxViewport(400, 500)
                    }
            }

        assertEquals(SparklingLynxViewport(400, 500), context.resolveLynxViewport())
    }

    @Test
    fun contextViewportOverridesSchemeViewport() {
        val context =
            SparklingContext().apply {
                lynxViewport = SparklingLynxViewport(200, 300)
                hybridSchemeParam =
                    HybridSchemeParam().apply {
                        lynxViewport = SparklingLynxViewport(100, 150)
                    }
            }

        assertEquals(SparklingLynxViewport(200, 300), context.resolveLynxViewport())
    }

    @Test
    fun builderReceivesExactViewportMeasureSpecs() {
        val builder = LynxViewBuilder()

        builder.applyLynxViewport(SparklingLynxViewport(360, 780))

        assertEquals(View.MeasureSpec.EXACTLY, View.MeasureSpec.getMode(builder.presetWidthMeasureSpec))
        assertEquals(360, View.MeasureSpec.getSize(builder.presetWidthMeasureSpec))
        assertEquals(View.MeasureSpec.EXACTLY, View.MeasureSpec.getMode(builder.presetHeightMeasureSpec))
        assertEquals(780, View.MeasureSpec.getSize(builder.presetHeightMeasureSpec))
    }

    private inline fun <reified T : Throwable> assertThrows(block: () -> Unit) {
        try {
            block()
        } catch (throwable: Throwable) {
            if (throwable is T) {
                return
            }
            throw AssertionError("Expected ${T::class.java.name}, got ${throwable::class.java.name}", throwable)
        }
        throw AssertionError("Expected ${T::class.java.name}")
    }
}
