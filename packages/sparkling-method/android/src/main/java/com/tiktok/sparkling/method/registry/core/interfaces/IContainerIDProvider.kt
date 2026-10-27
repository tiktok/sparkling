// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core.interfaces

/**
 * Provides the ID of the container a bridge call comes from.
 */
interface IContainerIDProvider {
    fun provideContainerID(): String?
}
