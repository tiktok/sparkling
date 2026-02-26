import { useCallback, useEffect, useState } from '@lynx-js/react'

import * as router from 'sparkling-navigation'

import './App.css'
import sparklingLogo from '../../assets/sparkling_icon.png'

export function App(props: { onMounted?: () => void }) {
  const [lastResult, setLastResult] = useState<string | null>(null)

  useEffect(() => {
    console.info('Hello, Sparkling template')
    props.onMounted?.()
  }, [props])

  const openSecondPage = useCallback(() => {
    router.navigate(
      {
        path: './second.lynx.bundle',
        options: {
          params: {
            title: 'Second Page',
            screen_orientation: 'portrait',
          },
        },
      },
      (result: router.NavigateResponse) => {
        setLastResult(JSON.stringify(result))
      }
    )
  }, [])

  return (
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
          <text className="label">Navigate</text>
          <view className="primary" bindtap={openSecondPage}>
            <text className="primary__text">Open second page</text>
            <text className="primary__icon">→</text>
          </view>
        </view>
      </view>

    </scroll-view>
  )
}
