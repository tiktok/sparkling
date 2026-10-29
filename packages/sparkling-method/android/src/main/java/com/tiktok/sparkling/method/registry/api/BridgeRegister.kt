// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.api

/**
 * Host hook for registering IDL methods with the minimum runtime.
 *
 * The host overrides [tryToRegister] and binds the instance on the bridge companion.
 */
open class BridgeRegister {
    /** Registers IDL methods. Empty until the host provides an implementation. */
    open fun tryToRegister() {}
}
