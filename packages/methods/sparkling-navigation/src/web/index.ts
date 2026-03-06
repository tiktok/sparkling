// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import { registerWebMethod } from 'sparkling-method/web-registry';

registerWebMethod('router.open', (params, callback) => {
    const scheme = (params.data as Record<string, unknown>)?.scheme as string | undefined;

    if (!scheme) {
        callback({ code: 0, msg: 'scheme is required' });
        return;
    }

    try {
        const url = new URL(scheme);
        const bundleParam = url.searchParams.get('bundle');
        const urlParam = url.searchParams.get('url');

        // Extract page name (without extension).
        // Web shell constructs the full URL as /${page}.lynx.bundle
        let pageName: string;
        if (urlParam) {
            // Dev mode: url param is a full URL, extract the basename
            const urlPath = new URL(urlParam).pathname;
            pageName = urlPath.replace(/^\//, '').replace(/\.lynx\.bundle$/, '');
        } else if (bundleParam) {
            pageName = bundleParam.replace(/\.lynx\.bundle$/, '');
        } else {
            callback({ code: 0, msg: 'No bundle or url param in scheme' });
            return;
        }

        // Push browser history state
        const state = { page: pageName, scheme };
        window.history.pushState(state, '', `?page=${encodeURIComponent(pageName)}`);

        // Dispatch custom event for web shell to swap <lynx-view>
        window.dispatchEvent(new CustomEvent('sparkling:navigate', {
            detail: { page: pageName, state },
        }));

        callback({ code: 1, msg: 'ok' });
    } catch (e) {
        callback({ code: 0, msg: `Failed to parse scheme: ${e}` });
    }
});

registerWebMethod('router.close', (_params, callback) => {
    window.history.back();
    callback({ code: 1, msg: 'ok' });
});
