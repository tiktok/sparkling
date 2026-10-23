// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

export type ThemePreference = 'Auto' | 'Light' | 'Dark'

interface ThemeGlobalProps {
  preferredTheme?: unknown
  queryItems?: Record<string, unknown> | null
}

function normalizeThemePreference(value: unknown): ThemePreference | undefined {
  const normalized = String(value ?? '').toLowerCase()
  if (normalized === 'light') return 'Light'
  if (normalized === 'dark') return 'Dark'
  if (normalized === 'follow-system' || normalized === 'auto') return 'Auto'
  return undefined
}

export function parseThemePreference(globalProps?: ThemeGlobalProps | null): ThemePreference {
  return normalizeThemePreference(globalProps?.queryItems?.force_theme_style)
    ?? normalizeThemePreference(globalProps?.preferredTheme)
    ?? 'Auto'
}
