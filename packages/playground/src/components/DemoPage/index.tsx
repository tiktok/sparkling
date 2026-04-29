import { useTheme } from '../../lib/theme.js'
import { SafeAreaView } from '../SafeAreaView.js'
import './index.css'

interface DemoPageProps {
  title: string
  children: any
}

export function DemoPage(props: DemoPageProps) {
  const { resolved } = useTheme()
  const isDark = resolved === 'dark'

  return (
    <SafeAreaView edges={['bottom']} style={{ flex: 1 }}>
      <view className={`demo-page ${isDark ? 'demo-page--dark' : 'demo-page--light'}`}>
        <scroll-view className="demo-scroll" scroll-orientation="vertical">
          <view className="demo-content">
            {props.children}
          </view>
        </scroll-view>
      </view>
    </SafeAreaView>
  )
}
