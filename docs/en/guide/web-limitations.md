# Web Limitations

The web platform provides a convenient development and preview environment, but has differences from the native Android/iOS platforms. This page documents known limitations.

## Rendering

- **DOM-based rendering**: `@lynx-js/web-core` renders Lynx components as custom HTML elements in the DOM, rather than using a native view hierarchy. CSS behavior follows browser standards, which may differ from Lynx's native layout engine in edge cases.
- **Performance**: For complex UIs with many elements or animations, native rendering will generally be faster. The web platform is best suited for development previews, not performance-critical production use.

## Native Bridge

- **`NativeModules` is unavailable**: On web, the Lynx `NativeModules` global does not exist. All method calls must go through the [web handler registry](/guide/web-method-implementations).
- **Unregistered methods**: Methods without a web handler return error code `-3` (module not registered). Check which methods have web handlers in the [built-in methods table](/guide/web-method-implementations#built-in-web-methods).

## Storage

- **Not encrypted**: `localStorage` is not encrypted, unlike native secure storage options. Do not store sensitive data (tokens, credentials) via `storage.*` methods on web in production.
- **Size limit**: `localStorage` has a ~5 MB per-origin limit in most browsers. Native storage does not have this restriction.
- **Synchronous**: `localStorage` operations are synchronous and block the main thread. For large datasets, this can cause jank.

## Media

- **Camera access**: `media.chooseMedia` with a camera source requires HTTPS. On `http://localhost` during development, camera access may be allowed by the browser, but in production, HTTPS is required.
- **File downloads**: `media.downloadFile` uses `fetch()` and is subject to CORS restrictions. The target URL must include appropriate CORS headers, or the download will fail.
- **File type filtering**: Browser `<input type="file">` accept filters are hints — users can override them and select any file type.

## Network

- **CORS**: All `fetch()` calls from the browser are subject to Cross-Origin Resource Sharing (CORS) policies. APIs that work from native (which has no CORS) may fail from web without server-side CORS headers.
- **Cookies**: Cookie handling differs between browser and native HTTP clients. Session management may behave differently on web.

## Device APIs

The following native capabilities are not available on web unless the browser provides equivalent APIs with user permission:

- Geolocation (available via browser Geolocation API, requires HTTPS)
- Push notifications (available via browser Push API, requires service worker)
- Biometric authentication (not available)
- App-level deep linking (not applicable in browser context)
- Background processing (limited to service workers)

## Feature Detection

Write platform-aware code using error handling:

```ts
import { callAsync } from 'sparkling-method';

try {
  const result = await callAsync('nativeOnly.feature', params);
  // Use native result
} catch (e) {
  if (e.code === -2 || e.code === -3) {
    // Not available on this platform
    // Show alternative UI or skip the feature
  }
}
```

Error codes:
- `-2`: `NativeModules` not available (running on web without a handler)
- `-3`: Module not registered (no web handler for this method)
