import { useState } from '@lynx-js/react'
import { chooseMedia } from 'sparkling-media'
import { ThemeProvider, useTheme } from '../../lib/theme.js'
import { DemoPage } from '../../components/DemoPage/index.js'
import { FormField } from '../../components/FormField/index.js'
import { ResultCard } from '../../components/ResultCard/index.js'

import './App.css'

interface TempFile {
  tempFilePath?: string
  tempFileAbsolutePath?: string
  size?: number
  mediaType?: string
  mimeType?: string
  base64Data?: string
}

function MediaChooseContent() {
  const { resolved } = useTheme()
  const isDark = resolved === 'dark'

  const dk = (base: string) => `${base} ${isDark ? `${base}--dark` : `${base}--light`}`

  // Config
  const [selectImage, setSelectImage] = useState(true)
  const [selectVideo, setSelectVideo] = useState(false)
  const [sourceType, setSourceType] = useState<'album' | 'camera'>('album')
  const [maxCount, setMaxCount] = useState('1')

  // Result
  const [resultCode, setResultCode] = useState<number | undefined>(undefined)
  const [resultMsg, setResultMsg] = useState<string | undefined>(undefined)
  const [rawResponse, setRawResponse] = useState('')
  const [tempFiles, setTempFiles] = useState<TempFile[]>([])
  const [loading, setLoading] = useState(false)

  const toggleMediaImage = () => {
    'background only'
    setSelectImage(!selectImage)
  }
  const toggleMediaVideo = () => {
    'background only'
    setSelectVideo(!selectVideo)
  }
  const selectAlbum = () => {
    'background only'
    setSourceType('album')
  }
  const selectCamera = () => {
    'background only'
    setSourceType('camera')
  }

  const handleChooseMedia = () => {
    'background only'
    // Clear previous results
    setResultCode(undefined)
    setResultMsg(undefined)
    setTempFiles([])
    setLoading(true)

    const mediaTypes: string[] = []
    if (selectImage) mediaTypes.push('image')
    if (selectVideo) mediaTypes.push('video')

    if (mediaTypes.length === 0) {
      setResultCode(-1)
      setResultMsg('Select at least one media type')
      setLoading(false)
      return
    }

    const mc = Number(maxCount)
    const params: Record<string, unknown> = {
      mediaTypes,
      sourceType,
      maxCount: !isNaN(mc) && mc > 0 ? mc : 1,
    }

    chooseMedia(params as any, (res: any) => {
      setLoading(false)
      setRawResponse(JSON.stringify(res, null, 2))
      const code = res?.code ?? -1
      setResultCode(code)
      setResultMsg(res?.msg || (code === 0 || code === 1 ? 'Success' : 'Failed'))
      // tempFiles may be in res.data.tempFiles or res.tempFiles
      const files = res?.data?.tempFiles || res?.tempFiles || []
      if (files.length > 0) {
        setTempFiles(files)
      }
    })
  }

  const chipClass = (active: boolean) => {
    if (active) return isDark ? 'mc-chip mc-chip--active-dark' : 'mc-chip mc-chip--active-light'
    return isDark ? 'mc-chip mc-chip--inactive-dark' : 'mc-chip mc-chip--inactive-light'
  }
  const chipTextClass = (active: boolean) => {
    if (active) return isDark ? 'mc-chip-text mc-chip-text--active-dark' : 'mc-chip-text mc-chip-text--active-light'
    return isDark ? 'mc-chip-text mc-chip-text--inactive-dark' : 'mc-chip-text mc-chip-text--inactive-light'
  }

  const formatBytes = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  }

  return (
    <DemoPage title="chooseMedia">
      {/* Config Section */}
      <view className={dk('mc-section')}>
        <text className={dk('mc-section-title')}>Pick Media</text>

        {/* mediaTypes */}
        <text className={dk('mc-label')}>Media Type</text>
        <view className="mc-chip-row">
          <view className={chipClass(selectImage)} bindtap={toggleMediaImage}>
            <text className={chipTextClass(selectImage)}>Image</text>
          </view>
          <view className={chipClass(selectVideo)} bindtap={toggleMediaVideo}>
            <text className={chipTextClass(selectVideo)}>Video</text>
          </view>
        </view>

        {/* sourceType */}
        <text className={dk('mc-label')}>Source</text>
        <view className="mc-chip-row">
          <view className={chipClass(sourceType === 'album')} bindtap={selectAlbum}>
            <text className={chipTextClass(sourceType === 'album')}>Album</text>
          </view>
          <view className={chipClass(sourceType === 'camera')} bindtap={selectCamera}>
            <text className={chipTextClass(sourceType === 'camera')}>Camera</text>
          </view>
        </view>

        {/* maxCount */}
        <FormField
          type="input"
          label="Max Count"
          description="How many files to select"
          value={maxCount}
          placeholder="1"
          onInput={setMaxCount}
        />
      </view>

      {/* Action */}
      <view className={dk('mc-section')}>
        <view className={dk('mc-btn')} bindtap={handleChooseMedia}>
          <text className="mc-btn-text">{loading ? 'Selecting...' : 'Choose Media'}</text>
        </view>

        <ResultCard label="Response" code={resultCode} msg={resultMsg} />

        {/* Raw response for debugging */}
        {rawResponse ? (
          <view style={{ backgroundColor: '#111', borderRadius: 8, padding: 10, marginTop: 8 }}>
            <text style={{ fontSize: '11px', fontFamily: 'monospace', color: '#25f4ee' }}>{rawResponse}</text>
          </view>
        ) : null}
      </view>

      {/* Results — Selected Files */}
      {tempFiles.length > 0 ? (
        <view className={dk('mc-section')}>
          <text className={dk('mc-section-title')}>
            {String(tempFiles.length)} File{tempFiles.length > 1 ? 's' : ''} Selected
          </text>

          {tempFiles.map((file, i) => (
            <view className={dk('mc-file-card')} key={String(i)}>
              {/* Header with badges */}
              <view className="mc-file-header">
                <text className="mc-file-index">#{String(i + 1)}</text>
                {file.mediaType ? (
                  <text className="mc-file-badge">{file.mediaType}</text>
                ) : null}
                {file.mimeType ? (
                  <text className="mc-file-badge mc-file-badge--mime">{file.mimeType}</text>
                ) : null}
                {file.size !== undefined ? (
                  <text className="mc-file-badge mc-file-badge--size">{formatBytes(file.size)}</text>
                ) : null}
              </view>

              {/* Path */}
              {file.tempFilePath ? (
                <view className={dk('mc-file-path-box')}>
                  <text className={dk('mc-file-path-label')}>path</text>
                  <text className={dk('mc-file-path-value')}>{file.tempFilePath}</text>
                </view>
              ) : null}

              {/* Absolute path (if different) */}
              {file.tempFileAbsolutePath && file.tempFileAbsolutePath !== file.tempFilePath ? (
                <view className={dk('mc-file-path-box')}>
                  <text className={dk('mc-file-path-label')}>absolute</text>
                  <text className={dk('mc-file-path-value')}>{file.tempFileAbsolutePath}</text>
                </view>
              ) : null}

              {/* Image preview */}
              {file.tempFilePath ? (
                <image
                  src={file.tempFilePath}
                  style={{ width: '100%', height: 200, borderRadius: 8, marginTop: 8 }}
                  mode="aspectFit"
                />
              ) : null}

              {/* Base64 preview hint */}
              {file.base64Data ? (
                <view className={dk('mc-file-path-box')}>
                  <text className={dk('mc-file-path-label')}>base64</text>
                  <text className={dk('mc-file-path-value')}>
                    {String(file.base64Data.length)} chars
                  </text>
                </view>
              ) : null}
            </view>
          ))}
        </view>
      ) : null}

      {/* Empty state — only when no result yet */}
      {tempFiles.length === 0 && resultCode === undefined ? (
        <view className={dk('mc-section')}>
          <view className={dk('mc-empty')}>
            <text className={dk('mc-empty-text')}>
              Tap "Choose Media" to select files from your device.
            </text>
          </view>
        </view>
      ) : null}
    </DemoPage>
  )
}

export function App() {
  return (
    <ThemeProvider>
      <MediaChooseContent />
    </ThemeProvider>
  )
}
