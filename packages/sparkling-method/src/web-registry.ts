// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import type { WebMethodHandler } from './types';

export type { WebMethodHandler };

const registry = new Map<string, WebMethodHandler>();

/**
 * Register a web implementation for a method.
 * Called at import time by method package `/web` subpath exports.
 */
export function registerWebMethod(methodName: string, handler: WebMethodHandler): void {
    if (!methodName || typeof methodName !== 'string') {
        throw new Error('methodName must be a non-empty string');
    }
    if (typeof handler !== 'function') {
        throw new Error('handler must be a function');
    }
    registry.set(methodName, handler);
}

/**
 * Look up a registered web handler by method name.
 */
export function getWebMethodHandler(methodName: string): WebMethodHandler | undefined {
    return registry.get(methodName);
}

/**
 * Check whether a web handler is registered for the given method name.
 */
export function hasWebMethod(methodName: string): boolean {
    return registry.has(methodName);
}

/**
 * Detect whether the current environment is a web browser
 * (as opposed to the Lynx native runtime).
 *
 * Note: We check for `document` rather than the absence of `NativeModules`
 * because `@lynx-js/web-core` defines a `NativeModules` stub in the browser.
 * Native Lynx does not provide a `document` global.
 */
export function isWebEnvironment(): boolean {
    return typeof document !== 'undefined';
}
