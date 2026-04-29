/**
 * Recent URL history backed by sparkling-storage.
 *
 * Stores up to MAX_ITEMS recently opened URLs as a JSON array.
 * Deduplicates on add (moves existing entry to front).
 */
import _pipe from 'sparkling-method'
const pipe = _pipe as unknown as {
  call: (method: string, params: unknown, callback: (res: any) => void) => void
}

const STORAGE_KEY = 'sparkling_recent_urls'
const MAX_ITEMS = 20

let memoryCache: string[] | null = null

export function getRecentUrls(callback: (urls: string[]) => void): void {
  if (memoryCache !== null) {
    callback(memoryCache)
    return
  }
  pipe.call('storage.getItem', { key: STORAGE_KEY }, (res: any) => {
    if (res?.code === 1 && res?.data?.data) {
      try {
        const parsed = JSON.parse(String(res.data.data))
        memoryCache = Array.isArray(parsed) ? parsed : []
      } catch {
        memoryCache = []
      }
    } else {
      memoryCache = []
    }
    callback(memoryCache)
  })
}

export function addRecentUrl(url: string, callback?: (urls: string[]) => void): void {
  const trimmed = url.trim()
  if (!trimmed) return

  const doAdd = (current: string[]) => {
    const filtered = current.filter((u) => u !== trimmed)
    const updated = [trimmed, ...filtered].slice(0, MAX_ITEMS)
    memoryCache = updated
    pipe.call('storage.setItem', { key: STORAGE_KEY, data: JSON.stringify(updated) }, () => {})
    callback?.(updated)
  }

  if (memoryCache !== null) {
    doAdd(memoryCache)
  } else {
    getRecentUrls((urls) => doAdd(urls))
  }
}

export function clearRecentUrls(callback?: () => void): void {
  memoryCache = []
  pipe.call('storage.setItem', { key: STORAGE_KEY, data: JSON.stringify([]) }, () => {
    callback?.()
  })
}
