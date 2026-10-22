// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debug

import androidx.fragment.app.FragmentActivity
import com.tiktok.sparkling.hybridkit.base.IKitView

/**
 * SPI implemented by optional debug-tool packages.
 *
 * The runtime module discovers implementations through [java.util.ServiceLoader]
 * so release builds can omit the debug tool without reflective class probing.
 */
interface SparklingDebugToolProvider {
    fun openInspectorPanel(activity: FragmentActivity)

    fun onKitViewCreated(
        containerId: String,
        kitView: IKitView,
    ) = Unit

    fun onKitViewDestroyed(
        containerId: String,
        kitView: IKitView?,
    ) = Unit

    fun setGlobalPropsCollector(collector: () -> Map<String, IKitView>) = Unit
}
