// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SparklingContextTransferStationTest {
    private lateinit var sparklingContext: SparklingContext

    @Before
    fun setUp() {
        SparklingContextTransferStation.clearAllContexts()
        sparklingContext =
            SparklingContext().apply {
                containerId = "transfer-station-id"
            }
    }

    @After
    fun tearDown() {
        SparklingContextTransferStation.clearAllContexts()
    }

    @Test
    fun saveAndRetrieveContextById() {
        SparklingContextTransferStation.saveSparklingContext(sparklingContext)

        val retrieved = SparklingContextTransferStation.getSparklingContext("transfer-station-id")

        assertEquals(sparklingContext, retrieved)
    }

    @Test
    fun releaseContextRemovesEntry() {
        SparklingContextTransferStation.saveSparklingContext(sparklingContext)

        SparklingContextTransferStation.releaseSparklingContext("transfer-station-id")

        val retrieved = SparklingContextTransferStation.getSparklingContext("transfer-station-id")
        assertNull(retrieved)
    }

    @Test
    fun releaseWithNullDoesNotCrash() {
        SparklingContextTransferStation.saveSparklingContext(sparklingContext)

        SparklingContextTransferStation.releaseSparklingContext(null)

        val retrieved = SparklingContextTransferStation.getSparklingContext("transfer-station-id")
        assertEquals(sparklingContext, retrieved)
    }
}
