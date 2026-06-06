package com.tiktok.sparkling.hybridkit.config

import android.app.Application
import com.lynx.tasm.LynxEnv
import com.tiktok.sparkling.hybridkit.lynx.SparklingLynxModuleWrapper
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Builder/companion-object coverage for [SparklingHybridConfig], [SparklingLynxConfig],
 * [BaseInfoConfig], [LogConfig] and [DebugConfig].
 */
class HybridConfigTest {
    @Test
    fun sparklingHybridConfigBuilderAppliesDefaults() {
        val baseInfo = BaseInfoConfig(isDebug = true)
        val cfg = SparklingHybridConfig.Builder(baseInfo).build()

        assertSame(baseInfo, cfg.baseInfoConfig)
        assertNull(cfg.lynxConfig)
        assertNull(cfg.webConfig)
        assertNull(cfg.bridgeConfig)
        assertNull(cfg.logConfig)
        assertNull(cfg.debugConfig)
    }

    @Test
    fun sparklingHybridConfigBuilderPropagatesAllSetters() {
        val baseInfo = BaseInfoConfig(isDebug = false)
        val lynxCfg = object : ILynxConfig {}
        val webCfg = object : IWebConfig {}
        val bridgeCfg = mockk<IBridgeConfig>(relaxed = true)
        val logCfg = LogConfig(mockk(relaxed = true))
        val debugCfg = DebugConfig(showErrorWhenJSBFailed = true)

        val cfg =
            SparklingHybridConfig.build(baseInfo) {
                setLynxConfig(lynxCfg)
                setWebConfig(webCfg)
                setBridgeConfig(bridgeCfg)
                setLogConfig(logCfg)
                setDebugConfig(debugCfg)
            }

        assertSame(baseInfo, cfg.baseInfoConfig)
        assertSame(lynxCfg, cfg.lynxConfig)
        assertSame(webCfg, cfg.webConfig)
        assertSame(bridgeCfg, cfg.bridgeConfig)
        assertSame(logCfg, cfg.logConfig)
        assertSame(debugCfg, cfg.debugConfig)
    }

    @Test
    fun baseInfoConfigDefaultMethodsReturnExpectedFallbacks() {
        val info = BaseInfoConfig(isDebug = true)
        assertTrue(info.isDebug)
        assertEquals("hello", info.applyGlobalLoadUrl("hello"))
        assertEquals("hello", info.applyAppendCommonParamsUrl("hello"))
        assertFalse(info.applyCommonShouldOverrideUrl(null, "scheme://"))
        assertNull(info.getStableProps(mockk(relaxed = true)))
        assertNull(info.getUnstableProps(mockk(relaxed = true), mockk(relaxed = true)))
        // applyGlobalSettings is a no-op; just exercise it
        info.applyGlobalSettings(mockk(relaxed = true), mockk(relaxed = true))
    }

    @Test
    fun debugAndLogConfigPropertiesAreReadable() {
        val logger =
            object : com.tiktok.sparkling.hybridkit.utils.HybridLogger {
                override fun onLog(
                    msg: String,
                    logLevel: com.tiktok.sparkling.hybridkit.utils.LogLevel,
                    tag: String,
                ) = Unit

                override fun onReject(
                    e: Throwable,
                    extraMsg: String,
                    tag: String,
                ) = Unit
            }
        val log = LogConfig(logger)
        assertSame(logger, log.logger)

        val debug = DebugConfig()
        assertFalse(debug.showErrorWhenJSBFailed)
        debug.showErrorWhenJSBFailed = true
        assertTrue(debug.showErrorWhenJSBFailed)
    }

    @Test
    fun sparklingLynxConfigBuilderAppliesDefaultsAndCustomValues() {
        val app = mockk<Application>(relaxed = true)

        val emptyBuilder = SparklingLynxConfig.Builder(app)
        assertSame(app, emptyBuilder.context)
        val empty = emptyBuilder.build()
        assertTrue(empty.isCheckPropsSetter)
        assertNull(empty.libraryLoader)
        assertNull(empty.templateProvider)
        assertTrue(empty.globalBehaviors.isEmpty())
        assertTrue(empty.globalModules.isEmpty())
        assertNotNull(empty.additionInit)

        val moduleWrapper = mockk<SparklingLynxModuleWrapper>(relaxed = true)
        var initCalled = false
        val cfg =
            SparklingLynxConfig.build(app) {
                setCheckPropsSetter(false)
                setLibraryLoader(null)
                setTemplateProvider(null)
                addLynxModules(mapOf("foo" to moduleWrapper))
                setAdditionInit { initCalled = true }
            }

        assertFalse(cfg.isCheckPropsSetter)
        assertEquals(1, cfg.globalModules.size)
        assertSame(moduleWrapper, cfg.globalModules["foo"])
        // exercise additionInit lambda
        cfg.additionInit.invoke(mockk<LynxEnv>(relaxed = true))
        assertTrue(initCalled)
    }
}
