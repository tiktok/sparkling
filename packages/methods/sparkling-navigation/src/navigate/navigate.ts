// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import { open } from '../open/open';
import { getDevServerBaseURL } from '../devServer';
import type {
    NavigateRequest,
    NavigateResponse,
    NavigateOptions,
    NavigateParams,
} from './navigate.d';

const DEFAULT_ROUTER_SCHEME = 'hybrid://lynxview_page';
const PROTOCOL_REGEX = /^[a-z][a-z0-9+.-]*:\/\//i;

function createErrorResponse(msg: string): NavigateResponse {
    return {
        code: -1,
        msg,
    };
}

function normalizePath(path: string): string {
    let normalized = path.trim();
    // Remove leading "./" or "/" to keep the bundle path relative
    normalized = normalized.replace(/^(?:\.\/|\/)+/, '');
    return normalized;
}

function buildScheme(baseScheme: string, bundlePath: string, params?: NavigateOptions['params']): string {
    const sanitizedBase = (baseScheme || DEFAULT_ROUTER_SCHEME).trim().replace(/[?&]+$/, '') || DEFAULT_ROUTER_SCHEME;
    const searchParams = new URLSearchParams();

    const devBaseURL = getDevServerBaseURL();
    if (devBaseURL) {
        // In dev mode, use url= to load bundles from the dev server
        const fullURL = `${devBaseURL.replace(/\/+$/, '')}/${bundlePath}`;
        searchParams.set('url', fullURL);
    } else {
        searchParams.set('bundle', bundlePath);
    }

    if (params && typeof params === 'object') {
        for (const key of Object.keys(params)) {
            if (devBaseURL) {
                if (key === 'url' || key === 'bundle') {
                    continue;
                }
            } else if (key === 'bundle' || key === 'url') {
                continue;
            }

            const value = (params as Record<string, unknown>)[key];

            if (value === undefined || value === null) {
                continue;
            }

            searchParams.append(key, String(value));
        }
    }

    // URLSearchParams encodes spaces as '+' (x-www-form-urlencoded), but
    // native URL parsers only understand '%20'. Replace to avoid mangled values.
    return `${sanitizedBase}?${searchParams.toString().replace(/\+/g, '%20')}`;
}

export function navigate(params: NavigateRequest, callback: (result: NavigateResponse) => void): void {
    if (!params) {
        const errorResponse = createErrorResponse('Invalid params: params cannot be null or undefined');
        if (typeof callback === 'function') {
            callback(errorResponse);
        }
        return;
    }

    if (!params.path || typeof params.path !== 'string' || !params.path.trim()) {
        const errorResponse = createErrorResponse('Invalid params: path must be a non-empty string');
        if (typeof callback === 'function') {
            callback(errorResponse);
        }
        return;
    }

    if (PROTOCOL_REGEX.test(params.path.trim())) {
        const errorResponse = createErrorResponse('Invalid params: path must be a relative path, not a full scheme');
        if (typeof callback === 'function') {
            callback(errorResponse);
        }
        return;
    }

    if (typeof callback !== 'function') {
        console.error('[sparkling-navigation] navigate: callback must be a function');
        return;
    }

    const bundlePath = normalizePath(params.path);
    const { params: schemeParams, ...restOptions } = params.options ?? {};
    delete (restOptions as Record<string, unknown>).extra;

    if (!bundlePath) {
        callback(createErrorResponse('Invalid params: path must resolve to a bundle name'));
        return;
    }

    const scheme = buildScheme(params.baseScheme ?? DEFAULT_ROUTER_SCHEME, bundlePath, schemeParams);

    open(
        {
            scheme,
            options: Object.keys(restOptions).length ? restOptions : undefined,
        },
        callback
    );
}
