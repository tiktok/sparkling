# `sparkling-storage`

Key-value storage helper APIs for Lynx/JS.

## Install

```bash
npm install sparkling-storage
```

## Exports

### `setItem(params, callback)`

Set an item in storage.

- **Request**: `{ key: string; data: any; biz?: string; validDuration?: number }`
- **Response**: `{ code: number; msg: string; data?: any }`

Example:

```ts
import { setItem } from 'sparkling-storage';

setItem(
  { key: 'token', data: 'abc123', biz: 'demo', validDuration: 3600 },
  (res) => {
    console.log(res.code, res.msg);
  }
);
```

### `getItem(params, callback)`

Get an item from storage.

- **Request**: `{ key: string; biz?: string }`
- **Response**: `{ code: number; msg: string; data?: { data?: any } }`

Example:

```ts
import { getItem } from 'sparkling-storage';

getItem({ key: 'token', biz: 'demo' }, (res) => {
  console.log(res.code, res.msg, res.data?.data);
});
```

## Native method names

This package calls:
- `storage.setItem`
- `storage.getItem`

Your host app must register native implementations for these methods. See
[Sparkling Method SDK](../sparkling-method-android.md) / [iOS](../sparkling-method-ios.md).

All methods use the shared [Sparkling Method response codes](../response-codes.md).
