import { useTheme } from '../../lib/theme.js'
import './index.css'

interface CodeBlockProps {
  label?: string
  content: string
}

export function CodeBlock(props: CodeBlockProps) {
  const { resolved } = useTheme()
  const isDark = resolved === 'dark'

  return (
    <view className="code-block-wrapper">
      {props.label ? (
        <text className={`code-block-label ${isDark ? 'code-block-label--dark' : 'code-block-label--light'}`}>
          {props.label}
        </text>
      ) : null}
      <view className={`code-block ${isDark ? 'code-block--dark' : 'code-block--light'}`}>
        <scroll-view scroll-orientation="horizontal" className="code-block-scroll">
          <text className={`code-block-text ${isDark ? 'code-block-text--dark' : 'code-block-text--light'}`}>
            {props.content}
          </text>
        </scroll-view>
      </view>
    </view>
  )
}
