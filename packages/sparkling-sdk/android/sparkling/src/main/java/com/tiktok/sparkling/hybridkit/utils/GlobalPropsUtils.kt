// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.utils

import android.app.Activity
import android.content.Context
import com.lynx.tasm.LynxEnv
import com.tiktok.sparkling.hybridkit.HybridContext
import com.tiktok.sparkling.hybridkit.HybridEnvironment
import com.tiktok.sparkling.hybridkit.base.HybridLoadSession
import com.tiktok.sparkling.hybridkit.base.Theme
import com.tiktok.sparkling.hybridkit.config.RuntimeInfo
import com.tiktok.sparkling.hybridkit.scheme.SparklingUriParser
import androidx.core.net.toUri
import java.util.concurrent.ConcurrentHashMap


/**
 * When setting globalprops, consider whether the props will change throughout the app lifecycle
 * will change -> setUnstableProps -> unstableMap
 * will not change -> setStableProps -> stableMap
 */
class GlobalPropsUtils {

    /** for global eg. screen size or OS or app version */
    private val stableMap = SafeConcurrentHashMap<String, Any>()

    /** for container eg. container id or url */
    private val unstableMap = SafeConcurrentHashMap<String, SafeConcurrentHashMap<String, Any>>()
    private val removableGlobalKeys = SafeConcurrentHashMap<String, MutableList<String>>()

    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            GlobalPropsUtils()
        }

        val builtInStableFields = mapOf<String, () -> Any>(
            RuntimeInfo.LYNX_SDK_VERSION to { LynxEnv.inst().lynxVersion },
            RuntimeInfo.SCREEN_WIDTH to {
                DevicesUtil.px2dp(
                    DevicesUtil.getScreenWidth(HybridEnvironment.instance.context).toDouble(),
                    HybridEnvironment.instance.context
                )
            },
            RuntimeInfo.SCREEN_HEIGHT to {
                DevicesUtil.px2dp(
                    DevicesUtil.getScreenHeight(HybridEnvironment.instance.context).toDouble(),
                    HybridEnvironment.instance.context
                )
            },
            RuntimeInfo.STATUS_BAR_HEIGHT to {
                DevicesUtil.px2dp(
                    DevicesUtil.getStatusBarHeight(HybridEnvironment.instance.context).toDouble(),
                    HybridEnvironment.instance.context
                )
            },
            RuntimeInfo.SCREEN_ORIENTATION to {
                val isPortrait = DevicesUtil.isScreenPortrait(HybridEnvironment.instance.context)
                if (isPortrait) "Portrait" else "Landscape"
            },
            RuntimeInfo.ORIENTATION to {
                val isPortrait = DevicesUtil.isScreenPortrait(HybridEnvironment.instance.context)
                if (isPortrait) 0 else 1
            },
            RuntimeInfo.DEVICE_MODEL to { DevicesUtil.model },
            RuntimeInfo.OS to { DevicesUtil.platform },
            RuntimeInfo.OS_VERSION to { DevicesUtil.system },
            RuntimeInfo.LANGUAGE to { DevicesUtil.language },
            RuntimeInfo.IS_LOW_POWER_MODE to { if (DevicesUtil.isLowPowerMode(HybridEnvironment.instance.context)) 1 else 0 },
            RuntimeInfo.A11Y_MODE to { if (DevicesUtil.isTalkBackEnabled(HybridEnvironment.instance.context)) 1 else 0 },
            RuntimeInfo.SAFEAREA_HEIGHT to {
                val ctx = HybridEnvironment.instance.context
                val activity = ctx as? Activity
                if (activity != null) {
                    val statusBarHeightDp = DevicesUtil.px2dp(
                        DevicesUtil.getStatusBarHeight(activity).toDouble(), activity
                    )
                    DevicesUtil.safeAreaHeight(statusBarHeightDp, activity)
                } else {
                    DevicesUtil.px2dp(
                        DevicesUtil.getScreenHeight(ctx).toDouble(), ctx
                    ) - DevicesUtil.px2dp(
                        DevicesUtil.getStatusBarHeight(ctx).toDouble(), ctx
                    )
                }
            },
            RuntimeInfo.IS_PAD to {
                if (DevicesUtil.isPad(HybridEnvironment.instance.context)) 1 else 0
            },
            RuntimeInfo.NAVIGATION_BAR_HEIGHT to {
                DevicesUtil.px2dp(
                    DevicesUtil.getNavigationBarHeight(HybridEnvironment.instance.context).toDouble(),
                    HybridEnvironment.instance.context
                )
            },
            RuntimeInfo.PIXEL_RATIO to {
                DevicesUtil.getPixelRadio(HybridEnvironment.instance.context)
            }
        )
    }

    fun init(hybridContext: HybridContext, context: Context) {
        val containerID = hybridContext.containerId

        hybridContext.scheme?.let { scheme ->
            runCatching {
                val uri = scheme.toUri()
                val queryMap = SparklingUriParser.parseQueryMap(
                    uri,
                    hybridContext.extra,
                    hybridContext.bundle
                )
                SparklingUriParser.saveUriAndQueries(containerID, queryMap)
                hybridContext.fullScheme =
                    SparklingUriParser.appendExtraQueryToUri(uri, queryMap).toString()
            }
        }

        if (reGenerateCheck()) {
            HybridEnvironment.instance.baseInfoConfig?.let {
                stableMap.putAll(it)
            }

            HybridEnvironment.instance.baseInfoConfig?.getStableProps(context)?.let { map ->
                stableMap.putAll(map)
            }

            stablePropsSupplier()
        }

        HybridEnvironment.instance.baseInfoConfig?.getUnstableProps(context, hybridContext)
            ?.let { map ->
                findContainerProps(containerID).putAll(map)
            }

        hybridContext.runtimeInfo.takeIf { it.size > 0 }?.let {
            findContainerProps(containerID).putAll(it)
        }

        unstablePropsSupplier(hybridContext, context)
    }

    fun updateUnstablePropsSupplier(
        hybridContext: HybridContext,
        context: Context
    ): ConcurrentHashMap<String, Any> {
        return unstablePropsSupplier(hybridContext, context)
    }

    fun setStableProps(props: Map<String, Any>) {
        props.let {
            stableMap.putAll(it)
        }
    }

    fun setUnstableProps(containerID: String, props: Map<String, Any>) {
        props.let {
            findContainerProps(containerID).putAll(it)
        }
    }


    fun getGlobalProps(containerID: String): MutableMap<String, Any> {
        return (stableMap + findContainerProps(containerID)).apply {
            val removableKeys = removableGlobalKeys[containerID]
            if (removableKeys != null) {
                filterNot {
                    removableKeys.contains(it.key)
                }
            }
        }.toMutableMap()
    }

    fun flushGlobalProps(containerID: String) {
        unstableMap.remove(containerID)
    }

    fun removeGlobalProps(containerID: String, list: List<String>?) {
        list?.forEach {
            unstableMap[containerID]?.remove(it)
        }
        if (removableGlobalKeys[containerID] == null) {
            removableGlobalKeys[containerID] = mutableListOf()
        }
        list?.let {
            removableGlobalKeys[containerID]?.addAll(it)
        }
    }

    private fun findContainerProps(containerID: String): ConcurrentHashMap<String, Any> {
        val map = unstableMap[containerID]
        return map ?: SafeConcurrentHashMap<String, Any>().apply { unstableMap[containerID] = this }
    }

    private fun stablePropsSupplier() {
        builtInStableFields.keys.forEach { key ->
            if (stableMap[key] == null) {
                builtInStableFields[key]?.invoke()?.let {
                    stableMap[key] = it
                }
            }
        }
    }

    private fun unstablePropsSupplier(
        hybridContext: HybridContext,
        context: Context?
    ): ConcurrentHashMap<String, Any> {
        val unstableMap = ConcurrentHashMap<String, Any>()
        findContainerProps(hybridContext.containerId).apply {
            unstableMap.apply {

                put(RuntimeInfo.CONTAINER_ID, hybridContext.containerId)
                put(RuntimeInfo.TEMPLATE_RES_DATA, hybridContext.templateResData)
                put(RuntimeInfo.QUERY_ITEMS, parseQueryMap(hybridContext))
                val loadSession = hybridContext.getDependency(HybridLoadSession::class.java)
                put("containerInitTime", loadSession?.openTime.toString())
                val isPortrait = DevicesUtil.isScreenPortrait(HybridEnvironment.instance.context)
                put("screenOrientation", if (isPortrait) "Portrait" else "Landscape")
                put("orientation", if (isPortrait) 0 else 1)
                put("theme", if (hybridContext.getTheme(context) == Theme.DARK) "dark" else "light")
            }
            putAll(unstableMap)
        }
        return unstableMap
    }

    private fun parseQueryMap(hybridContext: HybridContext): MutableMap<String, String> {
        return SparklingUriParser.queryParsedParams(hybridContext.containerId).apply {
            val openTime = hybridContext.getDependency(HybridLoadSession::class.java)?.openTime
                ?: System.currentTimeMillis()
            put("containerInitTime", openTime.toString())
        }
    }

    private fun reGenerateCheck(): Boolean {
        builtInStableFields.keys.forEach { key ->
            if (stableMap[key] == null) {
                return true
            }
        }
        return false
    }

    /**
     * @return Map<String, Any>
     */
    fun getStableGlobalProps(): Map<String, Any> {
        return stableMap
    }
}