# Scheme

Sparkling pages/containers are opened by a `hybrid://...` URL. This document defines the **unified scheme**
format and parameters that are **applied on both Android and iOS**.

## Hosts (container types)

Sparkling supports multiple `hybrid://` hosts:

- `hybrid://lynxview_page`: Lynx **page** container (**recommended**)
- `hybrid://lynxview`: legacy alias of `lynxview_page` (still supported)
- `hybrid://lynxview_card`: Lynx **card** container (currently Android)
- `hybrid://webview`: WebView container

Unless otherwise noted, examples below use `lynxview_page`.

## Format

### Bundle style (recommended)

```
hybrid://lynxview_page?bundle=<bundlePath>[&title=<title>][&hide_nav_bar=1][&title_color=<color>][&container_bg_color=<color>][&force_theme_style=light|dark]
```

- `hybrid://lynxview_page`: host type for Sparkling Lynx **page** containers (recommended).
- `bundle`: points to the `.lynx.bundle` you ship inside the app.

## Encoding rules

- Always **URL-encode** parameter values.
- If you pass hex colors, **`#` must be encoded as `%23`** (otherwise it becomes a URL fragment).
- Prefer building schemes with a query builder (`URLSearchParams`, `Uri.Builder`, etc.) instead of
  manual string concatenation.

Example with encoded colors:

```
hybrid://lynxview_page?bundle=main.lynx.bundle&title=Home&title_color=%23000000&container_bg_color=%23ffffff
```

## Parameters (cross-platform)

Only the following parameters are guaranteed to have an effect on **both Android and iOS**.

| Param | Type | Default | Meaning |
| --- | --- | --- | --- |
| `bundle` | `string` (required) | - | Lynx bundle path/name to load (must exist in app assets/resources). |
| `title` | `string` | platform default | Container title (usually shown in the navigation bar). |
| `hide_nav_bar` | `0`/`1` | `0` | Hide the navigation bar when set to `1`. |
| `title_color` | `#RRGGBB` (encoded) | platform default | Title text color. Use 6-digit RGB only; see “Color format”. |
| `container_bg_color` | `#RRGGBB` (encoded) | platform default | Container background color. Use 6-digit RGB only; see “Color format”. |
| `force_theme_style` | `light` \| `dark` | system default | Force light/dark theme for container-level theming and theme-dependent props. |
| `nav_bar_color` | `#RRGGBB` (encoded) | platform default | Navigation bar background color. Use 6-digit RGB only. |
| `hide_status_bar` | `0`/`1` | `0` | Hide the status bar when set to `1`. |
| `trans_status_bar` | `0`/`1` | `0` | Use transparent status bar when set to `1`. |
| `hide_loading` | `0`/`1` | `0` | Hide the loading view when set to `1`. |
| `loading_bg_color` | `#RRGGBB` (encoded) | platform default | Loading view background color. Use 6-digit RGB only. |
| `hide_error` | `0`/`1` | `0` | Hide the error view when set to `1`. |

## Android fixed Lynx viewport

Android accepts `width` and `height` as a pair of positive physical-pixel integers. When both are
valid, Sparkling creates the Lynx view with exact preset measure specs and hosts it at exactly that
size. This preserves the historical Lynx Explorer viewport semantic; it does not change the device
screen size or density.

| Param | Type | Default | Meaning |
| --- | --- | --- | --- |
| `width` | positive integer | container width | Fixed Lynx viewport width in physical pixels. |
| `height` | positive integer | container height | Fixed Lynx viewport height in physical pixels. |

The pair is atomic. If either parameter is missing, non-integer, zero, negative, or too large for an
Android measure spec, Sparkling ignores both and keeps the default full-size behavior.

A valid fixed viewport cannot be used with the effective Android
`SparklingThreadStrategy.MULTI_THREADS` rendering strategy. Sparkling resolves
the page strategy before the global default and rejects that unsafe
combination with a typed `SparklingLynxConfigurationException` before Lynx
view construction.

```
hybrid://lynxview_page?bundle=main.lynx.bundle&width=720&height=1280
```

### Color format (cross-platform)

Use **6-digit RGB** hex colors: `#RRGGBB` (encode `#` as `%23` in a URL).

Do **not** use 8-digit hex colors for transparency in the scheme (Android and iOS interpret 8-digit
hex differently).

## Examples

Minimal:

```
hybrid://lynxview_page?bundle=main.lynx.bundle
```

With title:

```
hybrid://lynxview_page?bundle=main.lynx.bundle&title=Home
```

Hide navigation bar:

```
hybrid://lynxview_page?bundle=main.lynx.bundle&hide_nav_bar=1
```

Force dark theme:

```
hybrid://lynxview_page?bundle=main.lynx.bundle&force_theme_style=dark
```


