// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling

import com.tiktok.sparkling.hybridkit.utils.ColorUtil
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SparklingThemeTest {
    @Before
    fun setUp() {
        ColorUtil.appContext = RuntimeEnvironment.getApplication()
        SparklingTheme.reset()
        SparklingTheme.preference = SparklingTheme.Preference.SYSTEM
    }

    @After
    fun tearDown() {
        SparklingTheme.preference = SparklingTheme.Preference.SYSTEM
        SparklingTheme.reset()
    }

    @Test
    fun followsTheDeviceByDefault() {
        assertEquals(SparklingTheme.Preference.SYSTEM, SparklingTheme.preference)
        // Nothing is forced, so a container decides for itself.
        assertNull(SparklingTheme.forcedStyle())
    }

    @Test
    fun forcesTheStyleItWasSetTo() {
        SparklingTheme.preference = SparklingTheme.Preference.DARK
        assertEquals("dark", SparklingTheme.forcedStyle())

        SparklingTheme.preference = SparklingTheme.Preference.LIGHT
        assertEquals("light", SparklingTheme.forcedStyle())
    }

    @Test
    fun survivesTheProcess() {
        SparklingTheme.preference = SparklingTheme.Preference.DARK
        // A fresh read, as if the app had been relaunched.
        SparklingTheme.reset()
        assertEquals(SparklingTheme.Preference.DARK, SparklingTheme.preference)
    }

    @Test
    fun doesNotPersistWhenTheHostOwnsStorage() {
        SparklingTheme.persist = false
        SparklingTheme.preference = SparklingTheme.Preference.DARK
        SparklingTheme.reset()

        assertEquals(SparklingTheme.Preference.SYSTEM, SparklingTheme.preference)
    }

    @Test
    fun parsesEveryAcceptedSpelling() {
        assertEquals(SparklingTheme.Preference.DARK, SparklingTheme.Preference.parse("dark"))
        assertEquals(SparklingTheme.Preference.DARK, SparklingTheme.Preference.parse("DARK"))
        assertEquals(SparklingTheme.Preference.LIGHT, SparklingTheme.Preference.parse("light"))
        assertEquals(SparklingTheme.Preference.SYSTEM, SparklingTheme.Preference.parse(null))
        assertEquals(SparklingTheme.Preference.SYSTEM, SparklingTheme.Preference.parse("nonsense"))
    }
}
