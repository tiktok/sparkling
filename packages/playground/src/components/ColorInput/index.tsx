import { useTheme } from '../../lib/theme.js'
import './index.css'

interface ColorInputProps {
  label: string
  description?: string
  value: string
  placeholder?: string
  onInput: (value: string) => void
  quickColors?: string[]
}

const DEFAULT_QUICK_COLORS = ['#000000', '#1c1c1e', '#ffffff', '#f0f2f5', '#0a0a2e', '#009995']

export function ColorInput(props: ColorInputProps) {
  const { resolved } = useTheme()
  const isDark = resolved === 'dark'
  const dk = (base: string) => `${base} ${isDark ? `${base}--dark` : `${base}--light`}`
  const colors = props.quickColors || DEFAULT_QUICK_COLORS

  const handleInput = (e: { detail: { value: string } }) => {
    'background only'
    props.onInput(e.detail.value.trim())
  }

  const handleQuickColor = (color: string) => {
    'background only'
    props.onInput(color)
  }

  return (
    <view className="color-input-field">
      <text className={dk('color-input-label')}>{props.label}</text>
      {props.description ? (
        <text className={dk('color-input-desc')}>{props.description}</text>
      ) : null}
      <view className="color-input-row">
        <input
          className={dk('color-input-box')}
          value={props.value}
          bindinput={handleInput}
          placeholder={props.placeholder || '#000000'}
          style={{ color: isDark ? '#ffffff' : '#000000' }}
        />
        <view className="color-input-swatch" style={{ backgroundColor: props.value || '#888888' }} />
      </view>
      <view className="color-input-presets">
        {colors.map((c) => (
          <view
            key={c}
            className={`color-input-preset ${props.value === c ? 'color-input-preset--active' : ''}`}
            style={{ backgroundColor: c }}
            bindtap={() => handleQuickColor(c)}
          />
        ))}
      </view>
    </view>
  )
}
