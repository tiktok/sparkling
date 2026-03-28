// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import pipe from 'sparkling-method';
import { getDevServerBaseURL } from '../devServer';
import type { OpenRequest, OpenResponse } from './open.d';

/**
 * Open a new page or route
 * @param params |
 * @param callback
 */
export function open(params: OpenRequest, callback: (result: OpenResponse) => void): void {
    if (!params) {
        const errorResponse: OpenResponse = {
            code: -1,
            msg: 'Invalid params: params cannot be null or undefined',
        };
        if (typeof callback === 'function') {
            callback(errorResponse);
        }
        return;
    }

    if (!params.scheme || typeof params.scheme !== 'string' || !params.scheme.trim()) {
        const errorResponse: OpenResponse = {
            code: -1,
            msg: 'Invalid params: scheme must be a non-empty string',
        };
        if (typeof callback === 'function') {
            callback(errorResponse);
        }
        return;
    }

    if (typeof callback !== 'function') {
        console.error('[sparkling-navigation] open: callback must be a function');
        return;
    }

    // In dev mode, rewrite bundle= to url= so the native side loads from the
    // dev server instead of stale local assets.
    let scheme = params.scheme.trim();
    const devBase = getDevServerBaseURL();
    if (devBase) {
        try {
            const u = new URL(scheme);
            const bundle = u.searchParams.get('bundle');
            if (bundle && !u.searchParams.get('url')) {
                u.searchParams.delete('bundle');
                u.searchParams.set('url', `${devBase.replace(/\/+$/, '')}/${bundle.replace(/^(?:\.\/|\/)+/, '')}`);
                scheme = u.toString();
            }
        } catch {
            // scheme is not a valid URL, pass through as-is
        }
    }

    pipe.call('router.open', {
        scheme,
        ...params.options,
    }, (v: unknown) => {
        const response = v as OpenResponse;
        const code = response?.code ?? -1;
        // Pipe status codes: 1 = succeeded, 0 = failed, negative = various errors.
        // When the native side reports success it may omit `msg`, so fall back to
        // 'ok' instead of the misleading 'Unknown error'.
        const isSuccess = code === 1;
        const msg = response?.msg ?? (isSuccess ? 'ok' : 'Unknown error');
        callback({ code, msg });
    });
}
