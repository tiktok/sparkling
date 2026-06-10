# `sparkling-navigation`

用于从 Lynx/JS 中打开和关闭 Sparkling 页面的路由辅助 API。

## 安装

```bash
npm install sparkling-navigation
```

## 导出

### `open(params, callback)`

通过 `scheme` 打开一个页面/路由。

- **请求**：`{ scheme: string; options?: OpenOptions }`
- **响应**：`{ code: number; msg: string }`

示例：

```ts
import { open } from 'sparkling-navigation';

open(
  { scheme: 'hybrid://lynxview_page?bundle=main.lynx.bundle&title=Home' },
  (res) => {
    console.log(res.code, res.msg);
  }
);
```

`OpenOptions`：
- `interceptor?: string`
- `extra?: object`

### `close(params?, callback?)`

关闭当前页面（或通过 ID 关闭指定容器）。

示例：

```ts
import { close } from 'sparkling-navigation';

close(); // 关闭当前页面
```

### `navigate(params, callback)`

根据 bundle 路径和可选参数构建 `hybrid://...` Scheme，然后打开。

- **请求**：`{ path: string; options?: NavigateOptions; baseScheme?: string }`
- `path` 必须是**相对 bundle 路径**，例如 `main.lynx.bundle`（而非完整 Scheme）。
- `baseScheme` 在实现中默认为 `hybrid://lynxview_page`。

示例：

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

说明：
- `navigate(...).options.params` 支持多种参数（如 `title`、`hide_nav_bar`、`container_bg_color` 等）。
  某个参数是否生效取决于原生端的支持情况。关于跨平台参数子集，请参阅 [Scheme](../scheme.md)。

## 原生方法名

此包调用以下方法：
- `router.open`
- `router.close`

你的宿主应用必须注册这些方法的原生实现。参阅
[Sparkling Method SDK](../sparkling-method-android.md) / [iOS](../sparkling-method-ios.md)。

所有方法都使用统一的 [Sparkling Method 响应码](../response-codes.md)。
