// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

/**
 * Normalized safe-area insets (logical px / dp) derived from Sparkling
 * `lynx.__globalProps` (see SPKGlobalPropsUtils on iOS, GlobalPropsUtils on Android).
 */
export interface SafeAreaInsets {
  top: number;
  right: number;
  bottom: number;
  left: number;
}

function toNumber(value: unknown): number {
  if (typeof value === 'number' && !Number.isNaN(value)) {
    return value;
  }
  if (typeof value === 'string') {
    const n = parseFloat(value);
    return Number.isNaN(n) ? 0 : n;
  }
  return 0;
}

/**
 * Reads top/bottom (and optional horizontal) insets from global props.
 *
 * - **iOS**: `topHeight`, `bottomHeight` (SPKGlobalPropsUtils)
 * - **Android**: `statusBarHeight`, `navigationBarHeight` (RuntimeInfo)
 */
export function getSafeAreaInsetsFromGlobalProps(
  globalProps?: Record<string, unknown> | null,
): SafeAreaInsets {
  if (!globalProps) {
    return { top: 0, right: 0, bottom: 0, left: 0 };
  }

  const os = String(globalProps.os ?? '').toLowerCase();

  if (os === 'ios') {
    return {
      top: toNumber(globalProps.topHeight),
      right: 0,
      bottom: toNumber(globalProps.bottomHeight),
      left: 0,
    };
  }

  if (os === 'android') {
    return {
      top: toNumber(globalProps.statusBarHeight),
      right: 0,
      bottom: toNumber(globalProps.navigationBarHeight),
      left: 0,
    };
  }

  if (
    globalProps.topHeight !== undefined ||
    globalProps.bottomHeight !== undefined
  ) {
    return {
      top: toNumber(globalProps.topHeight),
      right: 0,
      bottom: toNumber(globalProps.bottomHeight),
      left: 0,
    };
  }

  return {
    top: toNumber(globalProps.statusBarHeight),
    right: 0,
    bottom: toNumber(globalProps.navigationBarHeight),
    left: 0,
  };
}
