import { ThemeProvider, useTheme } from '../../lib/theme.js'
import { DemoPage } from '../../components/DemoPage/index.js'

import './App.css'

interface PropInfo {
  name: string
  type: string
  description: string
  platform?: string
}

const OS_PROPS: PropInfo[] = [
  {
    name: 'os',
    type: 'string',
    description: 'Operating system: "ios" or "android"',
  },
  {
    name: 'osVersion',
    type: 'string',
    description: 'OS version string (e.g. "17.4", "14")',
  },
]

const DEVICE_PROPS: PropInfo[] = [
  {
    name: 'deviceModel',
    type: 'string',
    description: 'Device model in lowercase (e.g. "iphone15,2", "pixel 8")',
  },
  {
    name: 'pixelRatio',
    type: 'number',
    description: 'Device pixel ratio (e.g. 2.0, 3.0)',
    platform: 'Android',
  },
  {
    name: 'isPad',
    type: 'number',
    description: '1 if tablet, 0 otherwise',
  },
  {
    name: 'isNotchScreen',
    type: 'boolean',
    description: 'Whether device has a screen notch or cutout',
  },
  {
    name: 'isIPhoneX',
    type: 'number',
    description: '1 if iPhone X-series or later, 0 otherwise',
    platform: 'iOS only',
  },
  {
    name: 'isIPhoneXMax',
    type: 'number',
    description: 'Identical to isIPhoneX in current implementations',
    platform: 'iOS only',
  },
]

const LOCALE_PROPS: PropInfo[] = [
  {
    name: 'language',
    type: 'string',
    description: 'Preferred language code from OS (e.g. "en-US", "zh-Hans")',
  },
  {
    name: 'appLanguage',
    type: 'string?',
    description: 'App-level language setting; host apps inject via setStableProps()',
  },
  {
    name: 'appLocale',
    type: 'string?',
    description: 'App-level locale setting; host apps inject via setStableProps()',
  },
]

const STATUS_PROPS: PropInfo[] = [
  {
    name: 'isLowPowerMode',
    type: 'number',
    description: '1 if battery-saver mode is active, 0 otherwise',
  },
  {
    name: 'isAppBackground',
    type: 'number',
    description: '1 if host app is in background, 0 if foreground',
  },
]

const RUNTIME_PROPS: PropInfo[] = [
  {
    name: 'lynxSdkVersion',
    type: 'string',
    description: 'Sparkling SDK version (SPK_version from native)',
  },
  {
    name: 'preferredTheme',
    type: 'string',
    description: 'Theme from force_theme_style: "light", "dark", or empty',
  },
  {
    name: 'accessibleMode',
    type: 'number',
    description: 'Bitmask for accessibility. Bit 0 = screen reader active',
  },
  {
    name: 'templateResData',
    type: 'string?',
    description: 'Optional pre-fetched data from native during template loading',
  },
]

function GpDeviceContent() {
  const { resolved } = useTheme()
  const isDark = resolved === 'dark'

  const dk = (base: string) => `${base} ${isDark ? `${base}--dark` : `${base}--light`}`

  // @ts-ignore - SystemInfo is a Lynx engine global; globalProps come from Sparkling host
  const sysInfo = typeof SystemInfo !== 'undefined' ? SystemInfo : {} as any
  const gp = (lynx.__globalProps || {}) as Record<string, any>
  // globalProps has all device info; SystemInfo is only needed for pixelRatio (iOS)
  const globalProps: Record<string, any> = {
    ...gp,
    pixelRatio: gp.pixelRatio ?? sysInfo.pixelRatio,
    isNotchScreen: gp.isNotchScreen ?? (gp.isIPhoneX === 1),
  }

  const formatValue = (val: any): string => {
    if (val === undefined) return 'undefined'
    if (val === null) return 'null'
    if (typeof val === 'boolean') return val ? 'true' : 'false'
    return String(val)
  }

  const getTypeBadgeClass = (type: string): string => {
    if (type.startsWith('string')) return 'gp-type-badge gp-type-badge--string'
    if (type === 'number') return 'gp-type-badge gp-type-badge--number'
    if (type === 'boolean') return 'gp-type-badge gp-type-badge--boolean'
    return 'gp-type-badge gp-type-badge--string'
  }

  const renderPropGroup = (title: string, props: PropInfo[]) => (
    <view className={dk('gp-section')}>
      <text className={dk('gp-section-title')}>{title}</text>

      {props.map((prop) => (
        <view className={dk('gp-prop-row')} key={prop.name}>
          <view className="gp-prop-header">
            <text className={dk('gp-prop-name')}>{prop.name}</text>
            <text className={getTypeBadgeClass(prop.type)}>{prop.type}</text>
            {prop.platform ? (
              <text className="gp-platform-badge">{prop.platform}</text>
            ) : null}
          </view>
          <text className={dk('gp-prop-desc')}>{prop.description}</text>
          <view className={dk('gp-prop-value-row')}>
            <text className={dk('gp-prop-value-label')}>Value:</text>
            <text className={dk('gp-prop-value')}>
              {formatValue(globalProps[prop.name])}
            </text>
          </view>
        </view>
      ))}
    </view>
  )

  return (
    <DemoPage title="Device & OS Info">
      {/* Intro */}
      <view className={dk('gp-section')}>
        <text className={dk('gp-section-title')}>GlobalProps — Device & OS</text>
        <text className={dk('gp-section-desc')}>
          Read device and OS information from lynx.__globalProps. These values are injected by the native container at initialization.
        </text>
      </view>

      {/* OS group */}
      {renderPropGroup('OS', OS_PROPS)}

      {/* Device group */}
      {renderPropGroup('Device', DEVICE_PROPS)}

      {/* Locale group */}
      {renderPropGroup('Locale', LOCALE_PROPS)}

      {/* Status group */}
      {renderPropGroup('Status', STATUS_PROPS)}

      {/* Runtime group */}
      {renderPropGroup('Runtime', RUNTIME_PROPS)}
    </DemoPage>
  )
}

export function App() {
  return (
    <ThemeProvider>
      <GpDeviceContent />
    </ThemeProvider>
  )
}
