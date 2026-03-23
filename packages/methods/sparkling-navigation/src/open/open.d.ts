// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

export interface OpenRequest {
  scheme: string;
  options?: OpenOptions;
}

export interface OpenOptions {
  replace?: boolean; // Replace current page by new page, default: false
  replaceType?: string; // alwaysCloseAfterOpen | alwaysCloseBeforeOpen | onlyCloseAfterOpenSucceed, default: onlyCloseAfterOpenSucceed
  useSysBrowser?: boolean;
  animated?: boolean;
  interceptor?: string; // Custom router interceptor
  extra?: Record<string, unknown>;
}

export interface OpenResponse {
  code: number;
  msg: string;
}

declare function open(params: OpenRequest, callback: (result: OpenResponse) => void): void;
