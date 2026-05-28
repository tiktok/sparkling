import { useState, useEffect } from '@lynx-js/react'
import { open, navigate } from '../../lib/navigation.js'
import { ThemeProvider, useTheme } from '../../lib/theme.js'
import { Navigator, type TabPage } from '../../components/Navigator/index.js'
// sparkling-method lacks "type": "module" → CJS/ESM interop mismatch with
// verbatimModuleSyntax. The bundler resolves the default export correctly at runtime.
import _pipe from 'sparkling-method'
const pipe = _pipe as unknown as { call: (method: string, params: unknown, callback: (v: unknown) => void) => void }

import { parseSchemeInput, buildSchemeInput } from '../../lib/schemeUrl.js'
import { getRecentUrls, addRecentUrl, clearRecentUrls } from '../../lib/recentHistory.js'
import './App.css'

interface DemoItem {
  title: string
  description: string
  bundle: string
  icon: string
}

interface Category {
  name: string
  icon: string
  color: string
  items: DemoItem[]
}

const CATEGORIES: Category[] = [
  {
    name: 'Scheme & Navigation',
    icon: '\u{1F9ED}',
    color: '#25f4ee',
    items: [
      { title: 'Navigation Stack', description: 'Push/pop multi-level page stack', bundle: 'nav-chain.lynx.bundle', icon: '\u{26D3}' },
      { title: 'Passing Data', description: 'Send custom params as queryItems', bundle: 'nav-basic.lynx.bundle', icon: '\u{27A1}' },
      { title: 'Presets', description: 'Pre-configured scheme examples', bundle: 'scheme-presets.lynx.bundle', icon: '\u{1F3A8}' },
      { title: 'Scheme Builder', description: 'Build and test any scheme configuration', bundle: 'scheme-builder.lynx.bundle', icon: '\u{1F527}' },
    ],
  },
  {
    name: 'GlobalProps',
    icon: '\u{1F4F1}',
    color: '#009995',
    items: [
      { title: 'Device & OS Info', description: 'OS, device, locale, SDK version, accessibility', bundle: 'gp-device.lynx.bundle', icon: '\u{1F4BB}' },
      { title: 'Screen & Safe Area', description: 'Screen dimensions, safe area, status bar', bundle: 'gp-screen.lynx.bundle', icon: '\u{1F4D0}' },
    ],
  },
  {
    name: 'Storage',
    icon: '\u{1F4BE}',
    color: '#ff9500',
    items: [
      { title: 'setItem / getItem', description: 'Persistent key-value storage with TTL', bundle: 'storage-demo.lynx.bundle', icon: '\u{1F5C4}' },
    ],
  },
  {
    name: 'Media',
    icon: '\u{1F3AC}',
    color: '#af52de',
    items: [
      { title: 'chooseMedia', description: 'Pick images and videos from album or camera', bundle: 'media-choose.lynx.bundle', icon: '\u{1F4F7}' },
      { title: 'Upload', description: 'Upload files and images to server', bundle: 'media-upload.lynx.bundle', icon: '\u{2B06}' },
      { title: 'Download & Save', description: 'Download files and save data URLs', bundle: 'media-download.lynx.bundle', icon: '\u{2B07}' },
    ],
  },
]

function HomePage(props: { showPage: boolean; topInset: number }) {
  const { resolved } = useTheme()
  const [source, setSource] = useState('')
  const [params, setParams] = useState<Record<string, string>>({})
  const [openResult, setOpenResult] = useState('')
  const [recentUrls, setRecentUrls] = useState<string[]>([])
  const isDark = resolved === 'dark'

  useEffect(() => {
    if (props.showPage) {
      getRecentUrls((urls) => setRecentUrls(urls))
    }
  }, [props.showPage])

  if (!props.showPage) return <></>

  const dk = (base: string) => `${base} ${isDark ? `${base}--dark` : `${base}--light`}`

  const displayInput = buildSchemeInput(source, params)
  const isFullscreen = params.hide_nav_bar === '1' && params.trans_status_bar === '1'
  const activeTheme = params.force_theme_style || ''

  const handleInput = (event: { detail: { value: string } }) => {
    'background only'
    const parsed = parseSchemeInput(event.detail.value.trim())
    setSource(parsed.source)
    setParams(parsed.params)
  }

  const handleFullscreenToggle = () => {
    'background only'
    const next = { ...params }
    if (isFullscreen) {
      delete next.hide_nav_bar
      delete next.trans_status_bar
    } else {
      next.hide_nav_bar = '1'
      next.trans_status_bar = '1'
    }
    setParams(next)
  }

  const handleThemeChip = (theme: string) => {
    'background only'
    const next = { ...params }
    if (activeTheme === theme) {
      // Toggle off
      delete next.force_theme_style
      delete next.nav_bar_color
      delete next.title_color
      delete next.container_bg_color
    } else if (theme === 'dark') {
      next.force_theme_style = 'dark'
      next.nav_bar_color = '#000000'
      next.title_color = '#ffffff'
      next.container_bg_color = '#000000'
    } else {
      next.force_theme_style = 'light'
      next.nav_bar_color = '#ffffff'
      next.title_color = '#000000'
      next.container_bg_color = '#f0f2f5'
    }
    setParams(next)
  }

  const handleGo = () => {
    'background only'
    const target = source || 'gp-screen.lynx.bundle'

    const lower = target.toLowerCase()
    const isScheme = lower.startsWith('hybrid://')
    const isHttp = lower.startsWith('http://') || lower.startsWith('https://')
    const isBundle = lower.endsWith('.lynx.bundle')

    if (!isScheme && !isHttp && !isBundle) {
      setOpenResult('Must be: *.lynx.bundle, hybrid://..., or http://...')
      return
    }

    setOpenResult('Opening...')
    const displayUrl = buildSchemeInput(target, params)
    addRecentUrl(displayUrl, (urls) => setRecentUrls(urls))

    if (isScheme) {
      open({ scheme: target }, (res) => {
        setOpenResult(`${res.code === 1 ? 'Opened' : 'Failed'}${res.msg && res.msg !== 'ok' ? ' — ' + res.msg : ''}`)
      })
    } else if (isHttp) {
      const qs = Object.entries(params).filter(([, v]) => v && v !== '0').map(([k, v]) => `${k}=${encodeURIComponent(v)}`).join('&')
      const scheme = `hybrid://lynxview_page?url=${encodeURIComponent(target)}${qs ? '&' + qs : ''}`
      open({ scheme }, (res) => {
        setOpenResult(`${res.code === 1 ? 'Opened' : 'Failed'}${res.msg && res.msg !== 'ok' ? ' — ' + res.msg : ''}`)
      })
    } else {
      navigate({ path: target, options: { params } }, (res) => {
        setOpenResult(`${res.code === 1 ? 'Opened' : 'Failed'}${res.msg && res.msg !== 'ok' ? ' — ' + res.msg : ''}`)
      })
    }
  }

  const handleOpenRecent = (url: string) => {
    'background only'
    const parsed = parseSchemeInput(url)
    setSource(parsed.source)
    setParams(parsed.params)
    // Trigger navigation immediately
    addRecentUrl(url, (urls) => setRecentUrls(urls))
    const lower = parsed.source.toLowerCase()
    if (lower.startsWith('hybrid://')) {
      open({ scheme: parsed.source }, () => {})
    } else if (lower.startsWith('http://') || lower.startsWith('https://')) {
      const qs = Object.entries(parsed.params).filter(([, v]) => v && v !== '0').map(([k, v]) => `${k}=${encodeURIComponent(v)}`).join('&')
      open({ scheme: `hybrid://lynxview_page?url=${encodeURIComponent(parsed.source)}${qs ? '&' + qs : ''}` }, () => {})
    } else {
      navigate({ path: parsed.source, options: { params: parsed.params } }, () => {})
    }
  }

  const handleClearRecent = () => {
    'background only'
    clearRecentUrls(() => setRecentUrls([]))
  }

  const handleItemTap = (bundle: string, title: string) => {
    'background only'
    // Pass the current resolved theme so sub-pages inherit the user's choice
    const dark = resolved === 'dark'
    navigate({
      path: bundle,
      options: {
        params: {
          title,
          hide_nav_bar: 0,
          container_bg_color: dark ? '#000000' : '#f0f2f5',
          nav_bar_color: dark ? '#000000' : '#ffffff',
          title_color: dark ? '#FFFFFF' : '#000000',
          loading_bg_color: dark ? '#000000' : '#f0f2f5',
          force_theme_style: resolved,
        },
      },
    }, () => {})
  }

  return (
    <scroll-view className="tab-content" scroll-orientation="vertical">
      <view className={`page ${isDark ? 'page--dark' : 'page--light'}`} style={{ paddingTop: `${props.topInset + 16}px` }}>
        {/* Header */}
        <view className="home-header">
          <image
            className="logo-image"
            src={require('../../assets/sparkling_icon.png')}
          />
          <view className="home-title-row">
            <text className={dk('home-title')}>Sparkling </text>
            <text className={dk('home-title')} style={{ color: '#F1204A' }}>Go</text>
          </view>
        </view>

        {/* Open Page Card */}
        <view className={dk('card')}>
          <view className="card-header-row">
            <text className="card-header-icon">{'\u{1F310}'}</text>
            <text className={dk('card-label')}>
              Open Page
            </text>
          </view>
          <view className="input-row">
            <input
              className={`url-input ${isDark ? 'url-input--dark' : 'url-input--light'}`}
              value={displayInput}
              bindinput={handleInput}
              placeholder="gp-screen.lynx.bundle"
              style={{ color: isDark ? '#ffffff' : '#000000' }}
            />
            <view className="go-button" bindtap={handleGo}>
              <text className="go-button-text">Go</text>
            </view>
          </view>
          <view className="toggle-row">
            <view className={`toggle-chip ${isFullscreen ? 'toggle-chip--active' : (isDark ? 'toggle-chip--dark' : 'toggle-chip--light')}`} bindtap={handleFullscreenToggle}>
              <text className={`toggle-chip-text ${isFullscreen ? 'toggle-chip-text--active' : (isDark ? 'toggle-chip-text--dark' : 'toggle-chip-text--light')}`}>Fullscreen</text>
            </view>
            <view className={`toggle-chip ${activeTheme === 'dark' ? 'toggle-chip--active' : (isDark ? 'toggle-chip--dark' : 'toggle-chip--light')}`} bindtap={() => handleThemeChip('dark')}>
              <text className={`toggle-chip-text ${activeTheme === 'dark' ? 'toggle-chip-text--active' : (isDark ? 'toggle-chip-text--dark' : 'toggle-chip-text--light')}`}>Dark</text>
            </view>
            <view className={`toggle-chip ${activeTheme === 'light' ? 'toggle-chip--active' : (isDark ? 'toggle-chip--dark' : 'toggle-chip--light')}`} bindtap={() => handleThemeChip('light')}>
              <text className={`toggle-chip-text ${activeTheme === 'light' ? 'toggle-chip-text--active' : (isDark ? 'toggle-chip-text--dark' : 'toggle-chip-text--light')}`}>Light</text>
            </view>
          </view>
          {openResult && openResult !== 'Opened' ? (
            <text className={`result-text ${isDark ? 'result-text--dark' : 'result-text--light'}`}>
              {openResult}
            </text>
          ) : null}
          {recentUrls.length > 0 ? (
            <view className={dk('recent-section')}>
              <view className="recent-header">
                <text className={dk('recent-title')}>Recents</text>
                <view bindtap={handleClearRecent}>
                  <text className="recent-clear">Clear</text>
                </view>
              </view>
              <scroll-view scroll-orientation="vertical" style={{ maxHeight: '140px' }}>
                {recentUrls.map((url) => (
                  <view key={url} className="recent-item" bindtap={() => handleOpenRecent(url)}>
                    <text className={dk('recent-url')}>{url}</text>
                    <text className={dk('recent-arrow')}>{'\u203A'}</text>
                  </view>
                ))}
              </scroll-view>
            </view>
          ) : null}
        </view>

        {/* Demo Categories (merged from Showcase) */}
        {CATEGORIES.map((category) => (
          <view key={category.name} className="category-section">
            <view className="category-header">
              <view className="category-icon-circle" style={{ backgroundColor: `${category.color}18` }}>
                <text className="category-icon">{category.icon}</text>
              </view>
              <text className={dk('category-name')} style={{ color: category.color }}>
                {category.name}
              </text>
              <view className="category-count-badge" style={{ backgroundColor: `${category.color}20` }}>
                <text className="category-count-text" style={{ color: category.color }}>
                  {category.items.length}
                </text>
              </view>
            </view>
            <view className={dk('category-card')}>
              {category.items.map((item, index) => (
                <view key={item.title}>
                  <view
                    className="menu-item"
                    bindtap={() => handleItemTap(item.bundle, item.title)}
                  >
                    <view className="menu-item-icon" style={{ backgroundColor: `${category.color}15` }}>
                      <text className="menu-item-icon-text">{item.icon}</text>
                    </view>
                    <view className="menu-item-content">
                      <text className={dk('menu-item-title')}>{item.title}</text>
                      <text className={dk('menu-item-desc')}>{item.description}</text>
                    </view>
                    <text className={dk('menu-item-arrow')}>{'\u203A'}</text>
                  </view>
                  {index < category.items.length - 1 ? (
                    <view className={dk('menu-item-divider')} />
                  ) : null}
                </view>
              ))}
            </view>
          </view>
        ))}

        {/* Version Footer */}
        <view className="home-footer">
          <text className={dk('home-footer-text')}>
            Unlocks Lynx at TikTok Scale
          </text>
        </view>
      </view>
    </scroll-view>
  )
}

declare const __DEV__: boolean

function getOrigin(url: string): string {
  const m = url.match(/^(https?:\/\/[^/]+)/)
  return m ? m[1] : ''
}

function SettingsPage(props: { showPage: boolean; topInset: number }) {
  const { preference, resolved, setPreference } = useTheme()
  const isDark = resolved === 'dark'
  const isDev = typeof __DEV__ !== 'undefined' && __DEV__

  // --- Dev server state ---
  const [devLoading, setDevLoading] = useState(true)
  const [devUrl, setDevUrl] = useState('')       // persisted URL from native
  const [devUrlInput, setDevUrlInput] = useState('')
  const [devStatus, setDevStatus] = useState<'idle' | 'saved' | 'save-error' | 'pipe-error'>('idle')
  const [devErrorMsg, setDevErrorMsg] = useState('')

  // Runtime bundle URL — injected by the native SDK when it loaded this page.
  // Available without any pipe call; tells us where we're ACTUALLY running from.
  const runtimeUrl = ((lynx.__globalProps || {}) as any)?.queryItems?.url as string || ''
  const isConnected = /^https?:\/\//.test(runtimeUrl)
  const runtimeOrigin = getOrigin(runtimeUrl)
  const persistedOrigin = getOrigin(devUrl)

  useEffect(() => {
    if (!props.showPage) {
      setDevLoading(true)
      return
    }
    if (!isDev) return
    setDevStatus('idle')
    setDevErrorMsg('')
    pipe.call('debugtool.getDevUrl', {}, (res: any) => {
      if (res?.code === 1) {
        const url = res?.data?.url || ''
        setDevUrl(url)
        setDevUrlInput(url)
      } else if (res?.code != null && res.code < 0) {
        setDevStatus('pipe-error')
        setDevErrorMsg(res?.msg || '')
      }
      setDevLoading(false)
    })
  }, [props.showPage])

  if (!props.showPage) return <></>

  const dk = (base: string) => `${base} ${isDark ? `${base}--dark` : `${base}--light`}`

  // --- Theme picker ---
  const themes: Array<{ label: string; value: 'Auto' | 'Light' | 'Dark'; icon: string }> = [
    { label: 'Auto', value: 'Auto', icon: '\u2728' },
    { label: 'Light', value: 'Light', icon: '\u2600' },
    { label: 'Dark', value: 'Dark', icon: '\u{1F319}' },
  ]

  const handleThemeSelect = (value: 'Auto' | 'Light' | 'Dark') => {
    'background only'
    setPreference(value)
  }

  // --- Dev URL validation & handlers ---
  const isValidUrl = /^https?:\/\/.+/.test(devUrlInput)
  const hasChanges = devUrlInput !== devUrl
  const canSave = isValidUrl && hasChanges
  const showValidation = devUrlInput.length > 0 && !isValidUrl
  const hasMismatch = isConnected && !!persistedOrigin && runtimeOrigin !== persistedOrigin && devStatus === 'idle'

  const handleDevUrlInput = (event: { detail: { value: string } }) => {
    'background only'
    setDevUrlInput(event.detail.value.trim())
    // Clear stale feedback when user edits
    if (devStatus === 'saved' || devStatus === 'save-error') {
      setDevStatus('idle')
    }
  }

  const handleDevUrlSave = () => {
    'background only'
    if (!canSave) return
    pipe.call('debugtool.setDevUrl', { url: devUrlInput }, (res: any) => {
      if (res?.code === 1) {
        setDevUrl(devUrlInput)
        setDevStatus('saved')
        setDevErrorMsg('')
      } else {
        setDevStatus('save-error')
        setDevErrorMsg(res?.msg || 'Unknown error. Ensure sparkling-debug-tool is up to date.')
      }
    })
  }

  // --- System info ---
  // @ts-ignore - SystemInfo is a Lynx global
  const sysInfo = typeof SystemInfo !== 'undefined' ? SystemInfo : {} as any

  const systemInfo = [
    { label: 'Lynx SDK Version', value: sysInfo.lynxSdkVersion || sysInfo.engineVersion || 'N/A' },
    { label: 'Platform', value: sysInfo.platform || 'N/A' },
    { label: 'OS Version', value: sysInfo.osVersion || 'N/A' },
    { label: 'Pixel Ratio', value: sysInfo.pixelRatio != null ? String(sysInfo.pixelRatio) : 'N/A' },
    { label: 'Screen Width', value: sysInfo.pixelWidth != null ? String(sysInfo.pixelWidth) + 'px' : 'N/A' },
    { label: 'Screen Height', value: sysInfo.pixelHeight != null ? String(sysInfo.pixelHeight) + 'px' : 'N/A' },
  ]

  return (
    <scroll-view className="tab-content" scroll-orientation="vertical">
      <view className={`page ${isDark ? 'page--dark' : 'page--light'}`} style={{ paddingTop: `${props.topInset + 16}px` }}>
        <text className={dk('page-title')}>Settings</text>

        {/* Theme Picker */}
        <view className={dk('card')}>
          <text className={dk('card-label')}>Appearance</text>
          <view className="theme-picker">
            {themes.map((t) => (
              <view
                key={t.value}
                className={`theme-option ${preference === t.value ? (isDark ? 'theme-option--active-dark' : 'theme-option--active-light') : (isDark ? 'theme-option--inactive-dark' : 'theme-option--inactive-light')}`}
                bindtap={() => handleThemeSelect(t.value)}
              >
                <text className="theme-option-icon">{t.icon}</text>
                <text className={`theme-option-label ${preference === t.value ? 'theme-option-label--active' : (isDark ? 'theme-option-label--dark' : 'theme-option-label--light')}`}>
                  {t.label}
                </text>
              </view>
            ))}
          </view>
        </view>

        {/* Dev Server (only in dev mode) */}
        {isDev ? (
          <view className={dk('card')}>
            <view className="card-header-row">
              <text className="card-header-icon">{'\u{1F6E0}'}</text>
              <text className={dk('card-label')}>Dev Server</text>
            </view>

            {/* ── Connection status ── */}
            <view className="dev-status-row">
              <view className={`dev-status-dot ${isConnected ? 'dev-status-dot--connected' : 'dev-status-dot--local'}`} />
              <text className={dk('dev-status-text')}>
                {isConnected ? 'Connected to dev server' : 'Running from local assets'}
              </text>
            </view>
            {isConnected ? (
              <text className={dk('dev-runtime-url')}>{runtimeOrigin}</text>
            ) : null}

            {/* ── URL editor (or loading / error state) ── */}
            {devLoading ? (
              <text className={dk('dev-hint')}>Loading configuration...</text>
            ) : devStatus === 'pipe-error' ? null
            : (
              <>
                <text className={dk('dev-field-label')}>Configured URL</text>
                <view className="input-row">
                  <input
                    className={dk('url-input')}
                    defaultValue={devUrlInput}
                    bindinput={handleDevUrlInput}
                    placeholder="http://192.168.1.100:5969/"
                    style={{ color: isDark ? '#ffffff' : '#000000' }}
                  />
                  <view
                    className={`dev-url-save ${canSave ? 'dev-url-save--active' : 'dev-url-save--disabled'}`}
                    bindtap={handleDevUrlSave}
                  >
                    <text className={`dev-url-save-text ${canSave ? '' : 'dev-url-save-text--disabled'}`}>Save</text>
                  </view>
                </view>

                {/* Inline validation */}
                {showValidation ? (
                  <text className="dev-hint dev-hint--error">Must start with http:// or https://</text>
                ) : null}

                {/* Save success */}
                {devStatus === 'saved' ? (
                  <view className="dev-banner dev-banner--success">
                    <text className={dk('dev-banner-text')}>
                      {'\u2713'}  Saved successfully.
                    </text>
                    <text className={dk('dev-banner-detail')}>
                      Restart the app to load bundles from the new server.
                    </text>
                  </view>
                ) : null}

                {/* Save error */}
                {devStatus === 'save-error' ? (
                  <view className="dev-banner dev-banner--error">
                    <text className={dk('dev-banner-text')}>
                      {'\u2717'}  Failed to save.
                    </text>
                    <text className={dk('dev-banner-detail')}>
                      {devErrorMsg}
                    </text>
                  </view>
                ) : null}

                {/* Mismatch: configured server ≠ active server */}
                {hasMismatch ? (
                  <view className="dev-banner dev-banner--info">
                    <text className={dk('dev-banner-text')}>
                      Configured server ({persistedOrigin}) differs from active connection ({runtimeOrigin}).
                    </text>
                    <text className={dk('dev-banner-detail')}>
                      Restart the app to connect to the configured server.
                    </text>
                  </view>
                ) : null}
              </>
            )}
          </view>
        ) : null}

        {/* System Info */}
        <view className={dk('card')}>
          <text className={dk('card-label')}>System Info</text>
          {systemInfo.map((item) => (
            <view key={item.label} className="info-row">
              <text className={dk('info-label')}>{item.label}</text>
              <text className={dk('info-value')}>{item.value}</text>
            </view>
          ))}
        </view>

        {/* Footer */}
        <view className="home-footer">
          <text className={dk('home-footer-text')}>
            Cross-platform © 2026 TikTok
          </text>
        </view>
      </view>
    </scroll-view>
  )
}

function MainContent() {
  const [activePage, setActivePage] = useState<TabPage>('home')
  const { resolved } = useTheme()
  const isDark = resolved === 'dark'

  // Read safe area insets directly from globalProps.
  // Only apply top padding when we're the root page (hide_nav_bar=1, no native nav bar).
  // When loaded as a sub-page with native nav bar, the native SPKViewController
  // already offsets the LynxView below the nav bar — applying topHeight again
  // would create double padding.
  const gp = (lynx.__globalProps || {}) as Record<string, any>
  const queryItems = (gp.queryItems || {}) as Record<string, string>
  const hasNativeNavBar = queryItems.hide_nav_bar !== '1'
  const topInset = hasNativeNavBar ? 0 : (Number(gp.topHeight) || 0)
  const bottomInset = Number(gp.bottomHeight) || 0

  return (
    <view
      className={`app-root ${isDark ? 'app-root--dark' : 'app-root--light'}`}
    >
      <HomePage showPage={activePage === 'home'} topInset={topInset} />
      <SettingsPage showPage={activePage === 'settings'} topInset={topInset} />
      <view style={{ paddingBottom: `${bottomInset}px`, backgroundColor: isDark ? '#1c1c1e' : '#ffffff' }}>
        <Navigator activePage={activePage} onNavigate={setActivePage} />
      </view>
    </view>
  )
}

export function App() {
  return (
    <ThemeProvider>
      <MainContent />
    </ThemeProvider>
  )
}
