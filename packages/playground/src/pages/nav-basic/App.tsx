import { useState } from '@lynx-js/react'
import { navigate } from '../../lib/navigation.js'
import { ThemeProvider, useTheme } from '../../lib/theme.js'
import { DemoPage } from '../../components/DemoPage/index.js'
import { FormField } from '../../components/FormField/index.js'
import { ResultCard } from '../../components/ResultCard/index.js'

import './App.css'

function NavBasicContent() {
  const { resolved } = useTheme()
  const isDark = resolved === 'dark'
  const dk = (base: string) => `${base} ${isDark ? `${base}--dark` : `${base}--light`}`

  const [key1, setKey1] = useState('userId')
  const [val1, setVal1] = useState('42')
  const [key2, setKey2] = useState('action')
  const [val2, setVal2] = useState('view')
  const [navResult, setNavResult] = useState<{ code: number; msg: string } | undefined>(undefined)

  const handleNavigate = () => {
    'background only'
    const params: Record<string, any> = {
      title: 'Navigation Target',
      hide_nav_bar: 0,
      container_bg_color: '#000000',
      nav_bar_color: '#000000',
      title_color: '#FFFFFF',
      force_theme_style: 'dark',
    }
    if (key1) params[key1] = val1
    if (key2) params[key2] = val2

    navigate(
      { path: 'gp-container.lynx.bundle', options: { params } },
      (res: { code: number; msg?: string }) => {
        setNavResult({ code: res.code, msg: res.msg || 'ok' })
      },
    )
  }

  return (
    <DemoPage title="Passing Data">
      <view className={dk('nav-section')}>
        <text className={dk('nav-section-title')}>Send Custom Params</text>
        <text className={dk('nav-section-desc')}>
          Enter key-value params below and navigate. The target page will show them as queryItems.
        </text>

        <view className="default-hint">
          <text className="default-badge">DEFAULTS</text>
          <text className={dk('default-text')}>userId=42, action=view</text>
        </view>

        <FormField type="input" label={key1 || 'key'} description="Custom param 1" value={val1} placeholder="value" onInput={setVal1} />
        <FormField type="input" label={key2 || 'key'} description="Custom param 2" value={val2} placeholder="value" onInput={setVal2} />

        <view className={dk('nav-button')} bindtap={handleNavigate}>
          <text className="nav-button-text">Navigate with Params</text>
        </view>
        <ResultCard label="Response" code={navResult?.code} msg={navResult?.msg} />
      </view>
    </DemoPage>
  )
}

export function App() {
  return (
    <ThemeProvider>
      <NavBasicContent />
    </ThemeProvider>
  )
}
