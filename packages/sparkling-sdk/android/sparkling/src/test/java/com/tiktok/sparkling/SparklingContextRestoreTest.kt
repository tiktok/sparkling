// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import android.content.Intent
import com.tiktok.sparkling.hybridkit.utils.ColorUtil
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * The blank page a hot start used to land on.
 *
 * When Android reclaims a backgrounded app, the task survives and the process
 * does not. The activity is recreated from the same Intent in a new process,
 * where the in-memory context map is empty - so the container had nothing to
 * render and showed an empty page titled "Sparkling Page".
 */
@RunWith(RobolectricTestRunner::class)
class SparklingContextRestoreTest {
    private val scheme = "hybrid://lynxview_page?bundle=main.lynx.bundle&hide_nav_bar=1"

    @Before
    fun setUp() {
        ColorUtil.appContext = RuntimeEnvironment.getApplication()
        SparklingContextTransferStation.clearAllContexts()
        SparklingContextTransferStation.onRestore = null
    }

    @After
    fun tearDown() {
        SparklingContextTransferStation.clearAllContexts()
        SparklingContextTransferStation.onRestore = null
    }

    private fun intent(
        containerId: String? = "container-1",
        scheme: String? = this.scheme,
        initData: String? = null,
    ): Intent =
        Intent().apply {
            containerId?.let { putExtra(Sparkling.SPARKLING_CONTEXT_CONTAINER_ID, it) }
            scheme?.let { putExtra(Sparkling.SPARKLING_CONTEXT_SCHEME, it) }
            initData?.let { putExtra(Sparkling.SPARKLING_CONTEXT_INIT_DATA, it) }
        }

    @Test
    fun returnsTheLiveContextWhenThereIsOne() {
        val live = SparklingContext().apply { containerId = "container-1" }
        SparklingContextTransferStation.saveSparklingContext(live)

        // The same object, not a rebuilt copy: the host's delegates hang off it.
        assertSame(live, SparklingContextTransferStation.restore(intent()))
    }

    @Test
    fun rebuildsFromTheIntentWhenTheProcessHasRestarted() {
        // An empty map is exactly what a new process starts with.
        val restored = SparklingContextTransferStation.restore(intent())

        assertNotNull(restored)
        assertEquals("container-1", restored?.containerId)
        assertEquals(scheme, restored?.scheme)
        // Parsed, so the page is laid out the way it was before rather than
        // falling back to the container's defaults.
        assertEquals("main.lynx.bundle", restored?.hybridSchemeParam?.bundle)
        assertEquals(true, restored?.hybridSchemeParam?.hideNavBar)
    }

    @Test
    fun keepsTheRebuiltContextSoTheFragmentFindsTheSameOne() {
        val first = SparklingContextTransferStation.restore(intent())
        val second = SparklingContextTransferStation.restore(intent())

        assertSame(first, second)
    }

    @Test
    fun carriesInitialDataAcross() {
        val restored = SparklingContextTransferStation.restore(intent(initData = "{\"a\":1}"))
        assertEquals("{\"a\":1}", restored?.initData())
    }

    @Test
    fun letsTheHostPutBackWhatOnlyItCanProvide() {
        var seen: SparklingContext? = null
        SparklingContextTransferStation.onRestore = { seen = it }

        val restored = SparklingContextTransferStation.restore(intent())

        assertSame(restored, seen)
    }

    @Test
    fun survivesAHostCallbackThatThrows() {
        SparklingContextTransferStation.onRestore = { error("host is broken") }
        assertNotNull(SparklingContextTransferStation.restore(intent()))
    }

    @Test
    fun answersNullWhenThereIsGenuinelyNothingToShow() {
        // An Intent from a build before the scheme travelled in it.
        assertNull(SparklingContextTransferStation.restore(intent(scheme = null)))
        assertNull(SparklingContextTransferStation.restore(intent(containerId = null)))
        assertNull(SparklingContextTransferStation.restore(null))
    }

    @Test
    fun answersNullForASchemeThisSdkCannotRender() {
        assertNull(SparklingContextTransferStation.restore(intent(scheme = "hybrid://something-else?bundle=b")))
    }
}
