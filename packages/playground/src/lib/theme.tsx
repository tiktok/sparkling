import { createContext, useContext, useState, useCallback } from '@lynx-js/react'

export type ThemePreference = 'Auto' | 'Light' | 'Dark'
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
  const gp = (lynx.__globalProps || {}) as Record<string, any>
  // The SDK puts force_theme_style in queryItems (nested), not as top-level preferredTheme.
  // Check both locations for robustness.
  const raw = gp.preferredTheme
    || (gp.queryItems as Record<string, any>)?.force_theme_style
    || 'Auto'
  const lower = String(raw).toLowerCase()
  if (lower === 'light') return 'Light'
  if (lower === 'dark') return 'Dark'
  return 'Auto'
}

export function ThemeProvider(props: { children: any }) {
  const [preference, setPreferenceState] = useState<ThemePreference>(getInitialPreference)

  const setPreference = useCallback((pref: ThemePreference) => {
    setPreferenceState(pref)
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
