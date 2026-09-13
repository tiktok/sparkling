// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SparklingBackPressTest {
    @After
    fun tearDown() {
        SparklingBackPress.reset()
    }

    @Test
    fun interceptsNothingByDefault() {
        assertFalse(SparklingBackPress.isIntercepting("container-1"))
    }

    @Test
    fun remembersWhichContainerIsIntercepting() {
        SparklingBackPress.setIntercepting("container-1", true)

        assertTrue(SparklingBackPress.isIntercepting("container-1"))
        // Interception belongs to one page, not to the app.
        assertFalse(SparklingBackPress.isIntercepting("container-2"))
    }

    @Test
    fun stopsInterceptingWhenAsked() {
        SparklingBackPress.setIntercepting("container-1", true)
        SparklingBackPress.setIntercepting("container-1", false)

        assertFalse(SparklingBackPress.isIntercepting("container-1"))
    }

    @Test
    fun ignoresAnIdThatNamesNothing() {
        SparklingBackPress.setIntercepting(null, true)
        SparklingBackPress.setIntercepting("", true)
        SparklingBackPress.setIntercepting("   ", true)

        assertFalse(SparklingBackPress.isIntercepting(null))
        assertFalse(SparklingBackPress.isIntercepting(""))
        assertFalse(SparklingBackPress.isIntercepting("   "))
    }

    @Test
    fun forgetsAContainerThatIsGone() {
        SparklingBackPress.setIntercepting("container-1", true)
        SparklingBackPress.forget("container-1")

        // A later container given the same id must not inherit an interception
        // nobody asked for.
        assertFalse(SparklingBackPress.isIntercepting("container-1"))
    }

    @Test
    fun defaultsToAskingForASecondPressOnTheTaskRoot() {
        assertEquals(SparklingBackPress.RootBehavior.CONFIRM_THEN_EXIT, SparklingBackPress.rootBehavior)
        assertEquals(2000L, SparklingBackPress.confirmWindowMillis)
    }

    @Test
    fun rootBehaviourIsConfigurable() {
        SparklingBackPress.rootBehavior = SparklingBackPress.RootBehavior.EXIT
        assertEquals(SparklingBackPress.RootBehavior.EXIT, SparklingBackPress.rootBehavior)
    }
}
