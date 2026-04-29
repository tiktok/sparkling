import { useState } from '@lynx-js/react'
import { navigate } from '../../lib/navigation.js'
import { ThemeProvider, useTheme } from '../../lib/theme.js'
import { DemoPage } from '../../components/DemoPage/index.js'
import './App.css'

interface Preset {
  name: string
  params: Record<string, any>
}

const PRESETS: Preset[] = [
  {
    name: 'Minimal',
    params: {},
  },
  {
    name: 'With Title',
    params: { title: 'My Page' },
  },
  {
    name: 'Fullscreen Immersive',
    params: { hide_nav_bar: 1, trans_status_bar: 1 },
  },
  {
    name: 'Force Dark Theme',
    params: {
      title: 'Dark Page',
      force_theme_style: 'dark',
      container_bg_color: '#000000',
      nav_bar_color: '#000000',
      title_color: '#ffffff',
    },
  },
  {
    name: 'Force Light Theme',
    params: {
      title: 'Light Page',
      force_theme_style: 'light',
      container_bg_color: '#f0f2f5',
      nav_bar_color: '#ffffff',
      title_color: '#000000',
    },
  },
  {
    name: 'Custom Nav Bar Color',
    params: { title: 'Branded', nav_bar_color: '#009995', title_color: '#ffffff' },
  },
  {
    name: 'Hidden Status Bar',
    params: { title: 'No Status Bar', hide_status_bar: 1 },
  },
]

function SchemePresetsContent() {
  const { resolved } = useTheme()
  const isDark = resolved === 'dark'
  const dk = (base: string) => `${base} ${isDark ? `${base}--dark` : `${base}--light`}`

  const [results, setResults] = useState<Record<string, { code: number; msg: string }>>({})

  const handleTry = (preset: Preset) => {
    'background only'
    navigate({
      path: 'gp-container.lynx.bundle',
      options: { params: preset.params },
    }, (res: { code: number; msg?: string }) => {
      setResults((prev) => ({
        ...prev,
        [preset.name]: {
          code: res.code,
          msg: res.msg || (res.code === 0 ? 'Success' : 'Failed'),
        },
      }))
    })
  }

  return (
    <DemoPage title="Presets">
      {PRESETS.map((preset) => {
        const result = results[preset.name]
        return (
          <view className={dk('preset-card')} key={preset.name}>
            <text className={dk('preset-name')}>{preset.name}</text>
            <view className="preset-params">
              {Object.entries(preset.params).map(([key, value]) => (
                <view className="preset-param-row" key={key}>
                  <text className={dk('preset-param-key')}>{key}:</text>
                  <text className={dk('preset-param-value')}>{String(value)}</text>
                </view>
              ))}
              {Object.keys(preset.params).length === 0 ? (
                <text className={dk('preset-param-value')}>(no extra params)</text>
              ) : null}
            </view>

            <view className={dk('try-button')} bindtap={() => handleTry(preset)}>
              <text className="try-button-text">Try It</text>
            </view>

            {result !== undefined ? (
              <view className="preset-result">
                <text className={dk('preset-result-label')}>Result:</text>
                <text
                  className={`preset-result-code ${result.code === 0 ? 'preset-result-code--success' : 'preset-result-code--error'}`}
                >
                  {result.code} — {result.msg}
                </text>
              </view>
            ) : null}
          </view>
        )
      })}
    </DemoPage>
  )
}

export function App() {
  return (
    <ThemeProvider>
      <SchemePresetsContent />
    </ThemeProvider>
  )
}
