// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.registry.core

/**
 * Shared contract for the open-source and downstream platform type sets, so that code which does
 * not care where a platform was declared can name a single type.
 *
 * It is deliberately a bare marker: the concrete sets are enums, and an enum cannot satisfy a
 * `name` property because `Enum.name` is final.
 */
interface IBridgePlatformType

/**
 * Desc: platform types supported by the minimum bridge runtime.
 */
enum class BridgePlatformType : IBridgePlatformType {
    WEB,
    LYNX,
    NONE,
    ALL,
}
