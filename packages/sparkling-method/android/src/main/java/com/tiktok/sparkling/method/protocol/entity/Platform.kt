// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

package com.tiktok.sparkling.method.protocol.entity

/**
 * The type bridge APIs use to say which front end issued a call. [Platform] holds the values the
 * minimum runtime serves; a host can contribute its own values on this same type.
 *
 * Implementations are enums, so [name] is satisfied by the enum entry name.
 */
interface IPlatform {
    val name: String
}

/**
 * Front ends the minimum runtime can serve.
 */
enum class Platform : IPlatform {
    Lynx, Web, Other,
}
