// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import { describe, expect, it } from 'vitest'
import { parseThemePreference } from './themePreference.js'

describe('parseThemePreference', () => {
  it('prefers force_theme_style over persisted preferredTheme', () => {
    expect(parseThemePreference({
      preferredTheme: 'dark',
      queryItems: { force_theme_style: 'light' },
    })).toBe('Light')
  })

  it('uses persisted preferredTheme when no force override exists', () => {
    expect(parseThemePreference({ preferredTheme: 'dark' })).toBe('Dark')
  })

  it('maps follow-system and missing values to Auto', () => {
    expect(parseThemePreference({ preferredTheme: 'follow-system' })).toBe('Auto')
    expect(parseThemePreference()).toBe('Auto')
  })
})
