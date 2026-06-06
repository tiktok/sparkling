// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.api

import android.content.Context
import android.view.View
import com.lynx.tasm.LynxBackgroundRuntime
import com.lynx.tasm.LynxBackgroundRuntimeOptions
import com.lynx.tasm.LynxViewBuilder
import com.tiktok.sparkling.method.protocol.BridgeContext
import com.tiktok.sparkling.method.protocol.InnerBridge
import com.tiktok.sparkling.method.protocol.entity.BridgeCall
import com.tiktok.sparkling.method.protocol.entity.BridgeResult
import com.tiktok.sparkling.method.protocol.handler.BridgeFactoryManager
import com.tiktok.sparkling.method.protocol.handler.BridgeThreadDispatcher
import com.tiktok.sparkling.method.protocol.impl.errors.JSBErrorReportModel
import com.tiktok.sparkling.method.protocol.impl.interceptor.BridgeMockInterceptor
import com.tiktok.sparkling.method.protocol.impl.monitor.BridgeSDKMonitor
import com.tiktok.sparkling.method.protocol.impl.monitor.IBridgeMonitor
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeCallback
import com.tiktok.sparkling.method.protocol.interfaces.IBridgeLifeClient
import com.tiktok.sparkling.method.protocol.utils.LogUtils
import com.tiktok.sparkling.method.registry.core.BridgeContextWrapper
import com.tiktok.sparkling.method.registry.core.IBridgeContext
import com.tiktok.sparkling.method.registry.core.IDLBridgeMethod
import com.tiktok.sparkling.method.registry.core.IDLMethodRegistryCacheManager
import com.tiktok.sparkling.method.registry.core.JSEventDelegate
import com.tiktok.sparkling.method.registry.core.SparklingBridgeManager
import com.tiktok.sparkling.method.registry.core.SparklingBridgeManager.DEFAULT_NAMESPACE
import com.tiktok.sparkling.method.registry.core.BridgePlatformType
import com.tiktok.sparkling.method.registry.core.interfaces.IContainerIDProvider
import com.tiktok.sparkling.method.registry.core.interfaces.IReleasable
import com.tiktok.sparkling.method.registry.api.util.BridgeProtocolConstants
import com.tiktok.sparkling.method.registry.api.util.log
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class SparklingBridge : IReleasable {
    internal val innerBridge: InnerBridge = InnerBridge()
    private val closedSubscribers = mutableListOf<Pair<String, JSONObject?>>()
    private val TAG = "SparklingBridge"
    private val bridgeSdkContext = BridgeContextWrapper()

    companion object {
        var bridgeFactoryManager: BridgeFactoryManager? = null

        private var _bridgeThreadDispatcher: BridgeThreadDispatcher? = null
        var bridgeThreadDispatcher: BridgeThreadDispatcher
            get() = _bridgeThreadDispatcher ?: BridgeThreadDispatcher().also { _bridgeThreadDispatcher = it }
            set(value) {
                _bridgeThreadDispatcher = value
            }

        /**
         * Whether the host is a debug environment
         */
        var isDebugEnv = false

        /**
         * JSB SDK Error report block jsb list.
         * if you set jsb name here, it's jsb error message will not report.
         */
        val jsbErrorReportBlockList = ArrayList<String>()

        val cancelCallbackConfig: CancelCallbackConfig =
            CancelCallbackConfig().apply { enable = false }

        /**
         * switch enableToast to true will invoke a toast when jsb callback with an error code
         */
        private var enableToast = false

        fun enableToast(isEnable: Boolean) {
            enableToast = isEnable
        }

        fun getToastSetting(): Boolean = enableToast

        fun attachInitListener(listener: BridgeInitListener) {
            BridgeManager.attachInitListener(listener)
        }
    }

    fun getErrorReportModel(): JSBErrorReportModel = innerBridge.getBridgeContext().errorReportModel

    /**
     * prepareLynxJSRuntime
     * because of there is no lynxjsruntime yet, so we need a new way to bind lynxjsruntime
     */
    fun prepareLynxJSRuntime(
        containerId: String,
        options: LynxBackgroundRuntimeOptions,
        context: Context,
    ) {
        BridgeManager.insert(containerId, this)
        innerBridge.initLynxJSRuntime(containerId, options, context, this)
        bridgeSdkContext.registerWeakObject(
            BridgeContext::class.java,
            innerBridge.getBridgeContext(),
        )
        bridgeSdkContext.setContainerID(containerId)
//        bridgeSdkContext.setView(view)
        bridgeSdkContext.setBridge(this)
        bridgeSdkContext.setJSEventDelegate(
            object : JSEventDelegate {
                override fun sendJSEvent(
                    eventName: String,
                    params: JSONObject?,
                ) {
                    sendJSRuntimeEvent(eventName, params)
                }
            },
        )
        bridgeSdkContext.registerObject(
            IDLBridgeMethod.JSEventDelegate::class.java,
            object : IDLBridgeMethod.JSEventDelegate {
                override fun sendJSEvent(
                    eventName: String,
                    params: Map<String, Any?>?,
                ) {
                    sendJSRuntimeEvent(
                        eventName,
                        params?.let { Utils.mapToJSON(params) } ?: JSONObject(),
                    )
                }
            },
        )
        bridgeSdkContext.registerObject(
            IContainerIDProvider::class.java,
            object : IContainerIDProvider {
                override fun provideContainerID(): String? = containerId
            },
        )
        val handler = getBridgeContext().defaultCallHandler
        handler.setBridgeContext(bridgeSdkContext)
        innerBridge.registerHandler(handler)
    }

    /**
     * bind lynxjsruntime
     */
    fun bindLynxJSRuntime(lynxBackgroundRuntime: LynxBackgroundRuntime) {
        innerBridge.bindLynxJSRuntime(lynxBackgroundRuntime)
    }

    fun sendJSRuntimeEvent(
        eventName: String,
        jsonObject: JSONObject?,
    ) {
        innerBridge.sendJSRuntimeEvent(eventName, jsonObject)
    }

    /**
     * The first step of protocol layer initialization
     * @jsBridgeProtocols bridge protocol for control injection
     * @see BridgeProtocolConstants specific protocol constant reference
     */
    fun init(
        view: View,
        containerId: String?,
        jsBridgeProtocols: Int,
    ) {
        BridgeManager.insert(view, this)
        innerBridge.init(view, containerId, jsBridgeProtocols, this)
        bridgeSdkContext.registerWeakObject(
            BridgeContext::class.java,
            innerBridge.getBridgeContext(),
        )
        bridgeSdkContext.setContainerID(containerId)
        bridgeSdkContext.setView(view)
        bridgeSdkContext.setBridge(this)
        bridgeSdkContext.setJSEventDelegate(
            object : JSEventDelegate {
                override fun sendJSEvent(
                    eventName: String,
                    params: JSONObject?,
                ) {
                    sendEvent(eventName, params)
                }
            },
        )
        bridgeSdkContext.registerObject(
            IDLBridgeMethod.JSEventDelegate::class.java,
            object : IDLBridgeMethod.JSEventDelegate {
                override fun sendJSEvent(
                    eventName: String,
                    params: Map<String, Any?>?,
                ) {
                    sendEvent(eventName, params?.let { Utils.mapToJSON(params) } ?: JSONObject())
                }
            },
        )
        bridgeSdkContext.registerObject(
            IContainerIDProvider::class.java,
            object : IContainerIDProvider {
                override fun provideContainerID(): String? = containerId
            },
        )
        val handler = getBridgeContext().defaultCallHandler
        handler.setBridgeContext(bridgeSdkContext)
        innerBridge.registerHandler(handler)
    }

    fun bindWithBusinessNamespace(namespace: String) {
        getBridgeContext().businessCallHandler =
            BusinessCallHandler(namespace).apply {
                setBridgeContext(bridgeSdkContext)
            }
    }

    /**
     * only for who has call bindWithBusinessNamespace to register it's unique IDLMethod
     */
    fun registerBusinessIDLMethod(
        clazz: Class<out IDLBridgeMethod>,
        scope: BridgePlatformType = BridgePlatformType.ALL,
    ) {
        getBridgeContext().getNamespace()?.let {
            getBridgeContext().businessCallHandler?.registerMethod(clazz, scope)
        }
    }

    fun registerBusinessIDLMethod(
        name: String,
        scope: BridgePlatformType = BridgePlatformType.ALL,
        clazz: Class<out IDLBridgeMethod>? = null,
        factory: () -> IDLBridgeMethod,
    ) {
        getBridgeContext().getNamespace()?.let {
            getBridgeContext().businessCallHandler?.registerMethod(name, scope, clazz, factory)
        }
    }

    fun registerIDLMethod(
        clazz: Class<out IDLBridgeMethod>?,
        scope: BridgePlatformType = BridgePlatformType.ALL,
        namespace: String = DEFAULT_NAMESPACE,
    ) {
        SparklingBridgeManager.registerIDLMethod(clazz, scope, namespace)
    }

    fun registerIDLMethod(
        name: String,
        scope: BridgePlatformType = BridgePlatformType.ALL,
        namespace: String = DEFAULT_NAMESPACE,
        clazz: Class<out IDLBridgeMethod>? = null,
        factory: () -> IDLBridgeMethod,
    ) {
        SparklingBridgeManager.registerIDLMethod(name, scope, namespace, clazz, factory)
    }

    fun isBusinessIDLMethodExists(
        name: String,
        platformType: BridgePlatformType = BridgePlatformType.ALL,
    ): Boolean {
        if (getBridgeContext().getNamespace().isNullOrEmpty()) {
            return false
        }
        val businessHandler = getBridgeContext().businessCallHandler ?: return false
        return businessHandler.isMethodExists(name, platformType)
    }

    fun registerLocalIDLMethod(
        clazz: Class<out IDLBridgeMethod>?,
        scope: BridgePlatformType = BridgePlatformType.ALL,
    ) {
        innerBridge.getBridgeContext().defaultCallHandler.registerLocalIDLMethod(clazz, scope)
    }

    fun registerLocalIDLMethod(
        name: String,
        scope: BridgePlatformType = BridgePlatformType.ALL,
        clazz: Class<out IDLBridgeMethod>? = null,
        factory: () -> IDLBridgeMethod,
    ) {
        innerBridge.getBridgeContext().defaultCallHandler.registerLocalIDLMethod(name, scope, clazz, factory)
    }

    fun getBridgeSDKContext(): IBridgeContext = bridgeSdkContext

//    @Deprecated("don't use this method")
    fun getBridgeContext(): BridgeContext = innerBridge.getBridgeContext()

    fun registerMonitor(monitor: IBridgeMonitor) {
        innerBridge.registerMonitor(monitor)
    }

    fun registerJSBMockInterceptor(interceptor: BridgeMockInterceptor) {
        innerBridge.getBridgeContext().jsbMockInterceptor = interceptor
    }

    fun initSDKMonitor(
        context: Context,
        appInfo: BridgeSDKMonitor.APPInfo4Monitor,
    ) {
        InnerBridge.initSDKMonitor(context, appInfo)
    }

    fun registerLynxModule(
        builder: LynxViewBuilder,
        containerId: String?,
    ) {
        innerBridge.registerLynxModule(builder, containerId)
    }

    fun registerIBridgeLifeClient(bridgeLifeClient: IBridgeLifeClient) {
        innerBridge.registerIBridgeLifeClient(bridgeLifeClient)
    }

    fun sendEvent(
        event: String,
        data: JSONObject?,
    ) {
        innerBridge.sendEvent(event, data)
    }

    fun addClosedEventObserver(
        bridgeNames: List<String>,
        params: List<JSONObject?>,
    ) {
        bridgeNames.forEachIndexed { index, s ->
            this.closedSubscribers.add(Pair(bridgeNames[index], params[index]))
        }
    }

    override fun release() {
        dealWithCloseEvent()
        innerBridge.getBridgeContext().defaultCallHandler.onRelease()
        innerBridge.getBridgeContext().businessCallHandler?.onRelease()
        innerBridge.getBridgeContext().errorReportModel.putJsbExtension("release", "true")
        BridgeManager.remove(this)
        bridgeSdkContext.containerID?.let {
            IDLMethodRegistryCacheManager.unregisterIDLMethodRegistryCache(
                it,
            )
        }
    }

    private fun dealWithCloseEvent() {
        closedSubscribers.forEach { pair ->
            val curPlatform =
                when (getBridgeContext().platform) {
                    BridgePlatformType.LYNX -> {
                        BridgeCall.PlatForm.Lynx
                    }

                    BridgePlatformType.WEB -> {
                        BridgeCall.PlatForm.Web
                    }

                    else -> {
                        BridgeCall.PlatForm.Other
                    }
                }

            getBridgeContext()?.dispatcher?.onDispatchBridgeMethod(
                BridgeCall(getBridgeContext()).apply {
                    bridgeName = pair.first
                    params = pair.second
                    nameSpace = "DEFAULT"
                    platform = curPlatform
                    url = ""
                },
                object : IBridgeCallback {
                    override fun onBridgeResult(
                        result: BridgeResult,
                        call: BridgeCall?,
                        monitorBuilder: BridgeSDKMonitor.MonitorModel.Builder?,
                    ) {
                        LogUtils.d(
                            TAG,
                            "dealWithCloseEvent, bridgeName: ${pair.first}}, result: $result",
                        )
                    }
                },
                getBridgeContext()!!,
                null,
            )
        }
    }
}

internal object BridgeManager {
    private val map: ConcurrentHashMap<Int, SparklingBridge> = ConcurrentHashMap()

    //    private val containerIdMap : WeakValueConcurrentHashMap<String, SparklingBridge> = WeakValueConcurrentHashMap()
    private val listeners = CopyOnWriteArrayList<BridgeInitListener>()

    fun attachInitListener(listener: BridgeInitListener) = listeners.add(listener)

    fun insert(
        webView: View,
        sparklingBridge: SparklingBridge,
    ) {
        map[webView.hashCode()] = sparklingBridge
        map.values.log()
        listeners.forEach {
            it.onInit(webView, sparklingBridge)
        }
    }

    fun getSparklingBridge(view: View): SparklingBridge? = map[view.hashCode()]

    fun remove(bridge: SparklingBridge) {
        val find = map.toList().find { it.second == bridge }
        if (find != null) {
            map.remove(find.first)
        }
        map.values.log()
//        containerIdMap.remove(bridge.getBridgeSDKContext().containerID)
    }

    /**
     * create this function for lynx runtime JSB
     * wait hdt to support this feature
     * @param containerId
     * @return
     */
    fun insert(
        containerId: String,
        sparklingBridge: SparklingBridge,
    ) {
    }
}

interface BridgeInitListener {
    fun onInit(
        view: View,
        sparklingBridge: SparklingBridge,
    )

    fun onInit(
        containerID: String,
        sparklingBridge: SparklingBridge,
    ) {}
}
