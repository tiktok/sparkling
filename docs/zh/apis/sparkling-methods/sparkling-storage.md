# `sparkling-storage`

用于 Lynx/JS 的键值存储辅助 API。

## 安装

```bash
npm install sparkling-storage
```

## 导出

### `setItem(params, callback)`

向存储中写入一个条目。

- **请求**：`{ key: string; data: any; biz?: string; validDuration?: number }`
- **响应**：`{ code: number; msg: string; data?: any }`

示例：

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

从存储中读取一个条目。

- **请求**：`{ key: string; biz?: string }`
- **响应**：`{ code: number; msg: string; data?: { data?: any } }`

示例：

```ts
import { getItem } from 'sparkling-storage';

getItem({ key: 'token', biz: 'demo' }, (res) => {
  console.log(res.code, res.msg, res.data?.data);
});
```

## 原生方法名

此包调用以下方法：
- `storage.setItem`
- `storage.getItem`

你的宿主应用必须注册这些方法的原生实现。参阅
[Sparkling Method SDK](../sparkling-method-android.md) / [iOS](../sparkling-method-ios.md)。

所有方法都使用统一的 [Sparkling Method 响应码](../response-codes.md)。
