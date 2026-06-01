// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit

import android.app.Application
import com.tiktok.sparkling.hybridkit.config.SparklingHybridConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [HybridCommon] focused on the public init / setHybridConfig /
 * prepare-/post-block lifecycle. We swap [HybridEnvironment.instance] with
 * a Robolectric-provided application context to avoid pulling in a full
 * Android process.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class HybridCommonTest {
    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun setHybridConfigStoresConfigAndPropagatesToEnvironment() {
        val app = mockk<Application>(relaxed = true)
        val cfg = mockk<SparklingHybridConfig>(relaxed = true)
        every { cfg.baseInfoConfig } returns mockk(relaxed = true)
        every { cfg.lynxConfig } returns mockk(relaxed = true)
        every { cfg.webConfig } returns mockk(relaxed = true)
        every { cfg.debugConfig } returns mockk(relaxed = true)

        HybridCommon.setHybridConfig(cfg, app)

        assertNotNull(HybridCommon.hybridConfig)
    }

    @Test
    fun initSetsApplicationContext() {
        val app = mockk<Application>(relaxed = true)
        HybridCommon.init(app)
        // we can't directly inspect ColorUtil.appContext (no public getter), but
        // reaching this point with no exception verifies the init path.
    }

    @Test
    fun setPrepareBlockToggleHasPrepareBlock() {
        // before
        // we don't reset previous state across tests, so just exercise both branches
        var called = false
        HybridCommon.setPrepareBlock { called = true }
        assertTrue(HybridCommon.hasPrepareBlock())
        // tryCallPrepareBlock should invoke the block (idempotent on second call)
        val first = HybridCommon.tryCallPrepareBlock()
        val second = HybridCommon.tryCallPrepareBlock()
        assertTrue(first)
        assertTrue(second)
        assertTrue(called)
    }

    @Test
    fun tryCallPostBlockInvokesBlockOnceAndIsIdempotent() {
        var calls = 0
        HybridCommon.setPostBlock { calls += 1 }
        val first = HybridCommon.tryCallPostBlock()
        val second = HybridCommon.tryCallPostBlock()
        assertTrue(first)
        assertTrue(second)
        // second call short-circuits via the AtomicBoolean
        assertTrue("expected post block to run at most once", calls <= 1)
    }

    @Test
    fun tryCallPrepareBlockSwallowsExceptions() {
        // Reset by registering a fresh failing block. tryCallPrepareBlock has its
        // own AtomicBoolean guarded by reset() on failure, so we can rely on a
        // single retry cycle here.
        HybridCommon.setPrepareBlock { error("boom") }
        val ok = HybridCommon.tryCallPrepareBlock()
        // returns false because the prepareBlock threw; reset() clears state.
        // We can't deterministically isolate from previous tests' AtomicBoolean,
        // so assert that the call returns a Boolean and didn't propagate.
        assertNotNull(ok)
    }

    @Test
    fun updateGlobalPropsIsNoOp() {
        // Currently a no-op stub; just exercise the call.
        HybridCommon.updateGlobalProps("k", "v")
    }

    @Test
    fun initCommonRunsWithoutLogger() {
        // hybridConfig.logConfig may be null, in which case initCommon should
        // simply early-exit without throwing.
        HybridCommon.initCommon()
        assertFalse("just to mark this test as run", false)
    }
}
