// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.service

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.tiktok.sparkling.hybridkit.service.HybridActivityStackManager
import com.tiktok.sparkling.hybridkit.utils.HybridActivityDelegate
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Field

@RunWith(RobolectricTestRunner::class)
class HybridActivityStackManagerTest {
    private lateinit var mockApplication: Application
    private lateinit var mockActivity: Activity
    private lateinit var activityLifecycleCallbacks: Application.ActivityLifecycleCallbacks

    @Before
    fun setUp() {
        clearAllMocks()

        mockApplication = mockk(relaxed = true)
        mockActivity = mockk(relaxed = true)

        mockkObject(HybridActivityDelegate)
        every { HybridActivityDelegate.getTopActivity() } returns null
        every { HybridActivityDelegate.setTopActivity(any()) } just Runs

        every { mockApplication.registerActivityLifecycleCallbacks(any()) } answers {
            activityLifecycleCallbacks = firstArg()
        }

        // Reset resumeCount to 0 using reflection to ensure clean state
        resetResumeCount()

        // Initialize HybridActivityStackManager to ensure activityLifecycleCallbacks is captured
        HybridActivityStackManager.init(mockApplication)
    }

    private fun resetResumeCount() {
        try {
            val resumeCountField: Field = HybridActivityStackManager::class.java.getDeclaredField("resumeCount")
            resumeCountField.isAccessible = true
            resumeCountField.setInt(HybridActivityStackManager, 0)
        } catch (e: Exception) {
            // If reflection fails, the tests might still work depending on the state
        }
    }

    @Test
    fun testInit() {
        HybridActivityStackManager.init(mockApplication)

        verify { mockApplication.registerActivityLifecycleCallbacks(any()) }
    }

    @Test
    fun testGetTopActivity() {
        val expectedActivity = mockk<Activity>()
        every { HybridActivityDelegate.getTopActivity() } returns expectedActivity

        val result = HybridActivityStackManager.getTopActivity()

        assertEquals(expectedActivity, result)
        verify { HybridActivityDelegate.getTopActivity() }
    }

    @Test
    fun testGetTopActivityWhenNull() {
        every { HybridActivityDelegate.getTopActivity() } returns null

        val result = HybridActivityStackManager.getTopActivity()

        assertNull(result)
    }

    @Test
    fun testIsBackgroundInitially() {
        assertTrue(HybridActivityStackManager.isBackground())
    }

    @Test
    fun testIsBackgroundAfterActivityResumed() {
        activityLifecycleCallbacks.onActivityResumed(mockActivity)

        assertFalse(HybridActivityStackManager.isBackground())
        verify { HybridActivityDelegate.setTopActivity(mockActivity) }
    }

    @Test
    fun testIsBackgroundAfterActivityPaused() {
        activityLifecycleCallbacks.onActivityResumed(mockActivity)
        assertFalse(HybridActivityStackManager.isBackground())

        activityLifecycleCallbacks.onActivityPaused(mockActivity)
        assertTrue(HybridActivityStackManager.isBackground())
    }

    @Test
    fun testMultipleActivitiesResumeAndPause() {
        val mockActivity2 = mockk<Activity>(relaxed = true)

        activityLifecycleCallbacks.onActivityResumed(mockActivity)
        assertFalse(HybridActivityStackManager.isBackground())

        activityLifecycleCallbacks.onActivityResumed(mockActivity2)
        assertFalse(HybridActivityStackManager.isBackground())

        activityLifecycleCallbacks.onActivityPaused(mockActivity)
        assertFalse(HybridActivityStackManager.isBackground())

        activityLifecycleCallbacks.onActivityPaused(mockActivity2)
        assertTrue(HybridActivityStackManager.isBackground())
    }

    @Test
    fun testActivityLifecycleCallbacksOnActivityCreated() {
        val mockBundle = mockk<Bundle>(relaxed = true)

        activityLifecycleCallbacks.onActivityCreated(mockActivity, mockBundle)

        assertTrue(HybridActivityStackManager.isBackground())
    }

    @Test
    fun testActivityLifecycleCallbacksOnActivityStarted() {
        activityLifecycleCallbacks.onActivityStarted(mockActivity)

        assertTrue(HybridActivityStackManager.isBackground())
    }

    @Test
    fun testActivityLifecycleCallbacksOnActivityStopped() {
        activityLifecycleCallbacks.onActivityStopped(mockActivity)

        assertTrue(HybridActivityStackManager.isBackground())
    }

    @Test
    fun testActivityLifecycleCallbacksOnActivitySaveInstanceState() {
        val mockBundle = mockk<Bundle>(relaxed = true)

        activityLifecycleCallbacks.onActivitySaveInstanceState(mockActivity, mockBundle)

        assertTrue(HybridActivityStackManager.isBackground())
    }

    @Test
    fun testActivityLifecycleCallbacksOnActivityDestroyed() {
        activityLifecycleCallbacks.onActivityDestroyed(mockActivity)

        assertTrue(HybridActivityStackManager.isBackground())
    }

    @Test
    fun testComplexActivityLifecycleScenario() {
        val mockActivity2 = mockk<Activity>(relaxed = true)
        val mockActivity3 = mockk<Activity>(relaxed = true)

        assertTrue(HybridActivityStackManager.isBackground())

        activityLifecycleCallbacks.onActivityResumed(mockActivity)
        assertFalse(HybridActivityStackManager.isBackground())
        verify { HybridActivityDelegate.setTopActivity(mockActivity) }

        activityLifecycleCallbacks.onActivityResumed(mockActivity2)
        assertFalse(HybridActivityStackManager.isBackground())
        verify { HybridActivityDelegate.setTopActivity(mockActivity2) }

        activityLifecycleCallbacks.onActivityResumed(mockActivity3)
        assertFalse(HybridActivityStackManager.isBackground())
        verify { HybridActivityDelegate.setTopActivity(mockActivity3) }

        activityLifecycleCallbacks.onActivityPaused(mockActivity2)
        assertFalse(HybridActivityStackManager.isBackground())

        activityLifecycleCallbacks.onActivityPaused(mockActivity)
        assertFalse(HybridActivityStackManager.isBackground())

        activityLifecycleCallbacks.onActivityPaused(mockActivity3)
        assertTrue(HybridActivityStackManager.isBackground())
    }
}
