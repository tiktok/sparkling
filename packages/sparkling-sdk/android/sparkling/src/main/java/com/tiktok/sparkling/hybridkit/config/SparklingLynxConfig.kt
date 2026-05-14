// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.config

import android.app.Application
import com.lynx.tasm.INativeLibraryLoader
import com.lynx.tasm.LynxEnv
import com.lynx.tasm.base.LLog
import com.lynx.tasm.behavior.Behavior
import com.lynx.tasm.provider.AbsTemplateProvider
import com.tiktok.sparkling.hybridkit.lynx.SparklingLynxModuleWrapper

class SparklingLynxConfig private constructor(
    context: Application,
    val isCheckPropsSetter: Boolean,
    val libraryLoader: INativeLibraryLoader?,
    val templateProvider: AbsTemplateProvider?,
    val globalBehaviors: MutableList<Behavior>,
    val globalModules: MutableMap<String, SparklingLynxModuleWrapper>,
    val additionInit: LynxEnv.() -> Unit,
    val logLevel: Int = LLog.INFO,
) : ILynxConfig {
    companion object {
        inline fun build(
            context: Application,
            block: Builder.() -> Unit,
        ) = Builder(context).apply(block).build()
    }

    class Builder(
        var context: Application,
    ) {
        private var isCheckPropsSetter = true
        private var libraryLoader: INativeLibraryLoader? = null
        private var templateProvider: AbsTemplateProvider? = null
        private val globalBehaviors = mutableListOf<Behavior>()
        private val globalModules = mutableMapOf<String, SparklingLynxModuleWrapper>()
        private var additionInit: LynxEnv.() -> Unit = {}

        fun setCheckPropsSetter(checkPropsSetter: Boolean) {
            isCheckPropsSetter = checkPropsSetter
        }

        fun setLibraryLoader(libraryLoader: INativeLibraryLoader?) {
            this.libraryLoader = libraryLoader
        }

        fun setTemplateProvider(templateProvider: AbsTemplateProvider?) {
            this.templateProvider = templateProvider
        }

        fun addBehaviors(behaviors: List<Behavior>) {
            globalBehaviors.addAll(behaviors)
        }

        fun addLynxModules(modules: Map<String, SparklingLynxModuleWrapper>) {
            globalModules.putAll(modules)
        }

        fun setAdditionInit(additionInit: LynxEnv.() -> Unit) {
            this.additionInit = additionInit
        }

        fun build() =
            SparklingLynxConfig(
                context,
                isCheckPropsSetter,
                libraryLoader,
                templateProvider,
                globalBehaviors,
                globalModules,
                additionInit,
            )
    }
}
