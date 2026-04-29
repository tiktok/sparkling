import { useTheme } from '../../lib/theme.js'
import './index.css'

export type TabPage = 'home' | 'settings'

interface NavigatorProps {
  activePage: TabPage
  onNavigate: (page: TabPage) => void
}

export function Navigator(props: NavigatorProps) {
  const { resolved } = useTheme()
  const isDark = resolved === 'dark'

  return (
    <view className={`navigator ${isDark ? 'navigator--dark' : 'navigator--light'}`}>
      <view
        className="nav-button"
        bindtap={() => props.onNavigate('home')}
      >
        <text
          className={`nav-icon ${props.activePage === 'home' ? 'nav-icon--active' : ''}`}
        >
          {'\u{1F3E0}'}
        </text>
        <text
          className={`nav-label ${props.activePage === 'home' ? 'nav-label--active' : ''}`}
        >
          Home
        </text>
      </view>
      <view
        className="nav-button"
        bindtap={() => props.onNavigate('settings')}
      >
        <text
          className={`nav-icon ${props.activePage === 'settings' ? 'nav-icon--active' : ''}`}
        >
          {'\u2699'}
        </text>
        <text
          className={`nav-label ${props.activePage === 'settings' ? 'nav-label--active' : ''}`}
        >
          Settings
        </text>
      </view>
    </view>
  )
}
