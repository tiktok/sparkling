// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
package com.tiktok.sparkling.debugtool

import java.net.URI

internal const val DEV_URL_ERROR_MESSAGE = "The url must be a valid http:// or https:// URL"

internal fun normalizedDevUrl(url: String?): String? {
    val normalized = url?.trim().orEmpty()
    if (normalized.isEmpty()) return null

    val parsed = runCatching { URI(normalized) }.getOrNull() ?: return null
    val scheme = parsed.scheme?.lowercase()
    if (scheme != "http" && scheme != "https") return null
    if (parsed.host.isNullOrEmpty()) return null

    return normalized
}
