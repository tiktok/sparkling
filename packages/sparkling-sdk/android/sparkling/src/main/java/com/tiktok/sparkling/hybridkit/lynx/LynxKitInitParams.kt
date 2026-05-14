// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.lynx

import android.net.Uri
import com.lynx.tasm.LynxBackgroundRuntime
import com.lynx.tasm.LynxViewClient
import com.lynx.tasm.TemplateBundle
import com.lynx.tasm.TemplateData
import com.lynx.tasm.behavior.Behavior
import com.tiktok.sparkling.hybridkit.base.HybridKitType
import com.tiktok.sparkling.hybridkit.base.IKitInitParam
import com.tiktok.sparkling.hybridkit.scheme.HybridSchemeParam
import com.tiktok.sparkling.hybridkit.service.IKitBridgeService
import kotlinx.coroutines.Deferred
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

open class LynxKitInitParams(
    open var lynxModules: MutableMap<String, SparklingLynxModuleWrapper>? = null,
    open var lynxBehaviors: MutableList<Behavior>? = null,
    open var initData: LynxInitData? = null,
    open var preloadFonts: String? = "",
    open override var loadUri: Uri?,
    open var templateBundle: TemplateBundle? = null,
    open var templateBundleDeferred: (() -> Deferred<TemplateBundle?>?)? = null,
    open var templateArray: ByteArray? = null,
) : IKitInitParam {
    constructor(
        lynxModules: MutableMap<String, SparklingLynxModuleWrapper>? = null,
        lynxBehaviors: MutableList<Behavior>? = null,
        initData: LynxInitData? = null,
        preloadFonts: String? = "",
        loadUri: Uri?,
    ) : this(lynxModules, lynxBehaviors, initData, preloadFonts, loadUri, null, null)

    override var type: HybridKitType = HybridKitType.LYNX

    var lynxWidth: Int? = null
    var lynxHeight: Int? = null
    var presetHeightSpec: Int? = HybridSchemeParam.DefaultPresetHeight
    var presetWidthSpec: Int? = HybridSchemeParam.DefaultPresetWidth
    var fontScale: Float? = null
    var templateData: TemplateData? = null
    var extraInfoCallback: ExtraInfoCallback? = null
    var kitBridgeService: IKitBridgeService? = null
    var hybridSchemaParams: HybridSchemeParam? = null
    var lynxBackgroundRuntime: LynxBackgroundRuntime? = null
    private val globalProps = ConcurrentHashMap<String, Any>()
    private var lynxClientDelegate: CopyOnWriteArrayList<LynxViewClient> = CopyOnWriteArrayList()

    override fun applyHybridSchemeParam(hybridSchemeParams: HybridSchemeParam?) {
        this.hybridSchemaParams = hybridSchemeParams
    }

    override fun getHybridSchemeParam(): HybridSchemeParam? = hybridSchemaParams

    override fun setGlobalProps(_globalProps: Map<String, Any>?) {
        _globalProps?.let {
            globalProps.putAll(_globalProps)
        }
    }

    override fun removeGlobalProps(_globalPropsKeys: List<String>?) {
        _globalPropsKeys?.forEach {
            globalProps.remove(it)
        }
    }

    override fun obtainGlobalProps(): Map<String, Any>? = globalProps

    fun addLynxClientDelegate(lynxClientDelegate: LynxViewClient) {
        this.lynxClientDelegate.add(lynxClientDelegate)
    }

    fun lynxClientDelegate(): CopyOnWriteArrayList<LynxViewClient> = lynxClientDelegate

    fun addBehaviours(behaviors: MutableList<Behavior>) {
        if (this.lynxBehaviors == null) {
            this.lynxBehaviors = CopyOnWriteArrayList()
        }
        this.lynxBehaviors?.addAll(behaviors)
    }

    fun getTemplateBundleIncludesDeferred(): TemplateBundle? {
        val deferred = templateBundleDeferred?.invoke()
        return if (deferred?.isCompleted == true) {
            runCatching {
                deferred.getCompleted() ?: templateBundle
            }.getOrDefault(templateBundle)
        } else {
            templateBundle
        }
    }
}

open class ExtraInfoCallback {
    /**
     * callback when getting extra info of template bundle
     */
    open fun extraInfoParsedInPreDecode(extraInfo: Map<String, Any>) {}

    /**
     * callback when getting extra info of template bundle
     */
    open fun extraInfoParsed(extraInfo: Map<String, Any>) {}
}
