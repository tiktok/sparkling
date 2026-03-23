import { useEffect } from '@lynx-js/react'
import { close } from 'sparkling-navigation'
import './App.css'

export function App(props: { onMounted?: () => void }) {
  useEffect(() => {
    props.onMounted?.()
  }, [])

  return (
    <view className="debug-tool-fallback">
      <view className="debug-tool-card">
        <text className="debug-tool-title">Debug tool switches</text>
        <text className="debug-tool-body">
          On device, open this route from the main playground menu (debugToolSwitch). The native app
          opens the Lynx debug switches panel instead of this bundle.
        </text>
        <view className="debug-tool-close" bindtap={() => close()}>
          <text className="debug-tool-close-label">Close</text>
        </view>
      </view>
    </view>
  )
}
