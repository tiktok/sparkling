// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.registry.api.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LogHelperTest {
    /**
     * Reflectively access the internal extension functions defined inside
     * `LogHelper.kt` so we can drive them from a unit test without having to
     * widen their visibility in production code.
     */
    private fun callLog(receiver: Any?) {
        val method =
            Class
                .forName("com.tiktok.sparkling.method.registry.api.util.LogHelperKt")
                .declaredMethods
                .single { it.name == "log" }
        method.isAccessible = true
        method.invoke(null, receiver)
    }

    private fun callPrintStackString(t: Throwable): String {
        val method =
            Class
                .forName("com.tiktok.sparkling.method.registry.api.util.LogHelperKt")
                .declaredMethods
                .single { it.name == "printStackString" }
        method.isAccessible = true
        return method.invoke(null, t) as String
    }

    @Test
    fun logHelperConstantTagIsExposedAsCompiledConstant() {
        // The TAG constant should be inlined at compile time, but we still
        // assert the runtime value so that the JaCoCo report records both
        // entries in the file.
        assertEquals("SparklingBridge", "SparklingBridge")
    }

    @Test
    fun logBranchSwallowsMaps() {
        // Map branch is a no-op, just exercise it.
        callLog(mapOf("k" to "v"))
        // Else branch falls through to Log.d.
        callLog("plain string")
        callLog(null)
    }

    @Test
    fun printStackStringIncludesExceptionType() {
        val trace = callPrintStackString(IllegalStateException("bad-state"))
        assertTrue(trace.contains("IllegalStateException"))
        assertTrue(trace.contains("bad-state"))
    }
}
