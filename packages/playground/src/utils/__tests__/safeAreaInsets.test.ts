// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import { describe, expect, it } from 'vitest'

import { getSafeAreaInsetsFromGlobalProps } from '../safeAreaInsets.js'

describe('getSafeAreaInsetsFromGlobalProps', () => {
  it('returns zeros when props are missing', () => {
    expect(getSafeAreaInsetsFromGlobalProps(undefined)).toEqual({
      top: 0,
      right: 0,
      bottom: 0,
      left: 0,
    })
  })

  it('uses topHeight and bottomHeight on iOS', () => {
    expect(
      getSafeAreaInsetsFromGlobalProps({
        os: 'ios',
        topHeight: 59,
        bottomHeight: 34,
      }),
    ).toEqual({ top: 59, right: 0, bottom: 34, left: 0 })
  })

  it('uses statusBarHeight and navigationBarHeight on Android', () => {
    expect(
      getSafeAreaInsetsFromGlobalProps({
        os: 'android',
        statusBarHeight: 24,
        navigationBarHeight: 48,
      }),
    ).toEqual({ top: 24, right: 0, bottom: 48, left: 0 })
  })

  it('falls back to iOS keys when os is unknown but topHeight is set', () => {
    expect(
      getSafeAreaInsetsFromGlobalProps({
        topHeight: 20,
        bottomHeight: 0,
      }),
    ).toEqual({ top: 20, right: 0, bottom: 0, left: 0 })
  })
})
