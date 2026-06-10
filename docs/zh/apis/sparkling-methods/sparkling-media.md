# `sparkling-media`

用于从 Lynx/JS 中选择、上传、下载和保存媒体文件的辅助 API。

## 安装

```bash
npm install sparkling-media
```

## 导出

### `chooseMedia(params, callback)`

从相册或相机中选择图片或视频。

- **请求**：

| 参数 | 类型 | 必填 | 默认值 | 描述 |
| --- | --- | --- | --- | --- |
| `mediaTypes` | `('image' \| 'video')[]` | 是 | - | 要选择的媒体类型 |
| `sourceType` | `'album' \| 'camera'` | 是 | - | 来源：相册或相机 |
| `maxCount` | `number` | 否 | `1` | 最大选择文件数 |
| `cameraType` | `'front' \| 'back'` | 否 | - | 当 `sourceType` 为 `'camera'` 时的相机类型（camera 时必填） |
| `compressImage` | `boolean` | 否 | `false` | 是否压缩图片 |
| `saveToPhotoAlbum` | `boolean` | 否 | `false` | 将拍摄的媒体保存到相册 |
| `needBase64Data` | `boolean` | 否 | `false` | 返回 base64 编码数据 |
| `compressOption` | `0 \| 1 \| 2 \| 3 \| 4` | 否 | `0` | `0`-默认，`1`-全部，`2`-仅 base64，`3`-仅图片，`4`-不压缩 |
| `compressWidth` | `number` | 否 | `0` | 目标压缩宽度 |
| `compressHeight` | `number` | 否 | `0` | 目标压缩高度 |
| `compressQuality` | `number` | 否 | `100` | 压缩质量（0-100） |

- **响应**：`{ code: number; msg: string; data?: { tempFiles: TempFile[] } }`

**TempFile**：

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| `tempFilePath` | `string` | 临时文件路径（相对） |
| `tempFileAbsolutePath` | `string` | 绝对文件路径 |
| `size` | `number` | 文件大小（字节） |
| `mediaType` | `'image' \| 'video'` | 媒体类型 |
| `mimeType` | `string` | 文件的 MIME 类型 |
| `base64Data` | `string` | Base64 数据（当 `needBase64Data` 为 `true` 时） |

示例：

```ts
import { chooseMedia } from 'sparkling-media';

chooseMedia(
  {
    mediaTypes: ['image'],
    sourceType: 'album',
    maxCount: 3,
  },
  (res) => {
    if (res.code === 1) {
      console.log(res.data?.tempFiles);
    }
  }
);
```

### `uploadFile(params, callback)`

上传文件到远程服务器。

- **请求**：

| 参数 | 类型 | 必填 | 描述 |
| --- | --- | --- | --- |
| `url` | `string` | 是 | 上传接口 URL |
| `filePath` | `string` | 否 | 要上传的本地文件路径 |
| `params` | `object` | 否 | 附加参数 |
| `header` | `object` | 否 | HTTP 请求头 |

- **响应**：`{ code: number; msg: string; data?: { url?: string; uri?: string; response?: object; clientCode?: number } }`

示例：

```ts
import { uploadFile } from 'sparkling-media';

uploadFile(
  {
    url: 'https://example.com/upload',
    filePath: '/tmp/photo.jpg',
    header: { Authorization: 'Bearer token' },
  },
  (res) => {
    console.log(res.code, res.data?.response);
  }
);
```

### `uploadImage(params, callback)`

上传图片到远程服务器。

- **请求**：

| 参数 | 类型 | 必填 | 描述 |
| --- | --- | --- | --- |
| `url` | `string` | 是 | 上传接口 URL |
| `filePath` | `string` | 否 | 要上传的本地图片文件路径 |
| `params` | `object` | 否 | 附加参数 |
| `header` | `object` | 否 | HTTP 请求头 |

- **响应**：`{ code: number; msg: string; data?: { url?: string; uri?: string; response?: object; clientCode?: number } }`

示例：

```ts
import { uploadImage } from 'sparkling-media';

uploadImage(
  {
    url: 'https://example.com/upload',
    filePath: '/tmp/photo.jpg',
    header: { Authorization: 'Bearer token' },
  },
  (res) => {
    console.log(res.code, res.data?.url);
  }
);
```

### `downloadFile(params, callback)`

从 URL 下载文件。

- **请求**：

| 参数 | 类型 | 必填 | 默认值 | 描述 |
| --- | --- | --- | --- | --- |
| `url` | `string` | 是 | - | 下载 URL |
| `extension` | `string` | 是 | - | 文件扩展名（如 `'png'`、`'jpg'`） |
| `params` | `object` | 否 | - | 附加参数 |
| `header` | `object` | 否 | - | HTTP 请求头 |
| `saveToAlbum` | `'image' \| 'video'` | 否 | - | 以图片或视频形式保存到相册 |
| `needCommonParams` | `boolean` | 否 | `true` | 为请求添加公共参数 |
| `timeoutInterval` | `number` | 否 | - | 请求超时时间（秒） |

- **响应**：`{ code: number; msg: string; data?: { httpCode: number; clientCode: number; header: object; filePath: string } }`

示例：

```ts
import { downloadFile } from 'sparkling-media';

downloadFile(
  {
    url: 'https://example.com/image.png',
    extension: 'png',
    saveToAlbum: 'image',
  },
  (res) => {
    if (res.code === 1) {
      console.log('已保存到：', res.data?.filePath);
    }
  }
);
```

### `saveDataURL(params, callback)`

将 base64 数据 URL 保存到设备。

- **请求**：

| 参数 | 类型 | 必填 | 描述 |
| --- | --- | --- | --- |
| `dataURL` | `string` | 是 | Base64 编码的数据 URL |
| `filename` | `string` | 是 | 文件名（不含扩展名） |
| `extension` | `string` | 是 | 文件扩展名（如 `'png'`、`'jpg'`） |
| `saveToAlbum` | `'image' \| 'video'` | 否 | 以图片或视频形式保存到相册 |

- **响应**：`{ code: number; msg: string; data?: { filePath: string } }`

示例：

```ts
import { saveDataURL } from 'sparkling-media';

saveDataURL(
  {
    dataURL: 'data:image/png;base64,iVBOR...',
    filename: 'screenshot',
    extension: 'png',
    saveToAlbum: 'image',
  },
  (res) => {
    if (res.code === 1) {
      console.log('已保存到：', res.data?.filePath);
    }
  }
);
```

## 原生方法名

此包调用以下方法：
- `media.chooseMedia`
- `media.uploadFile`
- `media.uploadImage`
- `media.downloadFile`
- `media.saveDataURL`

你的宿主应用必须注册这些方法的原生实现。参阅
[Sparkling Method SDK](../sparkling-method-android.md) / [iOS](../sparkling-method-ios.md)。

所有方法都使用统一的 [Sparkling Method 响应码](../response-codes.md)。
