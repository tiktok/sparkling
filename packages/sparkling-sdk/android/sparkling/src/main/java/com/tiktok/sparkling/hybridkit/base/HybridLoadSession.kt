// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.hybridkit.base

import com.tiktok.sparkling.hybridkit.utils.GsonUtils
import org.json.JSONObject

/**
 * @Author: linshuhao
 * @Description:
 */
class HybridLoadSession {
    /**
     * session id
     */
    var sessionId: String? = null

    var clickTime: Long? = null

    var openTime: Long? = null

    var lynxViewInitStart: Long? = null

    var lynxViewInitEnd: Long? = null

    var customInitStart: Long? = null

    var customInitEnd: Long? = null

    var prepareInitDataStart: Long? = null

    var prepareInitDataEnd: Long? = null

    var prepareFontStart: Long? = null

    var prepareFontEnd: Long? = null

    var prepareGlobalPropsStart: Long? = null

    var prepareGlobalPropsEnd: Long? = null

    var pluginExecuteStart: Long? = null

    var pluginExecuteEnd: Long? = null

    var prepareComponentStart: Long? = null

    var prepareComponentEnd: Long? = null

    var prepareTemplateStart: Long? = null

    var prepareTemplateEnd: Long? = null

    var prepareExtraInfoStart: Long? = 0

    var prepareExtraInfoEnd: Long? = 0

    var prepareJSBStart: Long? = null

    var prepareJSBEnd: Long? = null

    var loadEngineAndTemplateDataStart: Long? = null

    var startLoadTime: Long? = null

    var loadEngineStart: Long? = null

    var loadEngineEnd: Long? = null

    var extraRecord = mutableMapOf<String, Long>()

    var pluginInfos = mutableMapOf<String, Long>()

    override fun toString(): String = SnapShot(loadSession = this).toString()

    fun toJSONObject(): JSONObject = SnapShot(loadSession = this).toJSONObject()

    fun Long?.value(): Long = this ?: 0L

    class SnapShot(
        loadSession: HybridLoadSession,
    ) {
        init {
            loadSession.apply {
                total = loadEngineEnd.value() - openTime.value()
                init2StartRender = loadEngineStart.value() - openTime.value()
                renderCost = loadEngineEnd.value() - loadEngineStart.value()
                prepareTemplateCost = prepareTemplateEnd.value() - prepareTemplateStart.value()
                prepareJSBCost = prepareJSBEnd.value() - prepareJSBStart.value()
                preparePluginExecuteCost = pluginExecuteEnd.value() - pluginExecuteStart.value()
                prepareFontCost = prepareFontEnd.value() - prepareFontStart.value()
                lynxViewInitCost = lynxViewInitEnd.value() - lynxViewInitStart.value()
                lynxCost = lynxViewInitCost + renderCost
                prepareInitDataCost = prepareInitDataEnd.value() - prepareInitDataStart.value()
                prepareComponentCost = prepareComponentEnd.value() - prepareComponentStart.value()
                prepareGlobalPropsCost = prepareGlobalPropsEnd.value() - prepareGlobalPropsStart.value()
                prepareComponentEnd2PrepareTemplateStart = prepareTemplateStart.value() - prepareComponentEnd.value()
                sparkContainerCost = loadEngineStart.value() - openTime.value() - lynxViewInitCost - prepareComponentEnd2PrepareTemplateStart
                prepareExtraInfoCost = prepareExtraInfoEnd.value() - prepareExtraInfoStart.value()
                customInitCost = customInitEnd.value() - customInitStart.value()
                pluginInfoStr = GsonUtils.toJson(pluginInfos)
            }
        }

        private var total = 0L
        private var init2StartRender = 0L
        private var renderCost = 0L
        private var prepareTemplateCost = 0L
        private var prepareExtraInfoCost = 0L
        private var prepareJSBCost = 0L
        private var preparePluginExecuteCost = 0L
        private var prepareFontCost = 0L
        private var sparkContainerCost = 0L
        private var lynxCost = 0L
        private var lynxViewInitCost = 0L
        private var customInitCost = 0L
        private var prepareInitDataCost = 0L
        private var prepareComponentCost = 0L
        private var prepareGlobalPropsCost = 0L
        private var prepareComponentEnd2PrepareTemplateStart = 0L
        private var pluginInfoStr: String? = null

        override fun toString(): String =
            "total = $total , init2StartRender = $init2StartRender, renderCost = $renderCost, prepareTemplateCost = $prepareTemplateCost, " +
                "prepareJSBCost = $prepareJSBCost, preparePluginExecuteCost = $preparePluginExecuteCost, prepareFontCost = $prepareFontCost, " +
                "sparkContainerCost = $sparkContainerCost, lynxCost = $lynxCost, lynxViewInitCost = $lynxViewInitCost, prepareInitDataCost = $prepareInitDataCost, " +
                "prepareComponentCost = $prepareComponentCost, prepareGlobalPropsCost = $prepareGlobalPropsCost, prepareComponentEnd2PrepareTemplateStart = $prepareComponentEnd2PrepareTemplateStart, " +
                "customInitCost = $customInitCost, prepareExtraInfoCost = $prepareExtraInfoCost, pluginInfos = $pluginInfoStr"

        fun toJSONObject(): JSONObject =
            JSONObject().apply {
                put("total", total)
                put("init2StartRender", init2StartRender)
                put("renderCost", renderCost)
                put("prepareTemplateCost", prepareTemplateCost)
                put("prepareJSBCost", prepareJSBCost)
                put("preparePluginExecuteCost", preparePluginExecuteCost)
                put("prepareFontCost", prepareFontCost)
                put("sparkContainerCost", sparkContainerCost)
                put("lynxCost", lynxCost)
                put("lynxViewInitCost", lynxViewInitCost)
                put("prepareInitDataCost", prepareInitDataCost)
                put("prepareComponentCost", prepareComponentCost)
                put("prepareGlobalPropsCost", prepareGlobalPropsCost)
                put("prepareComponentEnd2PrepareTemplateStart", prepareComponentEnd2PrepareTemplateStart)
                put("prepareExtraInfoCost", prepareExtraInfoCost)
                put("customInitCost", customInitCost)
                put("pluginInfos", pluginInfoStr)
            }
    }
}
