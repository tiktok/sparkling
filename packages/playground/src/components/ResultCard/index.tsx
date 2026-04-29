import { useTheme } from '../../lib/theme.js'
import './index.css'

interface ResultCardProps {
  label?: string
  code?: number | string
  msg?: string
  data?: any
}

export function ResultCard(props: ResultCardProps) {
  const { resolved } = useTheme()
  const isDark = resolved === 'dark'

  const hasResult = props.code !== undefined || props.msg !== undefined || props.data !== undefined

  if (!hasResult) return null

  const dataStr = props.data !== undefined
    ? (typeof props.data === 'string' ? props.data : JSON.stringify(props.data, null, 2))
    : null

  const isSuccess = props.code === 0 || props.code === '0'

  return (
    <view className={`result-card ${isDark ? 'result-card--dark' : 'result-card--light'}`}>
      {props.label ? (
        <text className={`result-card-label ${isDark ? 'result-card-label--dark' : 'result-card-label--light'}`}>
          {props.label}
        </text>
      ) : null}

      <view className="result-card-body">
        {props.code !== undefined ? (
          <view className="result-card-row">
            <text className={`result-card-key ${isDark ? 'result-card-key--dark' : 'result-card-key--light'}`}>
              code
            </text>
            <text className={`result-card-value ${isSuccess ? 'result-card-value--success' : 'result-card-value--error'}`}>
              {String(props.code)}
            </text>
          </view>
        ) : null}

        {props.msg !== undefined ? (
          <view className="result-card-row">
            <text className={`result-card-key ${isDark ? 'result-card-key--dark' : 'result-card-key--light'}`}>
              msg
            </text>
            <text className={`result-card-msg ${isDark ? 'result-card-msg--dark' : 'result-card-msg--light'}`}>
              {props.msg}
            </text>
          </view>
        ) : null}

        {dataStr !== null ? (
          <view className="result-card-data">
            <text className={`result-card-key ${isDark ? 'result-card-key--dark' : 'result-card-key--light'}`}>
              data
            </text>
            <scroll-view scroll-orientation="horizontal" className="result-card-data-scroll">
              <text className={`result-card-data-text ${isDark ? 'result-card-data-text--dark' : 'result-card-data-text--light'}`}>
                {dataStr}
              </text>
            </scroll-view>
          </view>
        ) : null}
      </view>
    </view>
  )
}
