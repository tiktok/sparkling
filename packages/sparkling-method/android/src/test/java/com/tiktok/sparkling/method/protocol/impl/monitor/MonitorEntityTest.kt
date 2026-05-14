// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.protocol.impl.monitor

import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MonitorEntityTest {
    @Test
    fun testMonitorEntity() {
        val monitorEntity = MonitorEntity()
        monitorEntity.name = "testName"
        monitorEntity.code = 200
        monitorEntity.message = "testMessage"
        monitorEntity.url = "http://test.com"
        monitorEntity.beginTime = 1000L
        monitorEntity.endTime = 2000L
        monitorEntity.rawResult = JSONObject().put("key", "value")
        monitorEntity.rawRequest = JSONObject().put("key2", "value2")
        monitorEntity.hitBusinessHandler = true
        monitorEntity.nameSpace = "testNameSpace"
        monitorEntity.isRunInMainThread = false

        assert(monitorEntity.name == "testName")
        assert(monitorEntity.code == 200)
        assert(monitorEntity.message == "testMessage")
        assert(monitorEntity.url == "http://test.com")
        assert(monitorEntity.beginTime == 1000L)
        assert(monitorEntity.endTime == 2000L)
        assert(monitorEntity.rawResult?.getString("key") == "value")
        assert(monitorEntity.rawRequest?.getString("key2") == "value2")
        assert(monitorEntity.hitBusinessHandler)
        assert(monitorEntity.nameSpace == "testNameSpace")
        assert(!monitorEntity.isRunInMainThread)
    }
}
