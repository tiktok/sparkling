// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import pipe from 'sparkling-method';
import type { RemoveItemRequest, RemoveItemResponse } from './removeStorageItem.d';

export function removeItem(params: RemoveItemRequest, callback: (result: RemoveItemResponse) => void): void {
  if (!params?.key || typeof params.key !== 'string' || !params.key.trim()) {
    callback?.({ code: -1, msg: 'Invalid params: key must be a non-empty string' });
    return;
  }
  if (typeof callback !== 'function') return;
  pipe.call('storage.removeItem', { key: params.key.trim(), biz: params.biz }, (value: unknown) => {
    const response = value as RemoveItemResponse | undefined;
    const code = response?.code ?? -1;
    callback({ code, msg: response?.msg ?? (code === 1 ? 'ok' : 'Unknown error') });
  });
}
