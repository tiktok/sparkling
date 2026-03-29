# 多页面导航

Sparkling 采用原生[容器](./containers.md)模型，每个页面运行在独立的原生容器及其对应的 LynxView 中。页面之间通过 [scheme URL](./scheme.md) 驱动导航。

在构建配置的 `source.entry` 中定义的每个入口——无论是 `app.config.ts` 中的 `lynxConfig` 还是独立的 `lynx.config.ts`——都会生成一个可导航的 bundle。无需路由注册：bundle 构建完成后，任何页面都可以通过 `navigate()` 函数使用 bundle 路径跳转到该页面。

## 工作原理

调用 `navigate()` 时，Sparkling 会：

1. 构建 scheme URL（如 `hybrid://lynxview_page?bundle=detail.lynx.bundle&title=Detail`）
2. 将 URL 交给原生层，由原生层打开一个新容器
3. 新容器加载目标 bundle，并通过 `lynx.__globalProps.queryItems` 接收 URL 中的查询参数

## 基础导航

使用 `sparkling-navigation` 的 `navigate()` 方法，通过 bundle 路径打开另一个页面：

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

`params` 对象支持所有预定义 key（`title`、`hide_nav_bar`、`screen_orientation` 等），也支持任意自定义 key。

## 传递自定义数据

你在 `params` 中添加的任何 key 都会被拼接到 scheme URL 的查询参数中。在目标页面通过 `lynx.__globalProps.queryItems` 读取：

**页面 A — 发送方：**

```ts
import { navigate } from 'sparkling-navigation';

navigate(
  {
    path: 'pages/detail.lynx.bundle',
    options: {
      params: {
        title: '商品详情',
        itemId: '123',
        category: 'shoes',
      },
    },
  },
  (res) => {
    if (res.code !== 0) {
      console.error('导航失败:', res.msg);
    }
  }
);
```

**页面 B — 接收方：**

```ts
const globalProps = lynx.__globalProps;
const { itemId, category } = globalProps.queryItems;

console.log(itemId);   // "123"
console.log(category); // "shoes"
```

> 所有 query item 的值都是字符串类型。如需其他类型，请自行转换（如 `Number(itemId)`）。

## 关闭页面

调用 `close()` 关闭当前页面，返回上一个页面：

```ts
import { close } from 'sparkling-navigation';

close();
```

也可以通过容器 ID 关闭特定页面：

```ts
close({ containerID: 'some-container-id' });
```

## 完整示例

以下是一个最简两页应用。

### `pages/home.tsx` — 首页

```tsx
import { navigate } from 'sparkling-navigation';

function goToDetail(id: string) {
  navigate(
    {
      path: 'pages/detail.lynx.bundle',
      options: {
        params: {
          title: '详情',
          itemId: id,
        },
      },
    },
    (res) => {
      if (res.code !== 0) {
        console.error('导航失败:', res.msg);
      }
    }
  );
}

export function Home() {
  return <view onClick={() => goToDetail('42')}>打开详情</view>;
}
```

### `pages/detail.tsx` — 详情页

```tsx
import { close } from 'sparkling-navigation';

export function Detail() {
  const { itemId } = lynx.__globalProps.queryItems;

  return (
    <view>
      <text>商品: {itemId}</text>
      <view onClick={() => close()}>返回</view>
    </view>
  );
}
```
