# Sparkling Method SDK - TypeScript

`sparkling-method` 包提供了 **JS 与原生之间**方法调用的 JS 端管道。
它是所有 Sparkling 方法包（router、storage、media 等）的基础。

## 安装

```bash
npm install sparkling-method
```

## 快速开始

```ts
import pipe from 'sparkling-method';

// 回调风格
pipe.call('router.open', { scheme: 'hybrid://lynxview_page?bundle=main.lynx.bundle' }, (res) => {
  console.log(res.code, res.msg);
});

// 异步风格
const data = await pipe.callAsync('storage.getItem', { key: 'token' });
console.log(data);
```

## API

### `pipe.call(methodMap, params, callback, options?)`

通过回调调用原生方法。

| 参数 | 类型 | 描述 |
| --- | --- | --- |
| `methodMap` | `string \| MethodMap` | 方法名（如 `'router.open'`）或 `{ module, method }` 对象 |
| `params` | `TParams` | 传递给原生端的请求参数 |
| `callback` | `(response: unknown) => void` | 接收原生响应的回调函数 |
| `options` | `Record<string, unknown>` | 预留参数 |

示例：

```ts
import pipe from 'sparkling-method';

pipe.call('storage.setItem', { key: 'lang', data: 'en' }, (res) => {
  console.log(res);
});
```

### `pipe.callAsync(methodMap, params, options?, timeout?)`

调用原生方法并返回 `Promise`。

| 参数 | 类型 | 默认值 | 描述 |
| --- | --- | --- | --- |
| `methodMap` | `string \| MethodMap` | - | 方法名或 `{ module, method }` 对象 |
| `params` | `TParams` | - | 请求参数 |
| `options` | `Record<string, unknown>` | `undefined` | 预留参数 |
| `timeout` | `number` | `30000` | 超时时间（毫秒） |

返回 `Promise<TResponse>` —— 当 `code === 1` 时 resolve `response.data`，否则 reject。

示例：

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

`callAsync` 的便捷封装，可指定自定义超时时间。

| 参数 | 类型 | 描述 |
| --- | --- | --- |
| `methodMap` | `string \| MethodMap` | 方法名或 `{ module, method }` 对象 |
| `params` | `TParams` | 请求参数 |
| `timeout` | `number` | 超时时间（毫秒） |
| `options` | `Record<string, unknown>` | 预留参数 |

返回 `Promise<TResponse>`。

### `pipe.on(eventName, callback)`

订阅原生事件。

| 参数 | 类型 | 描述 |
| --- | --- | --- |
| `eventName` | `string` | 要监听的事件名 |
| `callback` | `EventCallback` | `(event: unknown) => void` |

返回 `EventCallback`，可用于 `off()` 取消订阅。

### `pipe.off(eventName, callback)`

取消订阅原生事件。

| 参数 | 类型 | 描述 |
| --- | --- | --- |
| `eventName` | `string` | 事件名 |
| `callback` | `EventCallback` | 之前传给 `on()` 的回调函数 |

## 类型定义

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

## 响应码

Sparkling Method 响应码遵循 Android/iOS 原生桥接约定：`1` 表示成功，
`0` 表示方法已执行但失败，负数表示桥接或方法错误。详见[响应码](./response-codes.md)。

## 与方法包的关系

Sparkling 方法包（`sparkling-navigation`、`sparkling-storage`、`sparkling-media` 等）是
`sparkling-method` 的轻量封装。它们底层调用 `pipe.call()`，使用预定义的方法名，
如 `router.open`、`storage.setItem`、`media.chooseMedia` 等。

你可以直接使用 `sparkling-method` 来：
- 调用你自己实现的自定义原生方法
- 监听原生事件
- 构建你自己的方法包
