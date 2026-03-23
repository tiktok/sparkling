import { useCallback, useEffect, useState } from '@lynx-js/react'
import * as router from 'sparkling-navigation'

import { SafeAreaView } from '../../components/SafeAreaView.js'

import './App.css'
import sparklingLogo from '../../assets/sparkling_icon.png'

export function App(props: { onMounted?: () => void }) {
  const [scheme, setScheme] = useState('hybrid://lynxview_page?bundle=main.lynx.bundle&title=Sparkling')
  const [lastResult, setLastResult] = useState<string | null>(null)
  const secondPageScheme = 'hybrid://lynxview_page?bundle=second.lynx.bundle&title=Second%20Page&screen_orientation=portrait'

  useEffect(() => {
    console.info('Hello, Sparkling template')
    props.onMounted?.()
  }, [props])

  const openWithScheme = useCallback((targetScheme: string) => {
    router.open(
      { scheme: targetScheme },
      (result: router.OpenResponse) => {
        setLastResult(JSON.stringify(result))
      }
    )
  }, [])

  const openScheme = useCallback(() => {
    openWithScheme(scheme)
  }, [openWithScheme, scheme])

  const openSecondPage = useCallback(() => {
    openWithScheme(secondPageScheme)
  }, [openWithScheme, secondPageScheme])

  return (
    <SafeAreaView edges={['top', 'bottom']} style={{ flex: 1 }}>
    <scroll-view className="page-scroll" scroll-orientation="vertical">
      <view className="app">
        <view className="hero">
          <text className="eyebrow">Sparkling · LynxJS</text>
          <image src={sparklingLogo} className="logo" />
          <text className="title">Sparkling Starter</text>
        </view>
        <view className="card">
          <view className="card__header">
            <text className="card__title">Multi page demo</text>
            <text className="card__tag card__tag--outline">Router</text>
          </view>
          <text className="label">Scheme</text>
          <text className="pill pill--mono">{secondPageScheme}</text>
          <view className="primary" bindtap={openSecondPage}>
            <text className="primary__text">Open second page</text>
            <text className="primary__icon">→</text>
          </view>
        </view>
      </view>

    </scroll-view>
    </SafeAreaView>
  )
}
