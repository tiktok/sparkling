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

function getQueryItems(
  globalProps?: Record<string, unknown> | null,
): Record<string, unknown> | undefined {
  const qi = globalProps?.queryItems;
  if (qi && typeof qi === 'object' && !Array.isArray(qi)) {
    return qi as Record<string, unknown>;
  }
  return undefined;
}

/** True for scheme flags like `trans_status_bar=1` / `true`. */
function isQueryFlagEnabled(value: unknown): boolean {
  if (value === true || value === 1) {
    return true;
  }
  if (typeof value === 'string') {
    const v = value.trim().toLowerCase();
    return v === '1' || v === 'true' || v === 'yes';
  }
  return false;
}

/**
 * Android: Lynx is laid out in the window's content area below the status bar
 * unless `trans_status_bar=1` (see SparklingActivity.initStatusBar). Padding by
 * `statusBarHeight` again would duplicate that offset and leave an empty band
 * (often showing the native container color). Only add top inset when edge-to-edge.
 */
function androidTopInsetDp(globalProps: Record<string, unknown>): number {
  const queryItems = getQueryItems(globalProps);
  if (!isQueryFlagEnabled(queryItems?.trans_status_bar)) {
    return 0;
  }
  return toNumber(globalProps.statusBarHeight);
}

/**
 * Reads top/bottom (and optional horizontal) insets from global props.
 *
 * - **iOS**: `topHeight`, `bottomHeight` (window safe area; SPKGlobalPropsUtils)
 * - **Android**: top inset only when `queryItems.trans_status_bar` is enabled
 *   (content may draw under the status bar); bottom uses `navigationBarHeight`
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
      top: androidTopInsetDp(globalProps),
      right: 0,
      bottom: toNumber(globalProps.navigationBarHeight),
      left: 0,
    };
  }

  // Fallback when `os` is missing (e.g. tests): prefer iOS keys if present.
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
    top: androidTopInsetDp(globalProps),
    right: 0,
    bottom: toNumber(globalProps.navigationBarHeight),
    left: 0,
  };
}
