# Scheme

在 Sparkling 中，每个页面或容器都通过一个 **scheme URL** 来标识和打开 —— 一个 `hybrid://` URI，用于告诉原生层_渲染什么_以及_如何配置_。

## 为什么使用 Scheme？

Sparkling 是一个混合框架：UI 用 Lynx/JS 编写，但每个页面都运行在原生容器中（iOS 上是 `UIViewController`，Android 上是 `Activity`/`Fragment`）。Scheme URL 是这两个世界之间的契约 —— 它让 JS 代码无需了解原生 API 就能打开原生容器，也让原生代码无需了解 JS 内部实现就能配置容器。

这种设计带来了：

- **解耦的导航** —— JS 只需要知道 bundle 路径，不需要了解平台特定的 view controller 或 intent。
- **深度链接** —— 任何 scheme URL 都可以从 App 外部触发（推送通知、其他应用等）。
- **可配置的容器** —— 导航栏可见性、颜色、屏幕方向和主题都通过 URL 参数在打开时设置。

## Scheme URL 的结构

```
hybrid://lynxview_page?bundle=detail.lynx.bundle&title=Detail&hide_nav_bar=1
└─────┘ └────────────┘ └───────────────────────────────────────────────────┘
  协议        host                          查询参数
```

- **协议**：始终为 `hybrid://`。
- **Host**：决定容器类型。`lynxview_page` 是标准的 Lynx 页面容器。
- **查询参数**：配置容器。`bundle` 是唯一的必填参数。

## 容器类型（Host）

Host 决定使用哪种原生容器来渲染内容。参见[容器](./containers.md)了解各容器模式的详情。

| Host | 描述 |
| --- | --- |
| `lynxview_page` | Lynx 页面容器（推荐） |
| `lynxview` | `lynxview_page` 的历史别名 |
| `lynxview_card` | Lynx 卡片容器（Android） |
| `webview` | WebView 容器 |

## 常用参数

| 参数 | 类型 | 描述 |
| --- | --- | --- |
| `bundle` | `string`（必填） | 要加载的 bundle 路径 |
| `title` | `string` | 导航栏标题 |
| `hide_nav_bar` | `0` / `1` | 隐藏导航栏 |
| `title_color` | `#RRGGBB` | 标题文字颜色 |
| `container_bg_color` | `#RRGGBB` | 容器背景颜色 |
| `nav_bar_color` | `#RRGGBB` | 导航栏背景颜色 |
| `force_theme_style` | `light` / `dark` | 强制指定主题 |
| `hide_status_bar` | `0` / `1` | 隐藏状态栏 |
| `trans_status_bar` | `0` / `1` | 透明状态栏 |
| `hide_loading` | `0` / `1` | 隐藏加载指示器 |
| `hide_error` | `0` / `1` | 隐藏错误视图 |

任何额外的查询参数都会通过 [`lynx.__globalProps.queryItems`](/apis/global-props/Interface.GlobalProps#queryitems) 传递到目标页面。

完整参数参考请查看 [Scheme API 文档](/apis/scheme)。

## 在 JS 中使用

通常不需要手动构建 scheme URL。[`navigate()`](/apis/sparkling-methods/sparkling-navigation) 函数会为你构建它们。参见[多页面导航](./multi-page-navigation.md)了解常见的导航模式（传递数据、关闭页面等）。

通常不需要手动构造 scheme URL。[`navigate()`](/apis/sparkling-methods/sparkling-navigation) 函数会自动构建：

```ts
import { navigate } from 'sparkling-navigation';

navigate(
  {
    path: 'pages/detail.lynx.bundle',
    options: {
      params: {
        title: '详情',
        hide_nav_bar: 1,
        itemId: '42',       // 自定义参数 → 可通过 queryItems 获取
      },
    },
  },
  (res) => console.log(res.code, res.msg)
);
```

如果需要完全控制，可以使用 `open()` 传入原始 scheme：

```ts
import { open } from 'sparkling-navigation';

open(
  { scheme: 'hybrid://lynxview_page?bundle=detail.lynx.bundle&title=Detail&itemId=42' },
  (res) => console.log(res.code, res.msg)
);
```

## 在原生代码中使用

在原生侧，通过 Sparkling SDK 路由器打开 scheme URL：

**Android (Kotlin)**：
```kotlin
SparklingRouter.open(context, "hybrid://lynxview_page?bundle=detail.lynx.bundle&title=Detail")
```

**iOS (Swift)**：
```swift
SparklingRouter.open("hybrid://lynxview_page?bundle=detail.lynx.bundle&title=Detail")
```

## 编码提示

- 始终对参数值进行 URL 编码。在 JS 中使用 `URLSearchParams`，Android 上使用 `Uri.Builder`，iOS 上使用 `URLComponents`。
- 十六进制颜色必须将 `#` 编码为 `%23` —— 否则浏览器会将 `#` 之后的内容当作 URL fragment。
- 使用 6 位 `#RRGGBB` 颜色。Android 和 iOS 对 8 位十六进制的解析方式不同。
