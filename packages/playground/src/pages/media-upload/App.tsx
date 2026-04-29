import { useState } from '@lynx-js/react'
import { uploadFile, uploadImage, chooseMedia } from 'sparkling-media'
import { ThemeProvider, useTheme } from '../../lib/theme.js'
import { DemoPage } from '../../components/DemoPage/index.js'
import { FormField } from '../../components/FormField/index.js'
import { ResultCard } from '../../components/ResultCard/index.js'

import './App.css'

type TabType = 'uploadFile' | 'uploadImage'

interface UploadResponse {
  code: number
  msg?: string
  data?: {
    url?: string
    uri?: string
    response?: object
    clientCode?: number
  }
}

function MediaUploadContent() {
  const { resolved } = useTheme()
  const isDark = resolved === 'dark'

  const dk = (base: string) => `${base} ${isDark ? `${base}--dark` : `${base}--light`}`

  // Tab
  const [activeTab, setActiveTab] = useState<TabType>('uploadFile')

  // uploadFile state
  const [fileUrl, setFileUrl] = useState('')
  const [fileFilePath, setFileFilePath] = useState('')
  const [fileChooseStatus, setFileChooseStatus] = useState('')
  const [fileResult, setFileResult] = useState<UploadResponse | undefined>(undefined)
  const [fileLoading, setFileLoading] = useState(false)

  // uploadImage state
  const [imageUrl, setImageUrl] = useState('')
  const [imageFilePath, setImageFilePath] = useState('')
  const [imageChooseStatus, setImageChooseStatus] = useState('')
  const [imageResult, setImageResult] = useState<UploadResponse | undefined>(undefined)
  const [imageLoading, setImageLoading] = useState(false)

  const selectUploadFile = () => {
    'background only'
    setActiveTab('uploadFile')
  }
  const selectUploadImage = () => {
    'background only'
    setActiveTab('uploadImage')
  }

  // Choose file for uploadFile tab
  const handleChooseForFile = () => {
    'background only'
    setFileChooseStatus('Opening picker...')
    chooseMedia(
      { mediaTypes: ['image', 'video'], sourceType: 'album', maxCount: 1 } as any,
      (res: { code: number; msg?: string; data?: { tempFiles?: { tempFilePath?: string; size?: number }[] } }) => {
        if (res.code === 0 && res.data?.tempFiles?.[0]?.tempFilePath) {
          const f = res.data.tempFiles[0]
          setFileFilePath(f.tempFilePath!)
          setFileChooseStatus('Selected: ' + f.tempFilePath!)
        } else {
          setFileChooseStatus(res.msg || 'Selection cancelled')
        }
      },
    )
  }

  // Choose image for uploadImage tab
  const handleChooseForImage = () => {
    'background only'
    setImageChooseStatus('Opening picker...')
    chooseMedia(
      { mediaTypes: ['image'], sourceType: 'album', maxCount: 1 } as any,
      (res: { code: number; msg?: string; data?: { tempFiles?: { tempFilePath?: string }[] } }) => {
        if (res.code === 0 && res.data?.tempFiles?.[0]?.tempFilePath) {
          setImageFilePath(res.data.tempFiles[0].tempFilePath!)
          setImageChooseStatus('Selected: ' + res.data.tempFiles[0].tempFilePath!)
        } else {
          setImageChooseStatus(res.msg || 'Selection cancelled')
        }
      },
    )
  }

  // Upload file
  const handleUploadFile = () => {
    'background only'
    setFileResult(undefined)
    setFileLoading(true)

    if (!fileUrl.trim()) {
      setFileResult({ code: -1, msg: 'URL is required' })
      setFileLoading(false)
      return
    }
    if (!fileFilePath.trim()) {
      setFileResult({ code: -1, msg: 'Choose a file first' })
      setFileLoading(false)
      return
    }

    uploadFile(
      { url: fileUrl.trim(), filePath: fileFilePath.trim() } as any,
      (res: UploadResponse) => {
        setFileLoading(false)
        setFileResult(res)
      },
    )
  }

  // Upload image
  const handleUploadImage = () => {
    'background only'
    setImageResult(undefined)
    setImageLoading(true)

    if (!imageUrl.trim()) {
      setImageResult({ code: -1, msg: 'URL is required' })
      setImageLoading(false)
      return
    }
    if (!imageFilePath.trim()) {
      setImageResult({ code: -1, msg: 'Choose an image first' })
      setImageLoading(false)
      return
    }

    uploadImage(
      { url: imageUrl.trim(), filePath: imageFilePath.trim() } as any,
      (res: UploadResponse) => {
        setImageLoading(false)
        setImageResult(res)
      },
    )
  }

  const tabClass = (tab: TabType) => {
    const active = activeTab === tab
    if (active) return isDark ? 'mu-tab mu-tab--active-dark' : 'mu-tab mu-tab--active-light'
    return isDark ? 'mu-tab mu-tab--inactive-dark' : 'mu-tab mu-tab--inactive-light'
  }
  const tabTextClass = (tab: TabType) => {
    const active = activeTab === tab
    if (active) return isDark ? 'mu-tab-text mu-tab-text--active-dark' : 'mu-tab-text mu-tab-text--active-light'
    return isDark ? 'mu-tab-text mu-tab-text--inactive-dark' : 'mu-tab-text mu-tab-text--inactive-light'
  }

  const renderResponseData = (result: UploadResponse | undefined) => {
    if (!result?.data) return null
    const d = result.data
    const entries: { key: string; value: string }[] = []
    if (d.url !== undefined) entries.push({ key: 'url', value: String(d.url) })
    if (d.uri !== undefined) entries.push({ key: 'uri', value: String(d.uri) })
    if (d.clientCode !== undefined) entries.push({ key: 'clientCode', value: String(d.clientCode) })
    if (d.response !== undefined) entries.push({ key: 'response', value: JSON.stringify(d.response, null, 2) })
    if (entries.length === 0) return null

    return (
      <view className={dk('mu-data-section')}>
        <text className={dk('mu-data-title')}>Response Data</text>
        {entries.map((entry) => (
          <view key={entry.key} className="mu-data-row">
            <text className={dk('mu-data-key')}>{entry.key}</text>
            <text className={dk('mu-data-value')}>{entry.value}</text>
          </view>
        ))}
      </view>
    )
  }

  return (
    <DemoPage title="Upload Media">
      {/* Tab Switcher */}
      <view className={dk('mu-section')}>
        <view className="mu-tab-row">
          <view className={tabClass('uploadFile')} bindtap={selectUploadFile}>
            <text className={tabTextClass('uploadFile')}>uploadFile</text>
          </view>
          <view className={tabClass('uploadImage')} bindtap={selectUploadImage}>
            <text className={tabTextClass('uploadImage')}>uploadImage</text>
          </view>
        </view>
      </view>

      {/* uploadFile Tab */}
      {activeTab === 'uploadFile' ? (
        <view className={dk('mu-section')}>
          <text className={dk('mu-section-title')}>Upload Any File</text>

          {/* Step 1: Choose file */}
          <view className={dk('mu-step')}>
            <text className={dk('mu-step-num')}>1</text>
            <text className={dk('mu-step-label')}>Choose a file</text>
          </view>
          <view className={dk('mu-choose-btn')} bindtap={handleChooseForFile}>
            <text className={isDark ? 'mu-choose-btn-text--dark' : 'mu-choose-btn-text--light'}>
              {fileFilePath ? 'Change File' : 'Choose File'}
            </text>
          </view>
          {fileChooseStatus ? (
            <text className={dk('mu-status-text')}>{fileChooseStatus}</text>
          ) : null}

          {/* Step 2: Enter URL */}
          <view className={dk('mu-step')}>
            <text className={dk('mu-step-num')}>2</text>
            <text className={dk('mu-step-label')}>Enter upload endpoint</text>
          </view>
          <FormField
            type="input"
            label="Upload URL"
            value={fileUrl}
            placeholder="https://httpbin.org/post"
            onInput={setFileUrl}
          />

          {/* Step 3: Upload */}
          <view className={dk('mu-step')}>
            <text className={dk('mu-step-num')}>3</text>
            <text className={dk('mu-step-label')}>Upload</text>
          </view>
          <view className={dk('mu-btn')} bindtap={handleUploadFile}>
            <text className="mu-btn-text">{fileLoading ? 'Uploading...' : 'Upload File'}</text>
          </view>

          <ResultCard label="Response" code={fileResult?.code} msg={fileResult?.msg} />
          {renderResponseData(fileResult)}
        </view>
      ) : null}

      {/* uploadImage Tab */}
      {activeTab === 'uploadImage' ? (
        <view className={dk('mu-section')}>
          <text className={dk('mu-section-title')}>Upload Image</text>

          {/* Step 1: Choose image */}
          <view className={dk('mu-step')}>
            <text className={dk('mu-step-num')}>1</text>
            <text className={dk('mu-step-label')}>Choose an image</text>
          </view>
          <view className={dk('mu-choose-btn')} bindtap={handleChooseForImage}>
            <text className={isDark ? 'mu-choose-btn-text--dark' : 'mu-choose-btn-text--light'}>
              {imageFilePath ? 'Change Image' : 'Choose Image'}
            </text>
          </view>
          {imageChooseStatus ? (
            <text className={dk('mu-status-text')}>{imageChooseStatus}</text>
          ) : null}

          {/* Step 2: Enter URL */}
          <view className={dk('mu-step')}>
            <text className={dk('mu-step-num')}>2</text>
            <text className={dk('mu-step-label')}>Enter upload endpoint</text>
          </view>
          <FormField
            type="input"
            label="Upload URL"
            value={imageUrl}
            placeholder="https://httpbin.org/post"
            onInput={setImageUrl}
          />

          {/* Step 3: Upload */}
          <view className={dk('mu-step')}>
            <text className={dk('mu-step-num')}>3</text>
            <text className={dk('mu-step-label')}>Upload</text>
          </view>
          <view className={dk('mu-btn')} bindtap={handleUploadImage}>
            <text className="mu-btn-text">{imageLoading ? 'Uploading...' : 'Upload Image'}</text>
          </view>

          <ResultCard label="Response" code={imageResult?.code} msg={imageResult?.msg} />
          {renderResponseData(imageResult)}
        </view>
      ) : null}
    </DemoPage>
  )
}

export function App() {
  return (
    <ThemeProvider>
      <MediaUploadContent />
    </ThemeProvider>
  )
}
