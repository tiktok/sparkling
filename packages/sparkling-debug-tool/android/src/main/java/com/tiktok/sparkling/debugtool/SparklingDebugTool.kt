// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool

import android.app.Application
import com.lynx.devtool.LynxDevtoolEnv
import com.lynx.service.devtool.LynxDevToolService
import com.lynx.tasm.LynxEnv
import com.lynx.tasm.service.LynxServiceCenter

object SparklingDebugTool {
    @JvmStatic
    fun init(application: Application) {
        LynxServiceCenter.inst().registerService(LynxDevToolService.INSTANCE)
        LynxEnv.inst().enableLynxDebug(true)
        LynxEnv.inst().enableDevtool(true)
        LynxEnv.inst().enableLogBox(true)
        LynxDevtoolEnv.inst().enableLongPressMenu(true)
    }
}
