# Sparkling Method SDK - TypeScript

The `sparkling-method` package provides the JS-side pipe for **JS ↔ native** method calls.
It is the foundation that all Sparkling method packages (router, storage, media, etc.) are built on.

## Install

```bash
npm install sparkling-method
```

## Quick start

```ts
import pipe from 'sparkling-method';

// Callback style
pipe.call('router.open', { scheme: 'hybrid://lynxview_page?bundle=main.lynx.bundle' }, (res) => {
  console.log(res.code, res.msg);
});

// Async style
const data = await pipe.callAsync('storage.getItem', { key: 'token' });
console.log(data);
```

## API

### `pipe.call(methodMap, params, callback, options?)`

Call a native method with a callback.

| Param | Type | Description |
| --- | --- | --- |
| `methodMap` | `string \| MethodMap` | Method name (e.g. `'router.open'`) or `{ module, method }` object |
| `params` | `TParams` | Request parameters passed to the native side |
| `callback` | `(response: unknown) => void` | Callback invoked with the native response |
| `options` | `Record<string, unknown>` | Reserved for future use |

Example:

```ts
import pipe from 'sparkling-method';

pipe.call('storage.setItem', { key: 'lang', data: 'en' }, (res) => {
  console.log(res);
});
```

### `pipe.callAsync(methodMap, params, options?, timeout?)`

Call a native method and return a `Promise`.

| Param | Type | Default | Description |
| --- | --- | --- | --- |
| `methodMap` | `string \| MethodMap` | - | Method name or `{ module, method }` object |
| `params` | `TParams` | - | Request parameters |
| `options` | `Record<string, unknown>` | `undefined` | Reserved for future use |
| `timeout` | `number` | `30000` | Timeout in milliseconds |

Returns `Promise<TResponse>` — resolves with `response.data` when `code === 1`, rejects otherwise.

Example:

```ts
import pipe from 'sparkling-method';

try {
  const data = await pipe.callAsync('storage.getItem', { key: 'lang' });
  console.log(data); // { data: 'en' }
} catch (err) {
  console.error(err); // { code: number, msg: string }
}
```

### `pipe.callWithTimeout(methodMap, params, timeout, options?)`

Convenience wrapper for `callAsync` with a custom timeout.

| Param | Type | Description |
| --- | --- | --- |
| `methodMap` | `string \| MethodMap` | Method name or `{ module, method }` object |
| `params` | `TParams` | Request parameters |
| `timeout` | `number` | Timeout in milliseconds |
| `options` | `Record<string, unknown>` | Reserved for future use |

Returns `Promise<TResponse>`.

### `pipe.on(eventName, callback)`

Subscribe to a native event.

| Param | Type | Description |
| --- | --- | --- |
| `eventName` | `string` | Event name to listen for |
| `callback` | `EventCallback` | `(event: unknown) => void` |

Returns the `EventCallback` for use with `off()`.

### `pipe.off(eventName, callback)`

Unsubscribe from a native event.

| Param | Type | Description |
| --- | --- | --- |
| `eventName` | `string` | Event name |
| `callback` | `EventCallback` | The callback previously passed to `on()` |

## Types

```ts
interface PipeResponse<T = unknown> {
  code: number;
  msg: string;
  data?: T;
}

interface PipeErrorResponse {
  code: number;
  msg: string;
  data?: undefined;
}

interface PipeCallOptions {
  timeout?: number;
  priority?: 'high' | 'normal' | 'low';
  retryCount?: number;
  retryDelay?: number;
}

type MethodMap = string | {
  module: string;
  method: string;
};

type EventCallback = (event: unknown) => void;
```

## Response codes

Sparkling Method response codes follow the Android/iOS native bridge convention:
`1` means success, `0` means the method ran but failed, and negative values are
bridge or method errors. See [Response codes](./response-codes.md) for details.

## Relationship with method packages

Sparkling method packages (`sparkling-navigation`, `sparkling-storage`, `sparkling-media`, etc.) are
thin wrappers around `sparkling-method`. They call `pipe.call()` under the hood with pre-defined
method names like `router.open`, `storage.setItem`, `media.chooseMedia`, etc.

You can use `sparkling-method` directly for:
- Calling custom native methods you've implemented
- Listening to native events
- Building your own method packages
