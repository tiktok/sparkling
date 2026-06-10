// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

export const createSuccessResponseBase = (extra?: any) => ({
  code: 1,
  msg: 'Success',
  ...extra,
});

export const createErrorResponseBase = (code: number = -1, msg: string = 'Error', extra?: any) => ({
  code,
  msg,
  ...extra,
});
