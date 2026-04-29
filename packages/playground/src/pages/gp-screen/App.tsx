import { ThemeProvider, useTheme } from '../../lib/theme.js'
import { DemoPage } from '../../components/DemoPage/index.js'
import './App.css'

interface PropInfo {
  name: string
  type: string
  description: string
  platform?: string
}

const SCREEN_PROPS: PropInfo[] = [
  {
    name: 'screenWidth',
    type: 'number',
    description: 'Screen width in logical pixels (dp/pt)',
  },
  {
    name: 'screenHeight',
    type: 'number',
    description: 'Screen height in logical pixels (dp/pt)',
  },
]

const BAR_PROPS: PropInfo[] = [
  {
    name: 'statusBarHeight',
    type: 'number',
    description: 'Height of the system status bar in px',
  },
  {
    name: 'navigationBarHeight',
    type: 'number',
    description: 'Height of the native navigation bar in px',
    platform: 'Android',
  },
]

const AREA_PROPS: PropInfo[] = [
  {
    name: 'safeAreaHeight',
    type: 'number',
    description: 'Combined safe area inset height (top + bottom)',
    platform: 'iOS',
  },
  {
    name: 'topHeight',
    type: 'number',
    description: 'Top safe area inset height (status bar + notch region)',
  },
  {
    name: 'bottomHeight',
    type: 'number',
    description: 'Bottom safe area inset height (home indicator region)',
  },
  {
    name: 'contentHeight',
    type: 'number',
    description: 'Available content height = screenHeight - topHeight - bottomHeight',
  },
]

const ORIENTATION_PROPS: PropInfo[] = [
  {
    name: 'screenOrientation',
    type: 'string',
    description: 'Current screen orientation: "portrait" or "landscape"',
  },
  {
    name: 'orientation',
    type: 'number',
    description: 'Orientation code: 0 = portrait, 1 = landscape left, 2 = portrait upside-down, 3 = landscape right',
  },
]

function GpScreenContent() {
  const { resolved } = useTheme()
  const isDark = resolved === 'dark'

  const dk = (base: string) => `${base} ${isDark ? `${base}--dark` : `${base}--light`}`

  // @ts-ignore - SystemInfo is a Lynx engine global; globalProps come from Sparkling host
  const sysInfo = typeof SystemInfo !== 'undefined' ? SystemInfo : {} as any
  const gp = (lynx.__globalProps || {}) as Record<string, any>
  // globalProps has all screen/safe-area values; SystemInfo adds physical pixel info
  const globalProps: Record<string, any> = {
    ...gp,
    pixelWidth: sysInfo.pixelWidth,
    pixelHeight: sysInfo.pixelHeight,
    pixelRatio: gp.pixelRatio ?? sysInfo.pixelRatio,
  }

  const formatValue = (val: any): string => {
    if (val === undefined) return 'undefined'
    if (val === null) return 'null'
    return String(val)
  }

  const numValue = (name: string): number => {
    const v = globalProps[name]
    return typeof v === 'number' ? v : 0
  }

  const getTypeBadgeClass = (type: string): string => {
    if (type.startsWith('string')) return 'gps-type-badge gps-type-badge--string'
    if (type === 'number') return 'gps-type-badge gps-type-badge--number'
    return 'gps-type-badge gps-type-badge--string'
  }

  const renderPropGroup = (title: string, props: PropInfo[]) => (
    <view className={dk('gps-section')}>
      <text className={dk('gps-section-title')}>{title}</text>

      {props.map((prop) => (
        <view className={dk('gps-prop-row')} key={prop.name}>
          <view className="gps-prop-header">
            <text className={dk('gps-prop-name')}>{prop.name}</text>
            <text className={getTypeBadgeClass(prop.type)}>{prop.type}</text>
            {prop.platform ? (
              <text className="gps-platform-badge">{prop.platform}</text>
            ) : null}
          </view>
          <text className={dk('gps-prop-desc')}>{prop.description}</text>
          <view className={dk('gps-prop-value-row')}>
            <text className={dk('gps-prop-value-label')}>Value:</text>
            <text className={dk('gps-prop-value')}>
              {formatValue(globalProps[prop.name])}
            </text>
          </view>
        </view>
      ))}
    </view>
  )

  // Diagram values
  const statusBarH = numValue('statusBarHeight')
  const navBarH = numValue('navigationBarHeight')
  const topH = numValue('topHeight')
  const bottomH = numValue('bottomHeight')
  const contentH = numValue('contentHeight')
  const screenH = numValue('screenHeight')
  const screenW = numValue('screenWidth')

  return (
    <DemoPage title="Screen & Safe Area">
      {/* Intro */}
      <view className={dk('gps-section')}>
        <text className={dk('gps-section-title')}>GlobalProps — Screen & Safe Area</text>
        <text className={dk('gps-section-desc')}>
          Screen dimensions, system bar heights, safe area insets, and orientation from lynx.__globalProps.
        </text>
      </view>

      {/* Visual Diagram */}
      <view className={dk('gps-section')}>
        <text className={dk('gps-section-title')}>Screen Layout Diagram</text>
        <text className={dk('gps-section-desc')}>
          Visual representation of the screen regions with live values ({screenW} x {screenH} px).
        </text>

        <view className={dk('gps-diagram')}>
          {/* Status Bar */}
          <view className="gps-diagram-block gps-diagram-block--status">
            <text className="gps-diagram-block-label">Status Bar</text>
            <text className="gps-diagram-block-value">{statusBarH}px</text>
          </view>

          {/* Top Safe Area (topHeight - statusBarHeight) */}
          <view className="gps-diagram-block gps-diagram-block--top-safe">
            <text className="gps-diagram-block-label">Top Safe Area</text>
            <text className="gps-diagram-block-value">topHeight: {topH}px</text>
          </view>

          {/* Content Area */}
          <view className="gps-diagram-block gps-diagram-block--content">
            <text className="gps-diagram-block-label">Content Area</text>
            <text className="gps-diagram-block-value">contentHeight: {contentH}px</text>
          </view>

          {/* Bottom Safe Area */}
          <view className="gps-diagram-block gps-diagram-block--bottom-safe">
            <text className="gps-diagram-block-label">Bottom Safe Area</text>
            <text className="gps-diagram-block-value">bottomHeight: {bottomH}px</text>
          </view>

          {/* Nav Bar (Android) */}
          <view className="gps-diagram-block gps-diagram-block--nav">
            <text className="gps-diagram-block-label">Nav Bar (Android)</text>
            <text className="gps-diagram-block-value">{navBarH}px</text>
          </view>

          {/* Screen dimension label */}
          <view className={dk('gps-diagram-dimension')}>
            <text className={dk('gps-diagram-dim-text')}>screenHeight: {screenH}px</text>
          </view>
        </view>

        {/* Diagram legend */}
        <view className="gps-legend">
          <view className="gps-legend-item">
            <view className="gps-legend-dot gps-legend-dot--status" />
            <text className={dk('gps-legend-text')}>Status Bar</text>
          </view>
          <view className="gps-legend-item">
            <view className="gps-legend-dot gps-legend-dot--top-safe" />
            <text className={dk('gps-legend-text')}>Top Safe Area</text>
          </view>
          <view className="gps-legend-item">
            <view className="gps-legend-dot gps-legend-dot--content" />
            <text className={dk('gps-legend-text')}>Content Area</text>
          </view>
          <view className="gps-legend-item">
            <view className="gps-legend-dot gps-legend-dot--bottom-safe" />
            <text className={dk('gps-legend-text')}>Bottom Safe Area</text>
          </view>
          <view className="gps-legend-item">
            <view className="gps-legend-dot gps-legend-dot--nav" />
            <text className={dk('gps-legend-text')}>Nav Bar</text>
          </view>
        </view>
      </view>

      {/* Screen Dimensions */}
      {renderPropGroup('Screen Dimensions', SCREEN_PROPS)}

      {/* System Bars */}
      {renderPropGroup('System Bars', BAR_PROPS)}

      {/* Safe Area / Layout */}
      {renderPropGroup('Safe Area & Layout', AREA_PROPS)}

      {/* Orientation */}
      {renderPropGroup('Orientation', ORIENTATION_PROPS)}

    </DemoPage>
  )
}

export function App() {
  return (
    <ThemeProvider>
      <GpScreenContent />
    </ThemeProvider>
  )
}
