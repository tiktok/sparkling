// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import { open } from '../open/open';
import type {
    NavigateRequest,
    NavigateResponse,
    NavigateOptions,
    NavigateParams,
} from './navigate.d';

const DEFAULT_ROUTER_SCHEME = 'hybrid://lynxview_page';
const PROTOCOL_REGEX = /^[a-z][a-z0-9+.-]*:\/\//i;
const ALLOWED_SCHEME_PARAMS = new Set<string>([
    'bundle',
    'title',
    'fallback_url',
    'title_color',
    'hide_nav_bar',
    'nav_bar_color',
    'screen_orientation',
    'hide_status_bar',
    'trans_status_bar',
    'show_nav_bar_in_trans_status_bar',
    'hide_loading',
    'loading_bg_color',
    'container_bg_color',
    'hide_error',
    'force_theme_style',
]);

const THEMED_COLOR_PARAMS = new Set<string>([
    'title_color',
    'nav_bar_color',
    'loading_bg_color',
    'container_bg_color',
]);

function isAllowedParam(key: string): boolean {
    if (ALLOWED_SCHEME_PARAMS.has(key)) {
        return true;
    }
    for (const colorKey of THEMED_COLOR_PARAMS) {
        if (key === `${colorKey}_light` || key === `${colorKey}_dark`) {
            return true;
        }
    }
    return false;
}

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
    searchParams.set('bundle', bundlePath);

    if (params && typeof params === 'object') {
        for (const key of Object.keys(params)) {
            if (!isAllowedParam(key)) {
                continue;
            }

            const value = params[key as keyof NavigateParams];

            if (value === undefined || value === null) {
                continue;
            }

            searchParams.append(key, String(value));
        }
    }

    return `${sanitizedBase}?${searchParams.toString()}`;
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
