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
import com.tiktok.sparkling.SparklingThreadStrategy
import com.tiktok.sparkling.hybridkit.lynx.SparklingLynxModuleWrapper

class SparklingLynxConfig private constructor(
    context: Application,
    val isCheckPropsSetter: Boolean,
    val libraryLoader: INativeLibraryLoader?,
    val templateProvider: AbsTemplateProvider?,
    val globalBehaviors: MutableList<Behavior>,
    val globalModules: MutableMap<String, SparklingLynxModuleWrapper>,
    val additionInit: LynxEnv.() -> Unit,
    val sharedProcessDensityOverride: Float?,
    val defaultThreadStrategy: SparklingThreadStrategy?,
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
        private var sharedProcessDensityOverride: Float? = null
        private var defaultThreadStrategy: SparklingThreadStrategy? = null

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

        /**
         * Overrides Lynx density for the entire host process.
         *
         * Lynx requires every LynxView in a process to use the same override. Hosts that enable
         * this must configure non-Sparkling LynxViews with the same density.
         */
        fun setSharedProcessDensityOverride(density: Float) {
            require(density.isFinite() && density > 0f) {
                "Shared-process Lynx density must be finite and greater than 0."
            }
            sharedProcessDensityOverride = density
        }

        fun setDefaultThreadStrategy(threadStrategy: SparklingThreadStrategy?) {
            defaultThreadStrategy = threadStrategy
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
                sharedProcessDensityOverride,
                defaultThreadStrategy,
            )
    }
}
