// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.hybridkit.utils

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Verifies [LogUtils] forwards messages to a registered [HybridLogger] for all
 * [LogLevel]s, falls back to android.util.Log when no logger is set, and
 * routes [LogUtils.printReject] correctly.
 */
@RunWith(RobolectricTestRunner::class)
class LogUtilsTest {
    @After
    fun reset() {
        LogUtils.logger = null
    }

    @Test
    fun customLoggerReceivesAllLogLevels() {
        val captured = mutableListOf<Triple<String, LogLevel, String>>()
        val latch = CountDownLatch(LogLevel.values().size)

        LogUtils.logger =
            object : HybridLogger {
                override fun onLog(
                    msg: String,
                    logLevel: LogLevel,
                    tag: String,
                ) {
                    synchronized(captured) { captured.add(Triple(msg, logLevel, tag)) }
                    latch.countDown()
                }

                override fun onReject(
                    e: Throwable,
                    extraMsg: String,
                    tag: String,
                ) {
                    // not exercised here
                }
            }

        for (level in LogLevel.values()) {
            LogUtils.printLog("msg-$level", level, "tagX")
        }

        assertEquals(true, latch.await(3, TimeUnit.SECONDS))
        assertEquals(LogLevel.values().size, captured.size)
        // Every level should be represented exactly once.
        val seen = captured.map { it.second }.toSet()
        assertEquals(LogLevel.values().toSet(), seen)
        // Tag is prefixed with `HybridKit_`.
        captured.forEach { assertEquals("HybridKit_tagX", it.third) }
    }

    @Test
    fun nullLoggerFallsBackToAndroidLogWithoutThrowing() {
        // logger is null by reset; just ensure no exception bubbles up.
        for (level in LogLevel.values()) {
            LogUtils.printLog("default-msg-$level", level, "tag")
        }
        // Drain async sequence executor so any pending Log.* calls complete.
        AsyncUtils
            .getExecutor(TaskType.Sequence)
            .submit { /* no-op */ }
            .get(3, TimeUnit.SECONDS)
    }

    @Test
    fun printRejectForwardsToCustomLogger() {
        val latch = CountDownLatch(1)
        val captured = arrayOfNulls<Throwable>(1)
        val capturedTag = arrayOfNulls<String>(1)
        val capturedExtra = arrayOfNulls<String>(1)

        LogUtils.logger =
            object : HybridLogger {
                override fun onLog(
                    msg: String,
                    logLevel: LogLevel,
                    tag: String,
                ) = Unit

                override fun onReject(
                    e: Throwable,
                    extraMsg: String,
                    tag: String,
                ) {
                    captured[0] = e
                    capturedTag[0] = tag
                    capturedExtra[0] = extraMsg
                    latch.countDown()
                }
            }

        val err = IllegalStateException("boom")
        LogUtils.printReject(err, extraMsg = "ctx", tag = "tagY")

        assertEquals(true, latch.await(3, TimeUnit.SECONDS))
        assertEquals(err, captured[0])
        assertEquals("ctx", capturedExtra[0])
        assertEquals("HybridKit_tagY", capturedTag[0])
    }

    @Test
    fun printRejectWithNullLoggerDoesNotThrow() {
        LogUtils.logger = null
        LogUtils.printReject(IllegalArgumentException("nope"))
        AsyncUtils
            .getExecutor(TaskType.Sequence)
            .submit { /* drain */ }
            .get(3, TimeUnit.SECONDS)
    }

    @Test
    fun logLevelEnumExposesAllValues() {
        // Trivial coverage for the enum to exercise generated values()/valueOf().
        assertNotNull(LogLevel.valueOf("D"))
        assertNotNull(LogLevel.valueOf("V"))
        assertNotNull(LogLevel.valueOf("I"))
        assertNotNull(LogLevel.valueOf("W"))
        assertNotNull(LogLevel.valueOf("E"))
        assertEquals(5, LogLevel.values().size)
    }
}
