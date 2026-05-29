// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import android.app.Application
import android.content.Intent
import com.tiktok.sparkling.Sparkling.Companion.SPARKLING_CONTEXT_CONTAINER_ID
import com.tiktok.sparkling.hybridkit.base.HybridKitType
import com.tiktok.sparkling.hybridkit.utils.ColorUtil
import com.tiktok.sparkling.utils.SchemeParser
import io.mockk.clearAllMocks
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    packageName = "com.tiktok.sparkling",
)
class SparklingTest {
    private lateinit var application: Application
    private lateinit var sparklingContext: SparklingContext

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        ColorUtil.appContext = application
        sparklingContext =
            SparklingContext().apply {
                containerId = "test_container_id"
                scheme = "hybrid://lynxview_page?bundle=test_bundle&title=Hello"
            }
        SparklingContextTransferStation.clearAllContexts()
        clearAllMocks()
    }

    @After
    fun tearDown() {
        SparklingContextTransferStation.clearAllContexts()
        unmockkAll()
    }

    @Test
    fun buildReturnsSparklingInstance() {
        val sparkling = Sparkling.build(application, sparklingContext)
        assertNotNull(sparkling)
    }

    @Test
    fun navigateProcessesContextAndStartsActivity() {
        val sparkling = Sparkling.build(application, sparklingContext)

        sparkling.navigate()

        val startedIntent: Intent? = shadowOf(application).nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(SparklingActivity::class.java.name, startedIntent?.component?.className)
        assertEquals(
            sparklingContext.containerId,
            startedIntent?.getStringExtra(SPARKLING_CONTEXT_CONTAINER_ID),
        )
        assertEquals(
            sparklingContext,
            SparklingContextTransferStation.getSparklingContext(sparklingContext.containerId),
        )
        assertEquals(HybridKitType.LYNX, sparklingContext.hybridSchemeParam?.engineType)
    }

    @Test
    fun processSparklingContextParsesScheme() {
        val sparkling = Sparkling.build(application, sparklingContext)

        val expected = SchemeParser.parseScheme(sparklingContext.scheme!!)
        sparkling.processSparklingContext(sparklingContext)

        assertNotNull(sparklingContext.hybridSchemeParam)
        assertEquals(expected?.engineType, sparklingContext.hybridSchemeParam?.engineType)
        assertEquals(expected?.bundle, sparklingContext.hybridSchemeParam?.bundle)
        assertEquals(expected?.title, sparklingContext.hybridSchemeParam?.title)
    }

    @Test
    fun processSparklingContextHandlesNullScheme() {
        val sparkling = Sparkling.build(application, sparklingContext)
        sparklingContext.scheme = null

        sparkling.processSparklingContext(sparklingContext)

        assertNull(sparklingContext.hybridSchemeParam)
    }

    @Test
    fun processSparklingContextHandlesEmptyScheme() {
        val sparkling = Sparkling.build(application, sparklingContext)
        sparklingContext.scheme = ""

        sparkling.processSparklingContext(sparklingContext)

        assertNull(sparklingContext.hybridSchemeParam)
    }

    @Test
    fun createViewFactoryMethodsExist() {
        val sparkling = Sparkling.build(application, sparklingContext)

        assertNotNull(sparkling.createView(withoutPrepare = true))
        assertNotNull(sparkling.createView())
    }

    @Test
    fun constantsExposeContainerMetadata() {
        assertEquals("SparklingContextContainerId", Sparkling.SPARKLING_CONTEXT_CONTAINER_ID)
        assertEquals(1, Sparkling.TYPE_PAGE)
        assertEquals(2, Sparkling.TYPE_POPUP)
        assertEquals(3, Sparkling.TYPE_CARD)
    }
}
