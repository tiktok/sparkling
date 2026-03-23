// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import type { ReactNode } from 'react'

import {
  getSafeAreaInsetsFromGlobalProps,
  type SafeAreaInsets,
} from '../utils/safeAreaInsets.js'

export type SafeAreaEdge = 'top' | 'bottom' | 'left' | 'right'

export interface SafeAreaViewProps {
  children?: ReactNode
  edges?: SafeAreaEdge[]
  globalProps?: Record<string, unknown> | null
  className?: string
  style?: Record<string, string | number | undefined>
}

function snapshotGlobalProps(): Record<string, unknown> | undefined {
  if (typeof lynx === 'undefined') {
    return undefined
  }
  return lynx.__globalProps as Record<string, unknown> | undefined
}

function paddingFromInsets(
  insets: SafeAreaInsets,
  edges: SafeAreaEdge[],
): Record<string, string> {
  const pad: Record<string, string> = {}
  if (edges.includes('top')) {
    pad.paddingTop = `${insets.top}px`
  }
  if (edges.includes('bottom')) {
    pad.paddingBottom = `${insets.bottom}px`
  }
  if (edges.includes('left')) {
    pad.paddingLeft = `${insets.left}px`
  }
  if (edges.includes('right')) {
    pad.paddingRight = `${insets.right}px`
  }
  return pad
}

/**
 * Applies safe-area padding from Sparkling `globalProps` so Lynx content stays
 * clear of the status bar and home indicator after native edge-to-edge layout.
 */
export function SafeAreaView(props: SafeAreaViewProps) {
  const {
    children,
    edges = ['top', 'bottom', 'left', 'right'],
    globalProps: globalPropsProp,
    className,
    style,
  } = props

  const raw = globalPropsProp ?? snapshotGlobalProps()
  const insets = getSafeAreaInsetsFromGlobalProps(raw)
  const padding = paddingFromInsets(insets, edges)

  const mergedStyle: Record<string, string | number | undefined> = {
    boxSizing: 'border-box',
    width: '100%',
    ...padding,
    ...style,
  }

  return (
    <view className={className} style={mergedStyle}>
      {children}
    </view>
  )
}
