// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import android.os.Parcel
import android.os.Parcelable
import android.view.View
import com.lynx.tasm.LynxViewBuilder
import com.tiktok.sparkling.hybridkit.lynx.LynxKitInitParams
import java.io.Serializable

/**
 * A fixed Lynx viewport in physical pixels.
 *
 * Both dimensions must be positive because Sparkling applies them as exact Lynx measure specs and
 * as the hosted Lynx view's layout size.
 */
data class SparklingLynxViewport(
    val widthPx: Int,
    val heightPx: Int,
) : Parcelable,
    Serializable {
    init {
        require(widthPx in 1..MAX_MEASURE_SPEC_SIZE_PX) {
            "Viewport width must be between 1 and $MAX_MEASURE_SPEC_SIZE_PX"
        }
        require(heightPx in 1..MAX_MEASURE_SPEC_SIZE_PX) {
            "Viewport height must be between 1 and $MAX_MEASURE_SPEC_SIZE_PX"
        }
    }

    private constructor(parcel: Parcel) : this(
        widthPx = parcel.readInt(),
        heightPx = parcel.readInt(),
    )

    override fun writeToParcel(
        parcel: Parcel,
        flags: Int,
    ) {
        parcel.writeInt(widthPx)
        parcel.writeInt(heightPx)
    }

    override fun describeContents(): Int = 0

    companion object {
        private const val MAX_MEASURE_SPEC_SIZE_PX = 0x3fffffff

        @JvmField
        val CREATOR: Parcelable.Creator<SparklingLynxViewport> =
            object : Parcelable.Creator<SparklingLynxViewport> {
                override fun createFromParcel(parcel: Parcel): SparklingLynxViewport = SparklingLynxViewport(parcel)

                override fun newArray(size: Int): Array<SparklingLynxViewport?> = arrayOfNulls(size)
            }

        internal fun fromRawDimensions(
            width: String?,
            height: String?,
        ): SparklingLynxViewport? {
            val widthPx = width?.toIntOrNull() ?: return null
            val heightPx = height?.toIntOrNull() ?: return null
            if (
                widthPx !in 1..MAX_MEASURE_SPEC_SIZE_PX ||
                heightPx !in 1..MAX_MEASURE_SPEC_SIZE_PX
            ) {
                return null
            }
            return SparklingLynxViewport(widthPx, heightPx)
        }
    }
}

internal fun SparklingContext.resolveLynxViewport(): SparklingLynxViewport? =
    (hybridParams as? LynxKitInitParams)?.lynxViewport
        ?: lynxViewport
        ?: hybridSchemeParam?.lynxViewport

internal fun LynxViewBuilder.applyLynxViewport(viewport: SparklingLynxViewport) {
    setPresetMeasuredSpec(
        View.MeasureSpec.makeMeasureSpec(viewport.widthPx, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(viewport.heightPx, View.MeasureSpec.EXACTLY),
    )
}
