// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import { createContext, useContext, useState, useCallback, useEffect } from '@lynx-js/react'
import _pipe, { type EventCallback, type PipeResponse } from 'sparkling-method'
import { parseThemePreference, type ThemePreference } from './themePreference.js'

type SparklingPipe = typeof import('sparkling-method').default
const pipe = _pipe as unknown as SparklingPipe

export type { ThemePreference } from './themePreference.js'
export type ResolvedTheme = 'light' | 'dark'

interface ThemeContextValue {
  preference: ThemePreference
  resolved: ResolvedTheme
  setPreference: (pref: ThemePreference) => void
}

const ThemeContext = createContext<ThemeContextValue>({
  preference: 'Auto',
  resolved: 'dark',
  setPreference: () => {},
})

function resolveTheme(preference: ThemePreference): ResolvedTheme {
  const p = preference.toLowerCase()
  if (p === 'light') return 'light'
  if (p === 'dark') return 'dark'
  // Auto: check system theme from globalProps
  const systemTheme = lynx.__globalProps?.theme
  if (systemTheme === 'light') return 'light'
  return 'dark'
}

function getInitialPreference(): ThemePreference {
  return parseThemePreference(lynx.__globalProps)
}

export function ThemeProvider(props: { children: any }) {
  const [preference, setPreferenceState] = useState<ThemePreference>(getInitialPreference)

  useEffect(() => {
    const handleGlobalPropsUpdated: EventCallback = () => {
      setPreferenceState(getInitialPreference())
    }
    const listener = pipe.on('globalPropsUpdated', handleGlobalPropsUpdated)
    return () => pipe.off('globalPropsUpdated', listener)
  }, [])

  const setPreference = useCallback((pref: ThemePreference) => {
    pipe.call('sparkling.setThemePreference', {
      preference: pref === 'Auto' ? 'follow-system' : pref.toLowerCase(),
    }, (response: unknown) => {
      const result = response as PipeResponse<{ preference?: string }>
      if (result.code === 1) {
        setPreferenceState(parseThemePreference({
          ...lynx.__globalProps,
          preferredTheme: result.data?.preference,
        }))
      }
    })
  }, [])

  const resolved = resolveTheme(preference)

  return (
    <ThemeContext.Provider value={{ preference, resolved, setPreference }}>
      {props.children}
    </ThemeContext.Provider>
  )
}

export function useTheme() {
  return useContext(ThemeContext)
}
