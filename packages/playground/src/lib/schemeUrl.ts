/**
 * Utility functions for parsing and building Sparkling scheme URLs.
 *
 * Supports three input formats:
 * - Bundle name: "gp-device.lynx.bundle"
 * - Hybrid scheme: "hybrid://lynxview_page?bundle=gp-device.lynx.bundle&hide_nav_bar=1"
 * - HTTP URL: "http://localhost:5969/gp-device.lynx.bundle"
 */

/** Known scheme parameters that control the native container (not custom user params). */
export const SCHEME_PARAMS = [
  'title', 'hide_nav_bar', 'nav_bar_color', 'nav_bar_color_dark', 'nav_bar_color_light',
  'title_color', 'title_color_dark', 'title_color_light',
  'container_bg_color', 'container_bg_color_dark', 'container_bg_color_light',
  'force_theme_style', 'hide_status_bar', 'trans_status_bar',
  'hide_loading', 'hide_error', 'loading_bg_color', 'loading_bg_color_dark', 'loading_bg_color_light',
  'url', 'bundle',
] as const

export interface ParsedScheme {
  /** The bundle name or HTTP URL (the "source" of the page) */
  source: string
  /** Query parameters (excluding bundle/url which are in `source`) */
  params: Record<string, string>
}

/**
 * Parse any user input into a structured scheme representation.
 *
 * - "gp-device.lynx.bundle" → { source: "gp-device.lynx.bundle", params: {} }
 * - "hybrid://lynxview_page?bundle=foo&hide_nav_bar=1" → { source: "foo", params: { hide_nav_bar: "1" } }
 * - "http://localhost:5969/main.lynx.bundle" → { source: "http://...", params: {} }
 */
export function parseSchemeInput(input: string): ParsedScheme {
  const trimmed = input.trim()
  if (!trimmed) return { source: '', params: {} }

  // HTTP(S) URL — could be a dev server URL or a hybrid scheme with params
  const lowerInput = trimmed.toLowerCase()

  if (lowerInput.startsWith('hybrid://')) {
    return parseHybridUrl(trimmed)
  }

  if (lowerInput.startsWith('http://') || lowerInput.startsWith('https://')) {
    // Plain HTTP URL (dev server bundle URL) — no query params to parse
    return { source: trimmed, params: {} }
  }

  // Plain bundle name, possibly with params: "foo.lynx.bundle?hide_nav_bar=1"
  const qIdx = trimmed.indexOf('?')
  if (qIdx >= 0) {
    const source = trimmed.slice(0, qIdx)
    const params = parseQueryString(trimmed.slice(qIdx + 1))
    delete params.bundle
    delete params.url
    return { source, params }
  }

  return { source: trimmed, params: {} }
}

/**
 * Build a display string from source + params.
 * If params are empty, returns just the source (bundle name or URL).
 * If params exist, returns "source?key=val&key=val".
 */
export function buildSchemeInput(source: string, params: Record<string, string>): string {
  const filtered = Object.entries(params).filter(([, v]) => v !== '' && v !== '0')
  if (filtered.length === 0) return source
  const qs = filtered.map(([k, v]) => `${k}=${v}`).join('&')
  return `${source}?${qs}`
}

function parseHybridUrl(url: string): ParsedScheme {
  const qIdx = url.indexOf('?')
  if (qIdx < 0) return { source: '', params: {} }

  const params = parseQueryString(url.slice(qIdx + 1))

  // Extract the source (bundle or url param)
  let source = ''
  if (params.url) {
    source = decodeURIComponent(params.url)
    delete params.url
  } else if (params.bundle) {
    source = decodeURIComponent(params.bundle)
    delete params.bundle
  }

  return { source, params }
}

function parseQueryString(qs: string): Record<string, string> {
  const params: Record<string, string> = {}
  if (!qs) return params
  for (const part of qs.split('&')) {
    const eqIdx = part.indexOf('=')
    if (eqIdx < 0) {
      params[decodeURIComponent(part)] = '1'
    } else {
      const key = decodeURIComponent(part.slice(0, eqIdx))
      const val = decodeURIComponent(part.slice(eqIdx + 1))
      params[key] = val
    }
  }
  return params
}
