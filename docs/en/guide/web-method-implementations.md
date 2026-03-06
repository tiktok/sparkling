# Web Method Implementations

Sparkling method SDKs (navigation, storage, media) communicate with the native layer via `NativeModules` on Android/iOS. On web, `NativeModules` is unavailable — instead, a **web handler registry** dispatches method calls to browser-native implementations.

## How It Works

```
App code → LynxPipe.call('router.open', params)
  ↓
sparkling-method detects web environment
  ↓
Web handler registry lookup by method name
  ↓
Browser-native implementation (History API, localStorage, etc.)
```

When `LynxPipe.call()` runs on the web:

1. It checks if `NativeModules` is available — on web, it isn't.
2. Before returning an error, it looks up the method name in the **web handler registry**.
3. If a handler is registered, it's invoked with the same params and callback.
4. If no handler is found, it returns error code `-3` (module not registered).

## Built-in Web Methods

### Navigation (`sparkling-navigation`)

| Method | Web Implementation | Notes |
|--------|-------------------|-------|
| `router.open` | Parses the `hybrid://` scheme, extracts the bundle name, uses the History API and fires a `CustomEvent` to swap the `<lynx-view>` URL | Full scheme parameter support |
| `router.close` | `window.history.back()` | |

### Storage (`sparkling-storage`)

| Method | Web Implementation | Notes |
|--------|-------------------|-------|
| `storage.getItem` | `localStorage.getItem()` | 5 MB per-origin limit |
| `storage.setItem` | `localStorage.setItem()` | |
| `storage.removeItem` | `localStorage.removeItem()` | |

### Media (`sparkling-media`)

| Method | Web Implementation | Notes |
|--------|-------------------|-------|
| `media.chooseMedia` | `<input type="file">` with accept filters | Camera source requires HTTPS |
| `media.downloadFile` | `fetch()` + `URL.createObjectURL()` + download link | Subject to CORS restrictions |

## Creating a Web Handler for a Custom Method

If you've created a custom method SDK (see [Create a Custom Method](/guide/get-started/create-custom-method)), you can add web support by following these steps:

### 1. Create the web handler

Create `src/web/index.ts` in your method package:

```ts
import { registerWebMethod } from 'sparkling-method';

registerWebMethod('myModule.myAction', (params, callback) => {
  // Implement using browser APIs
  const result = { /* ... */ };
  callback({ code: 0, data: result });
});

registerWebMethod('myModule.anotherAction', (params, callback) => {
  // ...
  callback({ code: 0, data: {} });
});
```

### 2. Add subpath export

In your method package's `package.json`, add a `./web` export:

```json
{
  "exports": {
    ".": "./dist/index.js",
    "./web": "./dist/web/index.js"
  }
}
```

### 3. Update module.config.json

Add `"web"` to the `platforms` array:

```json
{
  "name": "my-method",
  "platforms": ["android", "ios", "web"],
  "web": {
    "entryPoint": "./src/web/index.ts",
    "subpath": "./web"
  }
}
```

### 4. Run autolink

```bash
pnpm autolink
```

The CLI discovers the `web` platform entry and generates the appropriate imports so your web handler is loaded automatically.

## Testing Web Handlers

Web handlers are plain functions — you can test them in isolation:

```ts
import { getWebMethodHandler } from 'sparkling-method';

// After importing your web handler module
import 'my-method/web';

test('myModule.myAction returns expected data', (done) => {
  const handler = getWebMethodHandler('myModule.myAction');
  handler({ input: 'test' }, (response) => {
    expect(response.code).toBe(0);
    expect(response.data).toEqual({ /* expected */ });
    done();
  });
});
```

## Error Handling

If a method is called on web without a registered handler, `LynxPipe` returns:

```json
{ "code": -3, "message": "Module not registered" }
```

You can use this to implement graceful degradation:

```ts
import { callAsync } from 'sparkling-method';

try {
  const result = await callAsync('myModule.doThing', params);
} catch (e) {
  if (e.code === -3) {
    // Method not available on this platform — show fallback UI
  }
}
```
