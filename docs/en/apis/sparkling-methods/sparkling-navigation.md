# `sparkling-navigation`

Router helper APIs for opening/closing Sparkling pages from Lynx/JS.

## Install

```bash
npm install sparkling-navigation
```

## Exports

### `open(params, callback)`

Open a page/route by `scheme`.

- **Request**: `{ scheme: string; options?: OpenOptions }`
- **Response**: `{ code: number; msg: string }`

Example:

```ts
import { open } from 'sparkling-navigation';

open(
  { scheme: 'hybrid://lynxview_page?bundle=main.lynx.bundle&title=Home' },
  (res) => {
    console.log(res.code, res.msg);
  }
);
```

`OpenOptions`:
- `interceptor?: string`
- `extra?: object`

### `close(params?, callback?)`

Close the current page (or a specific container by ID).

Example:

```ts
import { close } from 'sparkling-navigation';

close(); // close current
```

### `navigate(params, callback)`

Build a `hybrid://...` scheme from a bundle path and optional params, then open it.

- **Request**: `{ path: string; options?: NavigateOptions; baseScheme?: string }`
- `path` must be a **relative bundle path**, e.g. `main.lynx.bundle` (not a full scheme).
- `baseScheme` defaults to `hybrid://lynxview_page` in the implementation.

Example:

```ts
import { navigate } from 'sparkling-navigation';

navigate(
  {
    path: 'main.lynx.bundle',
    options: {
      params: {
        title: 'Home',
        hide_nav_bar: 1,
      },
    },
  },
  (res) => {
    console.log(res.code, res.msg);
  }
);
```

`NavigateOptions`:
- `params?: object` – query params merged into the scheme
- `replace?: boolean`
- `replaceType?: 'alwaysCloseAfterOpen' | 'alwaysCloseBeforeOpen' | 'onlyCloseAfterOpenSucceed'`
- `interceptor?: string`
- `extra?: object`

See also:
- [sparkling-method](../sparkling-method-ts.md) – underlying pipe SDK
- [Response codes](../response-codes.md) – shared success and error code convention
