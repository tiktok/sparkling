import { useState } from '@lynx-js/react'
import { downloadFile, saveDataURL } from 'sparkling-media'
import { ThemeProvider, useTheme } from '../../lib/theme.js'
import { DemoPage } from '../../components/DemoPage/index.js'
import { FormField } from '../../components/FormField/index.js'
import { ResultCard } from '../../components/ResultCard/index.js'

import './App.css'

type TabType = 'downloadFile' | 'saveDataURL'

interface DownloadResponse {
  code: number
  msg?: string
  data?: {
    httpCode?: number
    clientCode?: number
    header?: object
    filePath?: string
  }
}

interface SaveResponse {
  code: number
  msg?: string
  data?: {
    filePath?: string
  }
}

// A tiny 1x1 red PNG as a test data URL
const SAMPLE_DATA_URL = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg=='

function MediaDownloadContent() {
  const { resolved } = useTheme()
  const isDark = resolved === 'dark'

  const dk = (base: string) => `${base} ${isDark ? `${base}--dark` : `${base}--light`}`

  // Tab
  const [activeTab, setActiveTab] = useState<TabType>('downloadFile')

  // downloadFile state
  const [dlUrl, setDlUrl] = useState('')
  const [dlExtension, setDlExtension] = useState('png')
  const [dlResult, setDlResult] = useState<DownloadResponse | undefined>(undefined)
  const [dlLoading, setDlLoading] = useState(false)

  // saveDataURL state
  const [sdDataURL, setSdDataURL] = useState('')
  const [sdFilename, setSdFilename] = useState('test-image')
  const [sdExtension, setSdExtension] = useState('png')
  const [sdResult, setSdResult] = useState<SaveResponse | undefined>(undefined)
  const [sdLoading, setSdLoading] = useState(false)

  const selectDownloadFile = () => {
    'background only'
    setActiveTab('downloadFile')
  }
  const selectSaveDataURL = () => {
    'background only'
    setActiveTab('saveDataURL')
  }

  const handleDownloadFile = () => {
    'background only'
    setDlResult(undefined)
    setDlLoading(true)

    if (!dlUrl.trim()) {
      setDlResult({ code: -1, msg: 'URL is required' })
      setDlLoading(false)
      return
    }
    if (!dlExtension.trim()) {
      setDlResult({ code: -1, msg: 'Extension is required' })
      setDlLoading(false)
      return
    }

    downloadFile(
      { url: dlUrl.trim(), extension: dlExtension.trim() } as any,
      (res: DownloadResponse) => {
        setDlLoading(false)
        setDlResult(res)
      },
    )
  }

  const handleFillSample = () => {
    'background only'
    setSdDataURL(SAMPLE_DATA_URL)
  }

  const handleSaveDataURL = () => {
    'background only'
    setSdResult(undefined)
    setSdLoading(true)

    if (!sdDataURL.trim()) {
      setSdResult({ code: -1, msg: 'dataURL is required' })
      setSdLoading(false)
      return
    }
    if (!sdFilename.trim()) {
      setSdResult({ code: -1, msg: 'filename is required' })
      setSdLoading(false)
      return
    }
    if (!sdExtension.trim()) {
      setSdResult({ code: -1, msg: 'extension is required' })
      setSdLoading(false)
      return
    }

    saveDataURL(
      {
        dataURL: sdDataURL.trim(),
        filename: sdFilename.trim(),
        extension: sdExtension.trim(),
      } as any,
      (res: SaveResponse) => {
        setSdLoading(false)
        setSdResult(res)
      },
    )
  }

  const tabClass = (tab: TabType) => {
    const active = activeTab === tab
    if (active) return isDark ? 'md-tab md-tab--active-dark' : 'md-tab md-tab--active-light'
    return isDark ? 'md-tab md-tab--inactive-dark' : 'md-tab md-tab--inactive-light'
  }
  const tabTextClass = (tab: TabType) => {
    const active = activeTab === tab
    if (active) return isDark ? 'md-tab-text md-tab-text--active-dark' : 'md-tab-text md-tab-text--active-light'
    return isDark ? 'md-tab-text md-tab-text--inactive-dark' : 'md-tab-text md-tab-text--inactive-light'
  }

  const renderDownloadData = () => {
    if (!dlResult?.data) return null
    const d = dlResult.data
    const entries: { key: string; value: string }[] = []
    if (d.filePath !== undefined) entries.push({ key: 'filePath', value: String(d.filePath) })
    if (d.httpCode !== undefined) entries.push({ key: 'httpCode', value: String(d.httpCode) })
    if (d.clientCode !== undefined) entries.push({ key: 'clientCode', value: String(d.clientCode) })
    if (entries.length === 0) return null

    return (
      <view className={dk('md-data-section')}>
        <text className={dk('md-data-title')}>Response Data</text>
        {entries.map((entry) => (
          <view key={entry.key} className="md-data-row">
            <text className={dk('md-data-key')}>{entry.key}</text>
            <text className={dk('md-data-value')}>{entry.value}</text>
          </view>
        ))}
      </view>
    )
  }

  const renderSaveData = () => {
    if (!sdResult?.data?.filePath) return null

    return (
      <view className={dk('md-data-section')}>
        <text className={dk('md-data-title')}>Saved File</text>
        <view className="md-data-row">
          <text className={dk('md-data-key')}>filePath</text>
          <text className={dk('md-data-value')}>{String(sdResult.data.filePath)}</text>
        </view>
      </view>
    )
  }

  return (
    <DemoPage title="Download & Save">
      {/* Tab Switcher */}
      <view className={dk('md-section')}>
        <view className="md-tab-row">
          <view className={tabClass('downloadFile')} bindtap={selectDownloadFile}>
            <text className={tabTextClass('downloadFile')}>downloadFile</text>
          </view>
          <view className={tabClass('saveDataURL')} bindtap={selectSaveDataURL}>
            <text className={tabTextClass('saveDataURL')}>saveDataURL</text>
          </view>
        </view>
      </view>

      {/* downloadFile Tab */}
      {activeTab === 'downloadFile' ? (
        <view className={dk('md-section')}>
          <text className={dk('md-section-title')}>Download a File</text>
          <text className={dk('md-section-desc')}>
            Enter a URL to download. The file is saved locally and the path is returned.
          </text>

          <FormField
            type="input"
            label="URL"
            description="Remote file to download"
            value={dlUrl}
            placeholder="https://picsum.photos/200"
            onInput={setDlUrl}
          />

          <view className="default-hint">
            <text className="default-badge">DEFAULT</text>
            <text className={dk('default-text')}>extension=png</text>
          </view>
          <FormField
            type="input"
            label="Extension"
            description="File type (png, jpg, mp4, etc.)"
            value={dlExtension}
            placeholder="png"
            onInput={setDlExtension}
          />

          <view className={dk('md-btn')} bindtap={handleDownloadFile}>
            <text className="md-btn-text">{dlLoading ? 'Downloading...' : 'Download'}</text>
          </view>

          <ResultCard label="Response" code={dlResult?.code} msg={dlResult?.msg} />
          {renderDownloadData()}
        </view>
      ) : null}

      {/* saveDataURL Tab */}
      {activeTab === 'saveDataURL' ? (
        <view className={dk('md-section')}>
          <text className={dk('md-section-title')}>Save Base64 Data</text>
          <text className={dk('md-section-desc')}>
            Save a base64-encoded data URL to the local filesystem.
          </text>

          <FormField
            type="input"
            label="Data URL"
            description="Base64 data URL (data:image/png;base64,...)"
            value={sdDataURL}
            placeholder="data:image/png;base64,..."
            onInput={setSdDataURL}
          />

          <view className={dk('md-sample-btn')} bindtap={handleFillSample}>
            <text className={isDark ? 'md-sample-btn-text--dark' : 'md-sample-btn-text--light'}>
              Fill sample 1x1 PNG
            </text>
          </view>

          <view className="default-hint">
            <text className="default-badge">DEFAULTS</text>
            <text className={dk('default-text')}>filename=test-image, extension=png</text>
          </view>
          <FormField
            type="input"
            label="Filename"
            description="Name for the saved file (no extension)"
            value={sdFilename}
            placeholder="test-image"
            onInput={setSdFilename}
          />

          <FormField
            type="input"
            label="Extension"
            description="File type (png, jpg, etc.)"
            value={sdExtension}
            placeholder="png"
            onInput={setSdExtension}
          />

          <view className={dk('md-btn')} bindtap={handleSaveDataURL}>
            <text className="md-btn-text">{sdLoading ? 'Saving...' : 'Save to File'}</text>
          </view>

          <ResultCard label="Response" code={sdResult?.code} msg={sdResult?.msg} />
          {renderSaveData()}
        </view>
      ) : null}
    </DemoPage>
  )
}

export function App() {
  return (
    <ThemeProvider>
      <MediaDownloadContent />
    </ThemeProvider>
  )
}
