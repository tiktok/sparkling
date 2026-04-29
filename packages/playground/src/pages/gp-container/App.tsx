import { close } from '../../lib/navigation.js'
import { ThemeProvider, useTheme } from '../../lib/theme.js'
import { DemoPage } from '../../components/DemoPage/index.js'

import './App.css'

const SCHEME_PARAMS = new Set([
  'title', 'hide_nav_bar', 'container_bg_color', 'nav_bar_color',
  'title_color', 'loading_bg_color', 'force_theme_style', 'hide_status_bar',
  'trans_status_bar', 'hide_loading', 'hide_error', 'url', 'bundle',
])

function NavTargetContent() {
  const { resolved } = useTheme()
  const isDark = resolved === 'dark'
  const dk = (base: string) => `${base} ${isDark ? `${base}--dark` : `${base}--light`}`

  const gp = (lynx.__globalProps || {}) as Record<string, any>
  const containerID: string = gp.containerID || ''
  const queryItems = (gp.queryItems || {}) as Record<string, string>

  const schemeKeys = Object.keys(queryItems).filter(k => SCHEME_PARAMS.has(k))
  const customKeys = Object.keys(queryItems).filter(k => !SCHEME_PARAMS.has(k))

  const renderRows = (keys: string[], highlight: boolean) => keys.map((key) => (
    <view className={dk('gpc-qi-row')} key={key}>
      <text className={highlight ? 'gpc-qi-key--highlight' : dk('gpc-qi-key')}>{key}</text>
      <text className={highlight ? 'gpc-qi-value--highlight' : dk('gpc-qi-value')}>{String(queryItems[key])}</text>
    </view>
  ))

  return (
    <DemoPage title="Navigation Target">
      {/* Container */}
      <view className={dk('gpc-section')}>
        <text className={dk('gpc-section-title')}>Container</text>
        <view className={dk('gpc-qi-row')}>
          <text className={dk('gpc-qi-key')}>containerID</text>
          <text className={dk('gpc-qi-value')} style={{ fontSize: '11px' }}>{containerID || 'N/A'}</text>
        </view>
      </view>

      {/* Scheme Params */}
      {schemeKeys.length > 0 ? (
        <view className={dk('gpc-section')}>
          <text className={dk('gpc-section-title')}>Scheme Params</text>
          <view className={dk('gpc-qi-list')}>
            {renderRows(schemeKeys, false)}
          </view>
        </view>
      ) : null}

      {/* Custom Params */}
      <view className={dk('gpc-section')}>
        <text className={dk('gpc-section-title')}>Custom Params</text>
        {customKeys.length > 0 ? (
          <view className={dk('gpc-qi-list')}>
            {renderRows(customKeys, true)}
          </view>
        ) : (
          <view className={dk('gpc-qi-empty')}>
            <text className={dk('gpc-qi-empty-text')}>
              No custom params. Navigate with custom key-value data to see them here.
            </text>
          </view>
        )}
      </view>

      {/* Close */}
      <view className={dk('gpc-section')}>
        <view
          className={dk('gpc-close-btn')}
          bindtap={() => { 'background only'; close() }}
        >
          <text className="gpc-close-text">Close Page</text>
        </view>
      </view>
    </DemoPage>
  )
}

export function App() {
  return (
    <ThemeProvider>
      <NavTargetContent />
    </ThemeProvider>
  )
}
