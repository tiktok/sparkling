// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.scheme

import android.os.Parcel
import android.os.Parcelable
import com.tiktok.sparkling.hybridkit.base.HybridContainerType
import com.tiktok.sparkling.hybridkit.base.HybridKitType
import java.io.Serializable

open class HybridSchemeParam(
    override var engineType: HybridKitType = HybridKitType.UNKNOWN,
    var containerType: HybridContainerType = HybridContainerType.UNKNOWN,
    var bundle: String? = null,
    var title: String? = null,
    var titleColor: String? = null,
    var hideNavBar: Boolean = false,
    var navBarColor: String? = null,
    var screenOrientation: String? = null,
    var hideStatusBar: Boolean = false,
    var transStatusBar: Boolean = false,
    var hideLoading: Boolean = false,
    var loadingBgColor: String? = null,
    var containerBgColor: String? = null,
    var showNavBarInTransStatusBar: Boolean = false,
    var hideError: Boolean = false,
//    var fallbackUrl: String? = null,
    var forceThemeStyle: String? = null
) : BaseSchemeParam(engineType), Serializable, Parcelable {

    constructor(parcel: Parcel) : this(
        HybridKitType.values()[parcel.readInt()],
        HybridContainerType.values()[parcel.readInt()],
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readByte() != 0.toByte(),
        parcel.readString(),
        parcel.readString(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readString(),
        parcel.readString(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(engineType.ordinal)
        parcel.writeInt(containerType.ordinal)
        parcel.writeString(bundle)
        parcel.writeString(title)
        parcel.writeString(titleColor)
        parcel.writeByte(if (hideNavBar) 1 else 0)
        parcel.writeString(navBarColor)
        parcel.writeString(screenOrientation)
        parcel.writeByte(if (hideStatusBar) 1 else 0)
        parcel.writeByte(if (transStatusBar) 1 else 0)
        parcel.writeByte(if (hideLoading) 1 else 0)
        parcel.writeString(loadingBgColor)
        parcel.writeString(containerBgColor)
        parcel.writeByte(if (showNavBarInTransStatusBar) 1 else 0)
        parcel.writeByte(if (hideError) 1 else 0)
//        parcel.writeString(fallbackUrl)
        parcel.writeString(forceThemeStyle)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        const val DefaultPresetWidth = -1
        const val DefaultPresetHeight = -1
        @JvmField
        val CREATOR : Parcelable.Creator<HybridSchemeParam> = object : Parcelable.Creator<HybridSchemeParam> {
            override fun createFromParcel(parcel: Parcel): HybridSchemeParam {
                return HybridSchemeParam(parcel)
            }

            override fun newArray(size: Int): Array<HybridSchemeParam?> {
                return arrayOfNulls(size)
            }
        }
    }


}
