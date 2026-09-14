# Scheme

Sparkling 页面/容器通过 `hybrid://...` URL 打开。本文档定义了 **Android 和 iOS 双端通用**的统一 Scheme 格式与参数。

生成的 HarmonyOS Shell 支持全页 `lynxview_page` 导航，以及 `bundle`/`url`、`title`、
`hide_nav_bar`、`hide_back_button`、`title_color`、`nav_bar_color`、
`container_bg_color`、`force_theme_style`、`hide_status_bar`、`trans_status_bar` 和
`screen_orientation`。该 Shell 尚未实现卡片/WebView 容器以及本文中的加载/错误视图参数。

## Host（容器类型）

Sparkling 支持多种 `hybrid://` host：

- `hybrid://lynxview_page`：Lynx **页面容器**（**推荐**）
- `hybrid://lynxview`：`lynxview_page` 的历史别名（仍可用）
- `hybrid://lynxview_card`：Lynx **卡片容器**（目前主要用于 Android）
- `hybrid://webview`：WebView 容器

如无特别说明，下文示例均使用 `lynxview_page`。

## 格式

### Bundle 风格（推荐）

```
hybrid://lynxview_page?bundle=<bundlePath>[&title=<title>][&hide_nav_bar=1][&title_color=<color>][&container_bg_color=<color>][&force_theme_style=light|dark]
```

- `hybrid://lynxview_page`：Sparkling Lynx **页面容器**的 host 类型（推荐）。
- `bundle`：指向你在 App 内打包的 `.lynx.bundle` 文件。

## 编码规则

- 始终对参数值进行 **URL 编码**。
- 如果传入十六进制颜色，**`#` 必须编码为 `%23`**（否则会被当作 URL fragment）。
- 建议使用查询构建器（`URLSearchParams`、`Uri.Builder` 等）来构造 Scheme，而非手动拼接字符串。

带编码颜色的示例：

```
hybrid://lynxview_page?bundle=main.lynx.bundle&title=Home&title_color=%23000000&container_bg_color=%23ffffff
```

## 参数（跨平台）

以下参数在 **Android 和 iOS 双端**均保证生效。

| 参数 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- |
| `bundle` | `string`（必填） | - | 要加载的 Lynx bundle 路径/名称（必须存在于 App 资源中）。 |
| `title` | `string` | 平台默认值 | 容器标题（通常显示在导航栏中）。 |
| `hide_nav_bar` | `0`/`1` | `0` | 设为 `1` 时隐藏导航栏。 |
| `title_color` | `#RRGGBB`（编码后） | 平台默认值 | 标题文字颜色。仅使用 6 位 RGB；参见"颜色格式"。 |
| `container_bg_color` | `#RRGGBB`（编码后） | 平台默认值 | 容器背景颜色。仅使用 6 位 RGB；参见"颜色格式"。 |
| `force_theme_style` | `light` \| `dark` | 系统默认 | 强制容器级别的明/暗主题及主题相关属性。 |
| `nav_bar_color` | `#RRGGBB`（编码后） | 平台默认值 | 导航栏背景颜色。仅使用 6 位 RGB。 |
| `hide_status_bar` | `0`/`1` | `0` | 设为 `1` 时隐藏状态栏。 |
| `trans_status_bar` | `0`/`1` | `0` | 设为 `1` 时使用透明状态栏。 |
| `hide_loading` | `0`/`1` | `0` | 设为 `1` 时隐藏加载视图。 |
| `loading_bg_color` | `#RRGGBB`（编码后） | 平台默认值 | 加载视图背景颜色。仅使用 6 位 RGB。 |
| `hide_error` | `0`/`1` | `0` | 设为 `1` 时隐藏错误视图。 |

### 颜色格式（跨平台）

使用 **6 位 RGB** 十六进制颜色：`#RRGGBB`（在 URL 中将 `#` 编码为 `%23`）。

**不要**在 Scheme 中使用 8 位十六进制颜色表示透明度（Android 和 iOS 对 8 位十六进制的解析方式不同）。

## 示例

最简形式：

```
hybrid://lynxview_page?bundle=main.lynx.bundle
```

带标题：

```
hybrid://lynxview_page?bundle=main.lynx.bundle&title=Home
```

隐藏导航栏：

```
hybrid://lynxview_page?bundle=main.lynx.bundle&hide_nav_bar=1
```

强制暗色主题：

```
hybrid://lynxview_page?bundle=main.lynx.bundle&force_theme_style=dark
```
