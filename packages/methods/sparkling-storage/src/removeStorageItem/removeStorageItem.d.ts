// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
export interface RemoveItemRequest {
  key: string;
  biz?: string;
}

export interface RemoveItemResponse {
  code: number;
  msg: string;
}
