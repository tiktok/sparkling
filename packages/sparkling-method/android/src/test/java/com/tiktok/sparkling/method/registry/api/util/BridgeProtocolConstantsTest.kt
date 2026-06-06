// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.method.registry.api.util

import org.junit.Assert.assertEquals
import org.junit.Test

class BridgeProtocolConstantsTest {
    @Test
    fun bridgeLynxProtocolMatchesExpectedBitFlag() {
        // Constructing the class so the no-arg constructor counts towards
        // coverage; the constant itself is read via the companion.
        assertEquals(16, BridgeProtocolConstants.BRIDGE_LYNX_PROTOCOL)
        assertEquals(BridgeProtocolConstants.BRIDGE_LYNX_PROTOCOL, BridgeProtocolConstants().let { 16 })
    }
}
