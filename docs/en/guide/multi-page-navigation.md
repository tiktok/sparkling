# Multi-page Navigation

Sparkling uses a native [container](./containers.md) model where each page runs as an independent native container with its own LynxView. Navigation between pages is driven by [scheme URLs](./scheme.md).

Every entry you define in `source.entry` of your build config — whether that's the `lynxConfig` inside `app.config.ts` or a standalone `lynx.config.ts` — produces a bundle that can be navigated to. There is no route registration step: once a bundle is built, any page can navigate to it by its bundle path using the `navigate()` function.

## How it works

When you call `navigate()`, Sparkling:

1. Builds a [scheme URL](./scheme.md) (e.g. `hybrid://lynxview_page?bundle=detail.lynx.bundle&title=Detail`)
2. Hands the URL to the native layer, which opens a new [container](./containers.md)
3. The new container loads the target bundle and receives the URL's query parameters via `lynx.__globalProps.queryItems`

## Basic navigation

Use `navigate()` from `sparkling-navigation` to open another page by its bundle path:

```ts
import { navigate } from 'sparkling-navigation';

navigate(
  {
    path: 'pages/detail.lynx.bundle',
    options: {
      params: {
        title: 'Detail',
      },
    },
  },
  (res) => {
    console.log(res.code, res.msg);
  }
);
```

The `params` object supports all [predefined scheme parameters](./scheme.md#common-parameters) (`title`, `hide_nav_bar`, `screen_orientation`, etc.) as well as arbitrary custom keys.

## Passing custom data

Any key you add to `params` is appended to the scheme URL as a query parameter. On the target page, read it from `lynx.__globalProps.queryItems`:

**Page A — sender:**

```ts
import { navigate } from 'sparkling-navigation';

navigate(
  {
    path: 'pages/detail.lynx.bundle',
    options: {
      params: {
        title: 'Item Detail',
        itemId: '123',
        category: 'shoes',
      },
    },
  },
  (res) => {
    if (res.code !== 0) {
      console.error('Navigation failed:', res.msg);
    }
  }
);
```

**Page B — receiver:**

```ts
const globalProps = lynx.__globalProps;
const { itemId, category } = globalProps.queryItems;

console.log(itemId);   // "123"
console.log(category); // "shoes"
```

> All query-item values arrive as strings. Parse them if you need other types (e.g. `Number(itemId)`).

## Closing a page

Call `close()` to dismiss the current page and return to the previous one:

```ts
import { close } from 'sparkling-navigation';

close();
```

You can also close a specific container by ID:

```ts
close({ containerID: 'some-container-id' });
```

## Full example

Below is a minimal two-page app.

### `pages/home.tsx` — Home page

```tsx
import { navigate } from 'sparkling-navigation';

function goToDetail(id: string) {
  navigate(
    {
      path: 'pages/detail.lynx.bundle',
      options: {
        params: {
          title: 'Detail',
          itemId: id,
        },
      },
    },
    (res) => {
      if (res.code !== 0) {
        console.error('Failed to navigate:', res.msg);
      }
    }
  );
}

export function Home() {
  return <view onClick={() => goToDetail('42')}>Open Detail</view>;
}
```

### `pages/detail.tsx` — Detail page

```tsx
import { close } from 'sparkling-navigation';

export function Detail() {
  const { itemId } = lynx.__globalProps.queryItems;

  return (
    <view>
      <text>Item: {itemId}</text>
      <view onClick={() => close()}>Go back</view>
    </view>
  );
}
```
