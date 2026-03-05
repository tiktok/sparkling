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

        let webBundlePath: string;
        if (urlParam) {
            // Dev mode: url param points to dev server, swap .lynx.bundle → .web.bundle
            webBundlePath = urlParam.replace(/\.lynx\.bundle$/, '.web.bundle');
        } else if (bundleParam) {
            // Production: swap extension
            webBundlePath = bundleParam.replace(/\.lynx\.bundle$/, '.web.bundle');
        } else {
            callback({ code: 0, msg: 'No bundle or url param in scheme' });
            return;
        }

        // Push browser history state
        const state = { bundle: webBundlePath, scheme };
        window.history.pushState(state, '', `?page=${encodeURIComponent(webBundlePath)}`);

        // Dispatch custom event for web shell to swap <lynx-view>
        window.dispatchEvent(new CustomEvent('sparkling:navigate', {
            detail: { url: webBundlePath, state },
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
