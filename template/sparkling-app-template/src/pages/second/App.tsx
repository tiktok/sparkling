import { useCallback, useEffect } from '@lynx-js/react'

import { close } from 'sparkling-navigation'

import { SafeAreaView } from '../../components/SafeAreaView.js'

import './App.css'

export function App(props: { onMounted?: () => void }) {

  useEffect(() => {
    console.info('Hello, Sparkling second page')
    console.info('lynx.__globalProps', lynx.__globalProps)
    props.onMounted?.()
  }, [props])

  const onClose = useCallback(() => {
    close()
  }, [])

  

  return (
    // Native nav consumes top safe area; Lynx only needs bottom inset here.
    <SafeAreaView edges={['bottom']} style={{ flex: 1 }} className="page">
      <view className="App">
        <view className="Banner">
          <text className="Title">This is the second page</text>
        </view>
        <view className="Content">
          <text className="Button" bindtap={onClose}>
            Close
          </text>
        </view>
      </view>
    </SafeAreaView>
  )
}
