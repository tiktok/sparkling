// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import { registerWebMethod } from 'sparkling-method/web-registry';

function storageKey(key: string, biz?: string): string {
    return biz ? `sparkling:${biz}:${key}` : `sparkling:${key}`;
}

registerWebMethod('storage.getItem', (params, callback) => {
    const data = params.data as Record<string, unknown> | null;
    const key = data?.key as string | undefined;
    const biz = data?.biz as string | undefined;

    if (!key) {
        callback({ code: 0, msg: 'key is required' });
        return;
    }

    try {
        const value = localStorage.getItem(storageKey(key, biz));
        callback({ code: 1, msg: 'ok', data: value });
    } catch (e) {
        callback({ code: 0, msg: `localStorage error: ${e}` });
    }
});

registerWebMethod('storage.setItem', (params, callback) => {
    const data = params.data as Record<string, unknown> | null;
    const key = data?.key as string | undefined;
    const value = data?.data;
    const biz = data?.biz as string | undefined;

    if (!key) {
        callback({ code: 0, msg: 'key is required' });
        return;
    }

    try {
        localStorage.setItem(
            storageKey(key, biz),
            typeof value === 'string' ? value : JSON.stringify(value),
        );
        callback({ code: 1, msg: 'ok' });
    } catch (e) {
        callback({ code: 0, msg: `localStorage error: ${e}` });
    }
});

registerWebMethod('storage.removeItem', (params, callback) => {
    const data = params.data as Record<string, unknown> | null;
    const key = data?.key as string | undefined;
    const biz = data?.biz as string | undefined;

    if (!key) {
        callback({ code: 0, msg: 'key is required' });
        return;
    }

    try {
        localStorage.removeItem(storageKey(key, biz));
        callback({ code: 1, msg: 'ok' });
    } catch (e) {
        callback({ code: 0, msg: `localStorage error: ${e}` });
    }
});
