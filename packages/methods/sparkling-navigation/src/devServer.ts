// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

declare const __DEV__: boolean;
declare const __webpack_public_path__: string;
declare const lynx: { __globalProps?: { queryItems?: Record<string, string> } } | undefined;

/**
 * Derive the dev server base URL from the actual bundle URL that the native
 * side used to load this page. Falls back to __webpack_public_path__.
 *
 * This ensures sub-page navigation uses the same host/port that the entry
 * page was loaded from — critical for real-device debugging where the native
 * side may use a LAN IP while __webpack_public_path__ is baked as localhost.
 */
export function getDevServerBaseURL(): string | undefined {
    try {
        if (typeof __DEV__ === 'undefined' || !__DEV__) {
            return undefined;
        }

        // Prefer the actual URL the native side used to load this bundle.
        // lynx.__globalProps.queryItems.url is set by the SDK from the scheme's
        // url= parameter (e.g. "http://192.168.1.100:5969/main.lynx.bundle").
        if (typeof lynx !== 'undefined' && lynx?.__globalProps?.queryItems?.url) {
            const pageUrl = lynx.__globalProps.queryItems.url;
            if (pageUrl.startsWith('http://') || pageUrl.startsWith('https://')) {
                const lastSlash = pageUrl.lastIndexOf('/');
                if (lastSlash > pageUrl.indexOf('//') + 1) {
                    return pageUrl.substring(0, lastSlash + 1);
                }
            }
        }

        // Fall back to build-time value.
        if (typeof __webpack_public_path__ === 'string' && __webpack_public_path__) {
            return __webpack_public_path__;
        }
    } catch {
        // globals not available
    }
    return undefined;
}
