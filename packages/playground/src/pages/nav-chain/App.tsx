import { useState } from '@lynx-js/react'
import { navigate, close } from '../../lib/navigation.js'
import { ThemeProvider, useTheme } from '../../lib/theme.js'
import { DemoPage } from '../../components/DemoPage/index.js'
import { ResultCard } from '../../components/ResultCard/index.js'

import './App.css'

function NavChainContent() {
  const { resolved } = useTheme()
  const isDark = resolved === 'dark'
  const dk = (base: string) => `${base} ${isDark ? `${base}--dark` : `${base}--light`}`

  const globalProps = lynx.__globalProps
  const queryItems: Record<string, string> = (globalProps as any)?.queryItems || {}
  const containerID: string = (globalProps as any)?.containerID || ''

  // Current depth in the page stack (1-based)
  const depth = Number(queryItems.depth) || 1
  const parentDepth = queryItems.from_depth || '(root)'

  const [navResult, setNavResult] = useState<{ code: number; msg: string } | undefined>(undefined)

  const handlePush = () => {
    'background only'
    navigate({
      path: 'nav-chain.lynx.bundle',
      options: {
        params: {
          title: `Stack Level ${depth + 1}`,
          hide_nav_bar: 0,
          container_bg_color: isDark ? '#000000' : '#f0f2f5',
          nav_bar_color: isDark ? '#000000' : '#ffffff',
          title_color: isDark ? '#FFFFFF' : '#000000',
          force_theme_style: resolved,
          depth: String(depth + 1),
          from_depth: String(depth),
        },
      },
    }, (res: { code: number; msg?: string }) => {
      setNavResult({ code: res.code, msg: res.msg || 'ok' })
    })
  }

  const handleClose = () => {
    'background only'
    close()
  }

  // Visual stack indicator — show dots for each level
  const stackDots = []
  for (let i = 1; i <= depth; i++) {
    stackDots.push(i)
  }

  return (
    <DemoPage title={`Stack Level ${depth}`}>
      {/* Stack Visualization */}
      <view className={dk('chain-section')}>
        <text className={dk('chain-section-title')}>Page Stack</text>
        <text className={dk('chain-section-desc')}>
          You are {depth} level{depth > 1 ? 's' : ''} deep. Tap "Push" to add another level. Tap "Pop" or the back button to go back.
        </text>

        <view className="stack-visual">
          {stackDots.map((i) => (
            <view
              key={i}
              className={`stack-dot ${i === depth ? 'stack-dot--current' : 'stack-dot--past'}`}
            >
              <text className={`stack-dot-text ${i === depth ? 'stack-dot-text--current' : 'stack-dot-text--past'}`}>
                {i}
              </text>
            </view>
          ))}
          <view className="stack-dot stack-dot--next">
            <text className="stack-dot-text stack-dot-text--next">?</text>
          </view>
        </view>
      </view>

      {/* Received Data */}
      <view className={dk('chain-section')}>
        <text className={dk('chain-section-title')}>Received Data</text>
        <view className={dk('chain-param-row')}>
          <text className={dk('chain-param-key')}>depth</text>
          <text className={dk('chain-param-value')}>{depth}</text>
        </view>
        <view className={dk('chain-param-row')}>
          <text className={dk('chain-param-key')}>from_depth</text>
          <text className={dk('chain-param-value')}>{parentDepth}</text>
        </view>
        <view className={dk('chain-param-row')}>
          <text className={dk('chain-param-key')}>containerID</text>
          <text className={dk('chain-param-value')} style={{ fontSize: '11px' }}>{containerID || 'N/A'}</text>
        </view>
      </view>

      {/* Push */}
      <view className={dk('chain-section')}>
        <text className={dk('chain-section-title')}>Push New Page</text>
        <text className={dk('chain-section-desc')}>
          Navigates to this same page with depth={depth + 1}. Each push adds to the native page stack.
        </text>
        <view className={isDark ? 'chain-button chain-button--primary-dark' : 'chain-button chain-button--primary-light'} bindtap={handlePush}>
          <text className="chain-button-text">Push Level {depth + 1}</text>
        </view>
        <ResultCard label="Response" code={navResult?.code} msg={navResult?.msg} />
      </view>

      {/* Pop */}
      <view className={dk('chain-section')}>
        <text className={dk('chain-section-title')}>Pop (Go Back)</text>
        <text className={dk('chain-section-desc')}>
          Calls close() to remove this page from the stack and return to level {depth - 1 || 'root'}.
        </text>
        <view className={isDark ? 'chain-button chain-button--secondary-dark' : 'chain-button chain-button--secondary-light'} bindtap={handleClose}>
          <text className={isDark ? 'chain-button-text chain-button-text--secondary-dark' : 'chain-button-text chain-button-text--secondary-light'}>
            Pop (Close)
          </text>
        </view>
      </view>
    </DemoPage>
  )
}

export function App() {
  return (
    <ThemeProvider>
      <NavChainContent />
    </ThemeProvider>
  )
}
