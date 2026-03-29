# Scheme

In Sparkling, every page or container is identified and opened by a **scheme URL** — a `hybrid://` URI that tells the native layer _what_ to render and _how_ to configure it.

## Why schemes?

Sparkling is a hybrid framework: your UI is written in Lynx/JS, but each page lives inside a native container (a `UIViewController` on iOS, an `Activity`/`Fragment` on Android). The scheme URL is the contract between the two worlds — it lets JS code open native containers without knowing native APIs, and lets native code configure containers without knowing JS internals.

This design gives you:

- **Decoupled navigation** — JS only needs to know a bundle path, not platform-specific view controllers or intents.
- **Deep linking** — any scheme URL can be triggered from outside the app (push notifications, other apps, etc.).
- **Configurable containers** — navigation bar visibility, colors, orientation, and theme are all set via URL parameters at open time.

## Anatomy of a scheme URL

```
hybrid://lynxview_page?bundle=detail.lynx.bundle&title=Detail&hide_nav_bar=1
└─────┘ └────────────┘ └───────────────────────────────────────────────────┘
protocol     host                        query parameters
```

- **Protocol**: always `hybrid://`.
- **Host**: determines the container type. `lynxview_page` is the standard Lynx page container.
- **Query parameters**: configure the container. `bundle` is the only required parameter.

## Container types (hosts)

The host determines which native container is used to render the content. See [Containers](./containers.md) for details on each container mode.

| Host | Description |
| --- | --- |
| `lynxview_page` | Lynx page container (recommended) |
| `lynxview` | Legacy alias for `lynxview_page` |
| `lynxview_card` | Lynx card container (Android) |
| `webview` | WebView container |

## Common parameters

| Param | Type | Description |
| --- | --- | --- |
| `bundle` | `string` (required) | Bundle path to load |
| `title` | `string` | Navigation bar title |
| `hide_nav_bar` | `0` / `1` | Hide the navigation bar |
| `title_color` | `#RRGGBB` | Title text color |
| `container_bg_color` | `#RRGGBB` | Container background color |
| `nav_bar_color` | `#RRGGBB` | Navigation bar background color |
| `force_theme_style` | `light` / `dark` | Force a specific theme |
| `hide_status_bar` | `0` / `1` | Hide the status bar |
| `trans_status_bar` | `0` / `1` | Transparent status bar |
| `hide_loading` | `0` / `1` | Hide the loading indicator |
| `hide_error` | `0` / `1` | Hide the error view |

Any additional query parameters are passed through to the target page via [`lynx.__globalProps.queryItems`](/apis/global-props/Interface.GlobalProps#queryitems).

For the full parameter reference, see the [Scheme API doc](/apis/scheme).

## Usage from JS

You rarely need to construct scheme URLs by hand. The [`navigate()`](/apis/sparkling-methods/sparkling-navigation) function builds them for you. See [Multi-page Navigation](./multi-page-navigation.md) for common navigation patterns (passing data, closing pages, etc.).

```ts
import { navigate } from 'sparkling-navigation';

navigate(
  {
    path: 'pages/detail.lynx.bundle',
    options: {
      params: {
        title: 'Detail',
        hide_nav_bar: 1,
        itemId: '42',       // custom param → available in queryItems
      },
    },
  },
  (res) => console.log(res.code, res.msg)
);
```

If you need full control, you can use `open()` with a raw scheme:

```ts
import { open } from 'sparkling-navigation';

open(
  { scheme: 'hybrid://lynxview_page?bundle=detail.lynx.bundle&title=Detail&itemId=42' },
  (res) => console.log(res.code, res.msg)
);
```

## Usage from native code

On the native side, you open scheme URLs through the Sparkling SDK router:

**Android (Kotlin)**:
```kotlin
SparklingRouter.open(context, "hybrid://lynxview_page?bundle=detail.lynx.bundle&title=Detail")
```

**iOS (Swift)**:
```swift
SparklingRouter.open("hybrid://lynxview_page?bundle=detail.lynx.bundle&title=Detail")
```

## Encoding tips

- Always URL-encode parameter values. Use `URLSearchParams` in JS, `Uri.Builder` on Android, or `URLComponents` on iOS.
- Hex colors must encode `#` as `%23` — otherwise the browser treats everything after `#` as a URL fragment.
- Stick to 6-digit `#RRGGBB` colors. Android and iOS interpret 8-digit hex differently.
