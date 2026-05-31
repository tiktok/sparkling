import { useTheme } from '../../lib/theme.js'
import './index.css'

interface FormFieldBaseProps {
  label: string
  description?: string
}

interface FormFieldInputProps extends FormFieldBaseProps {
  type: 'input'
  value: string
  placeholder?: string
  onInput: (value: string) => void
}

interface FormFieldToggleProps extends FormFieldBaseProps {
  type: 'toggle'
  value: boolean
  onToggle: (value: boolean) => void
}

interface FormFieldPickerProps extends FormFieldBaseProps {
  type: 'picker'
  value: string
  options: string[]
  onSelect: (value: string) => void
}

export type FormFieldProps = FormFieldInputProps | FormFieldToggleProps | FormFieldPickerProps

export function FormField(props: FormFieldProps) {
  const { resolved } = useTheme()
  const isDark = resolved === 'dark'

  // Toggle uses a different layout (iOS-style: label left, switch right)
  if (props.type === 'toggle') {
    return (
      <view className="form-field-toggle-ios">
        <view className="form-field-toggle-ios-left">
          <text className={`form-field-label ${isDark ? 'form-field-label--dark' : 'form-field-label--light'}`}>
            {props.label}
          </text>
          {props.description ? (
            <text className={`form-field-desc ${isDark ? 'form-field-desc--dark' : 'form-field-desc--light'}`}>
              {props.description}
            </text>
          ) : null}
        </view>
        <view
          className={`form-field-toggle ${(props as FormFieldToggleProps).value ? 'form-field-toggle--on' : (isDark ? 'form-field-toggle--off-dark' : 'form-field-toggle--off-light')}`}
          bindtap={() => {
            'background only'
            const p = props as FormFieldToggleProps
            p.onToggle(!p.value)
          }}
        >
          <view className={`form-field-toggle-knob ${(props as FormFieldToggleProps).value ? 'form-field-toggle-knob--on' : 'form-field-toggle-knob--off'}`} />
        </view>
      </view>
    )
  }

  return (
    <view className="form-field">
      <view className="form-field-header">
        <text className={`form-field-label ${isDark ? 'form-field-label--dark' : 'form-field-label--light'}`}>
          {props.label}
        </text>
        {props.description ? (
          <text className={`form-field-desc ${isDark ? 'form-field-desc--dark' : 'form-field-desc--light'}`}>
            {props.description}
          </text>
        ) : null}
      </view>

      {props.type === 'input' ? (
        <input
          className={`form-field-input ${isDark ? 'form-field-input--dark' : 'form-field-input--light'}`}
          defaultValue={props.value}
          placeholder={props.placeholder || ''}
          style={{ color: isDark ? '#ffffff' : '#000000' }}
          bindinput={(e: { detail: { value: string } }) => {
            'background only'
            ;(props as FormFieldInputProps).onInput(e.detail.value)
          }}
        />
      ) : null}

      {props.type === 'picker' ? (
        <view className="form-field-picker">
          {(props as FormFieldPickerProps).options.map((option) => (
            <view
              key={option}
              className={`form-field-picker-option ${option === (props as FormFieldPickerProps).value ? (isDark ? 'form-field-picker-option--active-dark' : 'form-field-picker-option--active-light') : (isDark ? 'form-field-picker-option--inactive-dark' : 'form-field-picker-option--inactive-light')}`}
              bindtap={() => {
                'background only'
                ;(props as FormFieldPickerProps).onSelect(option)
              }}
            >
              <text
                className={`form-field-picker-text ${option === (props as FormFieldPickerProps).value ? 'form-field-picker-text--active' : (isDark ? 'form-field-picker-text--dark' : 'form-field-picker-text--light')}`}
              >
                {option}
              </text>
            </view>
          ))}
        </view>
      ) : null}
    </view>
  )
}
