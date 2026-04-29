import { useState, useCallback } from '@lynx-js/react'
import { open } from '../../lib/navigation.js'
import { ThemeProvider, useTheme } from '../../lib/theme.js'
import { DemoPage } from '../../components/DemoPage/index.js'
import { FormField } from '../../components/FormField/index.js'
import { CodeBlock } from '../../components/CodeBlock/index.js'
import { ResultCard } from '../../components/ResultCard/index.js'
import { ColorInput } from '../../components/ColorInput/index.js'

import './App.css'

function SchemeBuilderContent() {
  const { resolved } = useTheme()
  const isDark = resolved === 'dark'
  const dk = (base: string) => `${base} ${isDark ? `${base}--dark` : `${base}--light`}`

  // State for all scheme parameters
  const [bundle, setBundle] = useState('gp-container.lynx.bundle')
  const [title, setTitle] = useState('')
  const [hideNavBar, setHideNavBar] = useState(false)
  const [titleColor, setTitleColor] = useState('')
  const [containerBgColor, setContainerBgColor] = useState('')
  const [containerBgColorDark, setContainerBgColorDark] = useState('')
  const [containerBgColorLight, setContainerBgColorLight] = useState('')
  const [navBarColor, setNavBarColor] = useState('')
  const [navBarColorDark, setNavBarColorDark] = useState('')
  const [navBarColorLight, setNavBarColorLight] = useState('')
  const [titleColorDark, setTitleColorDark] = useState('')
  const [titleColorLight, setTitleColorLight] = useState('')
  const [forceThemeStyle, setForceThemeStyle] = useState('')
  const [hideStatusBar, setHideStatusBar] = useState(false)
  const [transStatusBar, setTransStatusBar] = useState(false)
  const [hideLoading, setHideLoading] = useState(false)
  const [hideError, setHideError] = useState(false)
  const [loadingBgColor, setLoadingBgColor] = useState('')

  // Result state
  const [resultCode, setResultCode] = useState<number | undefined>(undefined)
  const [resultMsg, setResultMsg] = useState<string | undefined>(undefined)

  const buildScheme = useCallback(() => {
    const params: string[] = []
    if (bundle) params.push(`bundle=${encodeURIComponent(bundle)}`)
    if (title) params.push(`title=${encodeURIComponent(title)}`)
    if (hideNavBar) params.push('hide_nav_bar=1')
    if (titleColor) params.push(`title_color=${encodeColor(titleColor)}`)
    if (titleColorDark) params.push(`title_color_dark=${encodeColor(titleColorDark)}`)
    if (titleColorLight) params.push(`title_color_light=${encodeColor(titleColorLight)}`)
    if (containerBgColor) params.push(`container_bg_color=${encodeColor(containerBgColor)}`)
    if (containerBgColorDark) params.push(`container_bg_color_dark=${encodeColor(containerBgColorDark)}`)
    if (containerBgColorLight) params.push(`container_bg_color_light=${encodeColor(containerBgColorLight)}`)
    if (navBarColor) params.push(`nav_bar_color=${encodeColor(navBarColor)}`)
    if (navBarColorDark) params.push(`nav_bar_color_dark=${encodeColor(navBarColorDark)}`)
    if (navBarColorLight) params.push(`nav_bar_color_light=${encodeColor(navBarColorLight)}`)
    if (forceThemeStyle) params.push(`force_theme_style=${forceThemeStyle}`)
    if (hideStatusBar) params.push('hide_status_bar=1')
    if (transStatusBar) params.push('trans_status_bar=1')
    if (hideLoading) params.push('hide_loading=1')
    if (hideError) params.push('hide_error=1')
    if (loadingBgColor) params.push(`loading_bg_color=${encodeColor(loadingBgColor)}`)
    return `hybrid://lynxview_page?${params.join('&')}`
  }, [bundle, title, hideNavBar, titleColor, titleColorDark, titleColorLight, containerBgColor, containerBgColorDark, containerBgColorLight, navBarColor, navBarColorDark, navBarColorLight, forceThemeStyle, hideStatusBar, transStatusBar, hideLoading, hideError, loadingBgColor])

  const schemeUrl = buildScheme()

  const handleOpen = () => {
    'background only'
    setResultCode(undefined)
    setResultMsg(undefined)
    open({ scheme: schemeUrl }, (res: { code: number; msg?: string }) => {
      setResultCode(res.code)
      setResultMsg(res.msg || (res.code === 0 ? 'Success' : 'Failed'))
    })
  }

  // Computed preview colors
  const previewNavBg = navBarColor || (isDark ? '#1c1c1e' : '#f8f8f8')
  const previewTitleColor = titleColor || (isDark ? '#ffffff' : '#000000')
  const previewContainerBg = containerBgColor || (isDark ? '#000000' : '#f0f2f5')
  const previewLoadingBg = loadingBgColor || previewContainerBg
  const previewContentColor = forceThemeStyle === 'light' ? 'rgba(0,0,0,0.12)' : 'rgba(255,255,255,0.12)'
  const previewContentColor2 = forceThemeStyle === 'light' ? 'rgba(0,0,0,0.08)' : 'rgba(255,255,255,0.08)'

  return (
    <DemoPage title="Scheme Builder">
      {/* Unified Page Preview */}
      <view className={dk('builder-section')}>
        <text className={dk('builder-section-title')}>Page Preview</text>

        <view className="preview-device">
          {/* Status Bar */}
          {!hideStatusBar ? (
            <view
              className="preview-status-bar"
              style={{
                backgroundColor: transStatusBar ? 'transparent' : previewNavBg,
              }}
            >
              <text className="preview-status-time" style={{ color: previewTitleColor }}>9:41</text>
              <view className="preview-status-icons">
                <text className="preview-status-icon" style={{ color: previewTitleColor }}>{'\u{1F4F6}'}</text>
                <text className="preview-status-icon" style={{ color: previewTitleColor }}>{'\u{1F50B}'}</text>
              </view>
            </view>
          ) : null}

          {/* Nav Bar */}
          {!hideNavBar ? (
            <view className="preview-nav-bar" style={{ backgroundColor: previewNavBg }}>
              <text className="preview-nav-back" style={{ color: previewTitleColor }}>{'\u2039'}</text>
              <text className="preview-nav-title" style={{ color: previewTitleColor }}>
                {title || 'Page Title'}
              </text>
              <view className="preview-nav-spacer" />
            </view>
          ) : null}

          {/* Content Area */}
          <view className="preview-content" style={{ backgroundColor: previewContainerBg }}>
            {/* Loading overlay */}
            {!hideLoading ? (
              <view className="preview-loading" style={{ backgroundColor: previewLoadingBg }}>
                <view className="preview-loading-spinner" />
                <text className="preview-loading-text" style={{ color: previewContentColor }}>Loading...</text>
              </view>
            ) : (
              <view className="preview-content-lines">
                <view className="preview-line" style={{ backgroundColor: previewContentColor, width: '70%' }} />
                <view className="preview-line" style={{ backgroundColor: previewContentColor2, width: '45%' }} />
                <view className="preview-line" style={{ backgroundColor: previewContentColor2, width: '60%' }} />
              </view>
            )}
          </view>

          {/* Theme badge */}
          {forceThemeStyle ? (
            <view className="preview-theme-badge">
              <text className="preview-theme-badge-text">{forceThemeStyle}</text>
            </view>
          ) : null}
        </view>
      </view>

      {/* Navigation Bar */}
      <view className={dk('builder-section')}>
        <text className={dk('builder-section-title')}>Navigation Bar</text>
        <FormField type="input" label="title" description="Navigation bar title text" value={title} placeholder="Page title" onInput={setTitle} />
        <FormField type="toggle" label="hide_nav_bar" description="Hide navigation bar" value={hideNavBar} onToggle={setHideNavBar} />
        <ColorInput label="title_color" description="Title text color" value={titleColor} placeholder="#ffffff" onInput={setTitleColor} quickColors={['#ffffff', '#000000', '#25f4ee', '#fe2c3a']} />
        <ColorInput label="nav_bar_color" description="Nav bar background" value={navBarColor} placeholder="#000000" onInput={setNavBarColor} quickColors={['#000000', '#1c1c1e', '#ffffff', '#007aff', '#009995']} />
      </view>

      {/* Container */}
      <view className={dk('builder-section')}>
        <text className={dk('builder-section-title')}>Container</text>
        <ColorInput label="container_bg_color" description="Background color" value={containerBgColor} placeholder="#000000" onInput={setContainerBgColor} />
      </view>

      {/* Theme */}
      <view className={dk('builder-section')}>
        <text className={dk('builder-section-title')}>Theme</text>
        <FormField type="picker" label="force_theme_style" description="SDK picks *_dark or *_light color variants" value={forceThemeStyle || 'auto'} options={['auto', 'light', 'dark']} onSelect={(v) => { 'background only'; setForceThemeStyle(v === 'auto' ? '' : v) }} />
        {forceThemeStyle === 'dark' ? (
          <>
            <ColorInput label="nav_bar_color_dark" description="Nav bar in dark mode" value={navBarColorDark} placeholder="#000000" onInput={setNavBarColorDark} quickColors={['#000000', '#1c1c1e', '#0a0a2e']} />
            <ColorInput label="title_color_dark" description="Title in dark mode" value={titleColorDark} placeholder="#ffffff" onInput={setTitleColorDark} quickColors={['#ffffff', '#25f4ee', '#cccccc']} />
            <ColorInput label="container_bg_color_dark" description="Background in dark mode" value={containerBgColorDark} placeholder="#000000" onInput={setContainerBgColorDark} quickColors={['#000000', '#1c1c1e', '#0a0a2e']} />
          </>
        ) : null}
        {forceThemeStyle === 'light' ? (
          <>
            <ColorInput label="nav_bar_color_light" description="Nav bar in light mode" value={navBarColorLight} placeholder="#ffffff" onInput={setNavBarColorLight} quickColors={['#ffffff', '#f0f2f5', '#f8f8f8']} />
            <ColorInput label="title_color_light" description="Title in light mode" value={titleColorLight} placeholder="#000000" onInput={setTitleColorLight} quickColors={['#000000', '#333333', '#009995']} />
            <ColorInput label="container_bg_color_light" description="Background in light mode" value={containerBgColorLight} placeholder="#f0f2f5" onInput={setContainerBgColorLight} quickColors={['#ffffff', '#f0f2f5', '#f8f8f8']} />
          </>
        ) : null}
      </view>

      {/* Status Bar */}
      <view className={dk('builder-section')}>
        <text className={dk('builder-section-title')}>Status Bar</text>
        <FormField type="toggle" label="hide_status_bar" description="Hide device status bar" value={hideStatusBar} onToggle={setHideStatusBar} />
        <FormField type="toggle" label="trans_status_bar" description="Transparent status bar" value={transStatusBar} onToggle={setTransStatusBar} />
      </view>

      {/* Loading & Error */}
      <view className={dk('builder-section')}>
        <text className={dk('builder-section-title')}>Loading & Error</text>
        <FormField type="toggle" label="hide_loading" description="Hide loading indicator" value={hideLoading} onToggle={setHideLoading} />
        <FormField type="toggle" label="hide_error" description="Hide error view" value={hideError} onToggle={setHideError} />
        <ColorInput label="loading_bg_color" description="Loading background" value={loadingBgColor} placeholder="#000000" onInput={setLoadingBgColor} />
      </view>

      {/* Target Page */}
      <view className={dk('builder-section')}>
        <text className={dk('builder-section-title')}>Target Page</text>
        <view className="builder-default-hint">
          <text className="builder-default-badge">DEFAULT</text>
          <text className={dk('builder-default-text')}>gp-container.lynx.bundle</text>
        </view>
        <FormField type="input" label="bundle" description="Change to open a different page" value={bundle} placeholder="e.g. main.lynx.bundle" onInput={setBundle} />
      </view>

      {/* Generated URL + Open */}
      <view className={dk('builder-section')}>
        <text className={dk('builder-section-title')}>Generated Scheme URL</text>
        <input
          value={schemeUrl}
          style={{
            fontSize: '13px',
            fontFamily: 'monospace',
            height: '40px',
            borderRadius: '8px',
            padding: '0 12px',
            backgroundColor: isDark ? '#2a2a2a' : '#f0f2f5',
          }}
          text-color={isDark ? '#25f4ee' : '#009995'}
        />
        <view className={dk('open-button')} bindtap={handleOpen}>
          <text className="open-button-text">Open</text>
        </view>
      </view>

      <ResultCard label="Response" code={resultCode} msg={resultMsg} />
    </DemoPage>
  )
}

function encodeColor(color: string): string {
  if (!color) return ''
  const c = color.startsWith('#') ? color : `#${color}`
  return c.replace('#', '%23')
}

export function App() {
  return (
    <ThemeProvider>
      <SchemeBuilderContent />
    </ThemeProvider>
  )
}
