// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit

import android.app.Application
import android.util.Log
import com.tiktok.sparkling.hybridkit.config.SparklingHybridConfig
import com.tiktok.sparkling.hybridkit.utils.ColorUtil
import com.tiktok.sparkling.hybridkit.utils.LogLevel
import com.tiktok.sparkling.hybridkit.utils.LogUtils
import java.util.concurrent.atomic.AtomicBoolean

object HybridCommon {
    private const val TAG = "HybridCommon"
    var hybridConfig: SparklingHybridConfig? = null
        private set
    private var prepareBlockCalled = AtomicBoolean(false)

    @Volatile
    private var prepareBlock: (() -> Unit)? = null
    private var postBlockCalled = AtomicBoolean(false)
    private var postBlock: (() -> Unit)? = null

    fun init(application: Application) {
        ColorUtil.appContext = application
        HybridEnvironment.instance.context = application
    }

    /**
     * must be called before initLynxKit
     * @param hybridConfig cannot be null
     */
    fun setHybridConfig(
        hybridConfig: SparklingHybridConfig,
        application: Application,
    ) {
        this.hybridConfig = hybridConfig
        HybridEnvironment.instance.apply {
            baseInfoConfig = hybridConfig.baseInfoConfig
            lynxConfig = hybridConfig.lynxConfig
            webConfig = hybridConfig.webConfig
            context = application
            debugConfig = hybridConfig.debugConfig
//            isDebug = hybridConfig.baseInfoConfig.isDebug
        }
    }

    private fun reset() {
        hybridConfig = null
        prepareBlock = null
        prepareBlockCalled.set(false)
        postBlock = null
        postBlockCalled.set(false)
    }

    @Synchronized
    fun initCommon() {
        // init jsb
//        hybridConfig?.bridgeConfig?.let {
//            HybridService.instance().bind(
//                IBridgeService::class.java,
//                HybridBridgeService(it)
//            )
//        }

        hybridConfig?.logConfig?.let {
            LogUtils.logger = it.logger
        }
    }

    /**
     * this prepareBlock will be first statement called in initLynxKit/initWebKit.
     * you must call setHybridConfig in prepareBlock,
     * and you don't need to call setHybridConfig in other way.
     * @param prepareBlock must call setHybridConfig
     */
    fun setPrepareBlock(prepareBlock: () -> Unit) {
        this.prepareBlock = prepareBlock
    }

    fun hasPrepareBlock(): Boolean = prepareBlock != null

    fun setPostBlock(postBlock: () -> Unit) {
        this.postBlock = postBlock
    }

    @Synchronized
    fun tryCallPostBlock(): Boolean {
        if (!postBlockCalled.compareAndSet(false, true)) {
            return true
        }
        return try {
            postBlock?.let { it() }
            true
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Call PostBlock failed, please check your code.",
            )
            reset()
            false
        }
    }

    @Synchronized
    @JvmOverloads
    fun tryCallPrepareBlock(hybridContext: HybridContext? = null): Boolean {
        if (!prepareBlockCalled.compareAndSet(false, true)) {
            return true
        }
        return try {
            prepareBlock?.let { it() }
            true
        } catch (e: Exception) {
            LogUtils.printLog(
                "Call PrepareBlock failed, please check your code.",
                LogLevel.E,
                TAG,
            )
            reset()
            false
        }
    }

    fun updateGlobalProps(
        key: String,
        value: Any,
    ) {
//        HybridEnvironment.instance.baseInfoConfig?.put(key, value)
    }
}
