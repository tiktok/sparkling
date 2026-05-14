// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method

import android.app.Application

/**
 * Test application for Robolectric tests
 */
class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize any test-specific configurations here
    }
}
