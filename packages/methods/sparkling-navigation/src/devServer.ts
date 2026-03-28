// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

declare const __DEV__: boolean;
declare const __webpack_public_path__: string;

export function getDevServerBaseURL(): string | undefined {
    try {
        if (typeof __DEV__ !== 'undefined' && __DEV__ && typeof __webpack_public_path__ === 'string' && __webpack_public_path__) {
            return __webpack_public_path__;
        }
    } catch {
        // __DEV__ or __webpack_public_path__ not available
    }
    return undefined;
}
