// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

/**
 * Generic pipe response structure
 */
export interface PipeResponse<T = unknown> {
  code: number;
  msg: string;
  data?: T;
}

/**
 * Error response from pipe call
 */
export interface PipeErrorResponse {
  code: number;
  msg: string;
  data?: undefined;
}

/**
 * Pipe call options
 */
export interface PipeCallOptions {
  timeout?: number;
  priority?: 'high' | 'normal' | 'low';
  retryCount?: number;
  retryDelay?: number;
}

/**
 * Pipe method specification
 */
export type MethodMap = string | {
  module: string;
  method: string;
};

/**
 * Pipe event callback
 */
export type EventCallback = (event: unknown) => void;

/**
 * Handler function for a web method implementation.
 * Receives the same envelope that native modules receive and must
 * invoke the callback with a `{ code, msg, data? }` response.
 */
export type WebMethodHandler = (
    params: { containerID: string; protocolVersion: string; data: unknown },
    callback: (response: { code: number; msg: string; data?: unknown }) => void,
) => void;
