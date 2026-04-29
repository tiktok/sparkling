import { useState, useCallback } from '@lynx-js/react'
import { setItem, getItem } from 'sparkling-storage'
import { ThemeProvider, useTheme } from '../../lib/theme.js'
import { DemoPage } from '../../components/DemoPage/index.js'
import { FormField } from '../../components/FormField/index.js'
import { ResultCard } from '../../components/ResultCard/index.js'

import './App.css'

interface LogEntry {
  id: number
  type: 'setItem' | 'getItem'
  request: Record<string, unknown>
  response?: { code?: number; msg?: string; data?: unknown }
  timestamp: string
}

function StorageDemoContent() {
  const { resolved } = useTheme()
  const isDark = resolved === 'dark'

  const dk = (base: string) => `${base} ${isDark ? `${base}--dark` : `${base}--light`}`

  // setItem fields
  const [setKey, setSetKey] = useState('tiktok')
  const [setData, setSetData] = useState('sparkling')
  const [setBiz, setSetBiz] = useState('')
  const [setValidDuration, setSetValidDuration] = useState('')

  // setItem result
  const [setResultCode, setSetResultCode] = useState<number | undefined>(undefined)
  const [setResultMsg, setSetResultMsg] = useState<string | undefined>(undefined)
  const [setResultData, setSetResultData] = useState<unknown>(undefined)

  // getItem fields
  const [getKey, setGetKey] = useState('tiktok')
  const [getBiz, setGetBiz] = useState('')

  // getItem result
  const [getResultCode, setGetResultCode] = useState<number | undefined>(undefined)
  const [getResultMsg, setGetResultMsg] = useState<string | undefined>(undefined)
  const [getResultData, setGetResultData] = useState<unknown>(undefined)

  // Operation log
  const [logs, setLogs] = useState<LogEntry[]>([])
  const [logId, setLogId] = useState(0)

  const addLog = useCallback(
    (entry: Omit<LogEntry, 'id' | 'timestamp'>) => {
      const now = new Date()
      const ts = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`
      const newId = logId + 1
      setLogId(newId)
      setLogs((prev) => [{ ...entry, id: newId, timestamp: ts }, ...prev].slice(0, 20))
    },
    [logId],
  )

  // setItem handler
  const handleSetItem = () => {
    'background only'
    setSetResultCode(undefined)
    setSetResultMsg(undefined)
    setSetResultData(undefined)

    const params: { key: string; data: string; biz?: string; validDuration?: number } = {
      key: setKey,
      data: setData,
    }
    if (setBiz) params.biz = setBiz
    if (setValidDuration) {
      const dur = Number(setValidDuration)
      if (!isNaN(dur) && dur > 0) params.validDuration = dur
    }

    const reqLog: Record<string, unknown> = { ...params }

    setItem(params, (res: { code: number; msg?: string; data?: unknown }) => {
      setSetResultCode(res.code)
      setSetResultMsg(res.msg || (res.code === 0 ? 'Success' : 'Failed'))
      setSetResultData(res.data)
      addLog({ type: 'setItem', request: reqLog, response: res })
    })
  }

  // getItem handler
  const handleGetItem = () => {
    'background only'
    setGetResultCode(undefined)
    setGetResultMsg(undefined)
    setGetResultData(undefined)

    const params: { key: string; biz?: string } = { key: getKey }
    if (getBiz) params.biz = getBiz

    const reqLog: Record<string, unknown> = { ...params }

    getItem(params, (res: { code: number; msg?: string; data?: { data?: unknown } }) => {
      setGetResultCode(res.code)
      setGetResultMsg(res.msg || (res.code === 0 ? 'Success' : 'Failed'))
      setGetResultData(res.data)
      addLog({ type: 'getItem', request: reqLog, response: res })
    })
  }

  const clearLogs = () => {
    'background only'
    setLogs([])
  }

  return (
    <DemoPage title="Storage">
      {/* setItem Panel */}
      <view className={dk('sto-section')}>
        <text className={dk('sto-section-title')}>setItem</text>
        <text className={dk('sto-section-desc')}>
          Store a key-value pair. Native method: storage.setItem
        </text>

        <view className="default-hint">
          <text className="default-badge">DEFAULTS</text>
          <text className={dk('default-text')}>key=tiktok, data=sparkling</text>
        </view>

        <FormField
          type="input"
          label="key"
          description="Storage key (required)"
          value={setKey}
          placeholder="e.g. username"
          onInput={setSetKey}
        />
        <FormField
          type="input"
          label="data"
          description="Value to store (required)"
          value={setData}
          placeholder='e.g. John or {"name":"John"}'
          onInput={setSetData}
        />
        <FormField
          type="input"
          label="biz"
          description="Business namespace (optional)"
          value={setBiz}
          placeholder="e.g. demo"
          onInput={setSetBiz}
        />
        <FormField
          type="input"
          label="validDuration"
          description="Expiration in seconds (optional)"
          value={setValidDuration}
          placeholder="e.g. 3600"
          onInput={setSetValidDuration}
        />

        <view className={dk('sto-btn')} bindtap={handleSetItem}>
          <text className="sto-btn-text">Save</text>
        </view>

        <ResultCard label="setItem Response" code={setResultCode} msg={setResultMsg} data={setResultData} />
      </view>

      {/* getItem Panel */}
      <view className={dk('sto-section')}>
        <text className={dk('sto-section-title')}>getItem</text>
        <text className={dk('sto-section-desc')}>
          Retrieve a stored value by key. Native method: storage.getItem
        </text>

        <view className="default-hint">
          <text className="default-badge">DEFAULT</text>
          <text className={dk('default-text')}>key=tiktok</text>
        </view>

        <FormField
          type="input"
          label="key"
          description="Storage key to read (required)"
          value={getKey}
          placeholder="e.g. username"
          onInput={setGetKey}
        />
        <FormField
          type="input"
          label="biz"
          description="Business namespace (optional)"
          value={getBiz}
          placeholder="e.g. demo"
          onInput={setGetBiz}
        />

        <view className={dk('sto-btn')} bindtap={handleGetItem}>
          <text className="sto-btn-text">Read</text>
        </view>

        <ResultCard label="getItem Response" code={getResultCode} msg={getResultMsg} data={getResultData} />
      </view>

      {/* Operation Log */}
      <view className={dk('sto-section')}>
        <view className="sto-log-header">
          <text className={dk('sto-section-title')}>Operation Log</text>
          {logs.length > 0 ? (
            <view className={dk('sto-clear-btn')} bindtap={clearLogs}>
              <text className={dk('sto-clear-btn-text')}>Clear</text>
            </view>
          ) : null}
        </view>
        <text className={dk('sto-section-desc')}>
          History of storage operations with request parameters and responses (most recent first).
        </text>

        {logs.length === 0 ? (
          <view className={dk('sto-log-empty')}>
            <text className={dk('sto-log-empty-text')}>
              No operations yet. Use setItem or getItem above to see the log.
            </text>
          </view>
        ) : null}

        {logs.map((log) => (
          <view className={dk('sto-log-entry')} key={String(log.id)}>
            <view className="sto-log-entry-header">
              <text className={log.type === 'setItem' ? 'sto-log-badge sto-log-badge--set' : 'sto-log-badge sto-log-badge--get'}>
                {log.type}
              </text>
              <text className={dk('sto-log-time')}>{log.timestamp}</text>
            </view>
            <text className={dk('sto-log-label')}>Request:</text>
            <text className={dk('sto-log-json')}>
              {JSON.stringify(log.request, null, 2)}
            </text>
            {log.response ? (
              <view className="sto-log-response">
                <text className={dk('sto-log-label')}>Response:</text>
                <text className={dk('sto-log-json')}>
                  {JSON.stringify(log.response, null, 2)}
                </text>
              </view>
            ) : null}
          </view>
        ))}
      </view>
    </DemoPage>
  )
}

export function App() {
  return (
    <ThemeProvider>
      <StorageDemoContent />
    </ThemeProvider>
  )
}
