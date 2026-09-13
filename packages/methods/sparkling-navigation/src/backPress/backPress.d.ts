// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
export interface SetBackPressInterceptRequest {
  /** True to handle back in the page, false to give it back to the container. */
  intercept: boolean;
  /** Defaults to the calling container. */
  containerID?: string;
}

export interface SetBackPressInterceptResponse {
  code: number;
  msg: string;
  /** False where there is no hardware back button, or the host declined. */
  supported?: boolean;
}
