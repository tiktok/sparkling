// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

/// <reference path="typing.d.ts" />

import type { PipeResponse, PipeErrorResponse, PipeCallOptions, MethodMap, EventCallback } from './types';

export type {
    PipeResponse,
    PipeErrorResponse,
    PipeCallOptions,
    MethodMap,
    EventCallback,
};

/**
 * Get the container ID from Lynx global props
 * @returns Container ID string, empty string if not available
 */
function getContainerID(): string {
    try {
        if (typeof lynx !== 'undefined' && lynx && lynx.__globalProps) {
            return lynx.__globalProps.containerID ?? '';
        }
        return '';
    } catch {
        return '';
    }
}

/**
 * Get the Lynx context for event handling
 * @returns Lynx context object
 * @throws Error if Lynx context is not available
 */
function getLynxContext(): typeof lynx {
    if (typeof lynx === 'object' && lynx && typeof lynx.getJSModule === 'function') {
        return lynx;
    }
    throw new Error('Lynx context not available. Ensure you are running in a Lynx environment.');
}

/**
 * Create an error response for failed pipe calls
 */
function createErrorResponse(code: number, msg: string): PipeErrorResponse {
    return { code, msg };
}

const LynxPipe = {
    /**
     * Call a native method through the pipe
     * @param methodMap - Method name string or object with module and method
     * @param params - Parameters to pass to the native method
     * @param callback - Callback function to receive the result
     * @param options - Additional options (reserved for future use)
     */
    call<TParams = unknown, TResponse = unknown>(
        methodMap: string | { module: string; method: string },
        params: TParams,
        callback: (v: unknown) => void,
        options: Record<string, unknown> = {}
    ): void {
        // Validate callback is provided and is a function
        if (typeof callback !== 'function') {
            console.error('[LynxPipe] callback must be a function');
            return;
        }

        let module: string;
        let method: string;

        if (typeof methodMap === 'object' && methodMap !== null) {
            // Validate module and method are non-empty strings
            if (!methodMap.module || typeof methodMap.module !== 'string') {
                callback(createErrorResponse(-1, 'Invalid module name: module must be a non-empty string'));
                return;
            }
            if (!methodMap.method || typeof methodMap.method !== 'string') {
                callback(createErrorResponse(-1, 'Invalid method name: method must be a non-empty string'));
                return;
            }
            module = methodMap.module;
            method = methodMap.method;
        } else if (typeof methodMap === 'string' && methodMap.trim()) {
            module = 'spkPipe';
            method = methodMap;
        } else {
            callback(createErrorResponse(-1, 'Invalid methodMap: must be a non-empty string or object with module and method'));
            return;
        }

        // Check if NativeModules is available
        if (typeof NativeModules === 'undefined') {
            callback(createErrorResponse(-2, 'NativeModules is not available. Ensure you are running in a lynx environment.'));
            return;
        }

        // Check if the specified module exists
        if (!NativeModules[module]) {
            callback(createErrorResponse(-3, `Native module "${module}" is not registered.`));
            return;
        }

        // Check if the module has a call method
        if (typeof NativeModules[module].call !== 'function') {
            callback(createErrorResponse(-4, `Native module "${module}" does not have a callable "call" method.`));
            return;
        }

        // Log in development mode only
        if (process.env.NODE_ENV === 'development') {
            console.log('[LynxPipe] call', module, method, params);
        }

        try {
            NativeModules[module].call(
                method,
                {
                    containerID: getContainerID(),
                    protocolVersion: '1.0.0',
                    data: params ?? null,
                },
                callback
            );
        } catch (error) {
            const errorMsg = error instanceof Error ? error.message : String(error);
            callback(createErrorResponse(-5, `Pipe call failed: ${errorMsg}`));
        }
    },

    /**
     * Register an event listener
     * @param eventName - Name of the event to listen for
     * @param callback - Callback function to handle the event
     * @returns The callback function for later removal
     * @throws Error if eventName is empty or callback is not a function
     */
    on: function (eventName: string, callback: EventCallback): EventCallback {
        // Validate eventName
        if (!eventName || typeof eventName !== 'string') {
            throw new Error('eventName must be a non-empty string');
        }

        // Validate callback
        if (typeof callback !== 'function') {
            throw new Error('callback must be a function');
        }

        const context = getLynxContext();
        const eventEmitter = context.getJSModule('GlobalEventEmitter');

        if (!eventEmitter || typeof eventEmitter.addListener !== 'function') {
            throw new Error('GlobalEventEmitter is not available or does not support addListener');
        }

        eventEmitter.addListener(eventName, callback, this);
        return callback;
    },

    /**
     * Remove an event listener
     * @param eventName - Name of the event to stop listening for
     * @param callback - The callback function to remove
     * @throws Error if eventName is empty or callback is not a function
     */
    off: function (eventName: string, callback: EventCallback): void {
        // Validate eventName
        if (!eventName || typeof eventName !== 'string') {
            throw new Error('eventName must be a non-empty string');
        }

        // Validate callback
        if (typeof callback !== 'function') {
            throw new Error('callback must be a function');
        }

        const context = getLynxContext();
        const eventEmitter = context.getJSModule('GlobalEventEmitter');

        if (!eventEmitter || typeof eventEmitter.removeListener !== 'function') {
            throw new Error('GlobalEventEmitter is not available or does not support removeListener');
        }

        eventEmitter.removeListener(eventName, callback);
    },

    /**
     * Call a native method through the pipe and return a Promise
     * @param methodMap - Method name string or object with module and method
     * @param params - Parameters to pass to the native method
     * @param options - Additional options (reserved for future use)
     * @param timeout - Optional timeout in milliseconds (default: 30000)
     * @returns Promise that resolves with the response data
     */
    callAsync<TParams, TResponse>(
        methodMap: string | { module: string; method: string },
        params: TParams,
        options: Record<string, unknown> = {},
        timeout = 30000
    ): Promise<TResponse> {
        return new Promise((resolve, reject) => {
            const timer = setTimeout(() => {
                reject(new Error(`Pipe call timeout after ${timeout}ms`));
            }, timeout);

            this.call<TParams, TResponse>(methodMap, params, (response: unknown) => {
                clearTimeout(timer);

                const rawResponse = response as PipeRawResponse;

                if (rawResponse.code === 1) {
                    resolve(rawResponse.data as TResponse);
                } else {
                    reject(new Error(`Pipe call failed: ${rawResponse.msg}`));
                }
            }, options);
        });
    },

    /**
     * Call a native method through the pipe with a custom timeout
     * @param methodMap - Method name string or object with module and method
     * @param params - Parameters to pass to the native method
     * @param timeout - Timeout in milliseconds
     * @param options - Additional options
     * @returns Promise that resolves with the response data
     */
    callWithTimeout<TParams, TResponse>(
        methodMap: string | { module: string; method: string },
        params: TParams,
        timeout: number,
        options: Record<string, unknown> = {}
    ): Promise<TResponse> {
        return this.callAsync<TParams, TResponse>(methodMap, params, options, timeout);
    }
};

export default LynxPipe;
