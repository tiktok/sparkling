// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

/**
 * Supplies a bridge method instance on demand, so a registration can carry how to build the method
 * rather than only which class it is.
 *
 * Declared as a `fun interface` so that a registration can pass a lambda.
 */
fun interface IDLMethodProvider {
    fun provideMethod(): IDLBridgeMethod
}
