# `sparkling-media`

Media helper APIs for choosing, uploading, downloading, and saving media files from Lynx/JS.

## Install

```bash
npm install sparkling-media
```

## Exports

### `chooseMedia(params, callback)`

Select images or videos from album or camera.

- **Request**:

| Param | Type | Required | Default | Description |
| --- | --- | --- | --- | --- |
| `mediaTypes` | `('image' \| 'video')[]` | Yes | - | Types of media to select |
| `sourceType` | `'album' \| 'camera'` | Yes | - | Source: photo album or camera |
| `maxCount` | `number` | No | `1` | Maximum number of files to select |
| `cameraType` | `'front' \| 'back'` | No | - | Camera type when `sourceType` is `'camera'` (required for camera) |
| `compressImage` | `boolean` | No | `false` | Whether to compress images |
| `saveToPhotoAlbum` | `boolean` | No | `false` | Save captured media to photo album |
| `needBase64Data` | `boolean` | No | `false` | Return base64 encoded data |
| `compressOption` | `0 \| 1 \| 2 \| 3 \| 4` | No | `0` | `0`-default, `1`-both, `2`-onlyBase64, `3`-onlyImage, `4`-none |
| `compressWidth` | `number` | No | `0` | Target compress width |
| `compressHeight` | `number` | No | `0` | Target compress height |
| `compressQuality` | `number` | No | `100` | Compress quality (0-100) |

- **Response**: `{ code: number; msg: string; data?: { tempFiles: TempFile[] } }`

**TempFile**:

| Field | Type | Description |
| --- | --- | --- |
| `tempFilePath` | `string` | Temporary file path (relative) |
| `tempFileAbsolutePath` | `string` | Absolute file path |
| `size` | `number` | File size in bytes |
| `mediaType` | `'image' \| 'video'` | Media type |
| `mimeType` | `string` | MIME type of the file |
| `base64Data` | `string` | Base64 data (if `needBase64Data` is `true`) |

Example:

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

Upload a file to a remote server.

- **Request**:

| Param | Type | Required | Description |
| --- | --- | --- | --- |
| `url` | `string` | Yes | Upload endpoint URL |
| `filePath` | `string` | No | Local file path to upload |
| `params` | `object` | No | Additional parameters to send |
| `header` | `object` | No | HTTP headers |

- **Response**: `{ code: number; msg: string; data?: { url?: string; uri?: string; response?: object; clientCode?: number } }`

Example:

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

Upload an image to a remote server.

- **Request**:

| Param | Type | Required | Description |
| --- | --- | --- | --- |
| `url` | `string` | Yes | Upload endpoint URL |
| `filePath` | `string` | No | Local image file path to upload |
| `params` | `object` | No | Additional parameters to send |
| `header` | `object` | No | HTTP headers |

- **Response**: `{ code: number; msg: string; data?: { url?: string; uri?: string; response?: object; clientCode?: number } }`

Example:

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

Download a file from a URL.

- **Request**:

| Param | Type | Required | Default | Description |
| --- | --- | --- | --- | --- |
| `url` | `string` | Yes | - | Download URL |
| `extension` | `string` | Yes | - | File extension (e.g. `'png'`, `'jpg'`) |
| `params` | `object` | No | - | Additional parameters |
| `header` | `object` | No | - | HTTP headers |
| `saveToAlbum` | `'image' \| 'video'` | No | - | Save to photo album as image or video |
| `needCommonParams` | `boolean` | No | `true` | Add common parameters to request |
| `timeoutInterval` | `number` | No | - | Request timeout in seconds |

- **Response**: `{ code: number; msg: string; data?: { httpCode: number; clientCode: number; header: object; filePath: string } }`

Example:

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
      console.log('Saved to:', res.data?.filePath);
    }
  }
);
```

### `saveDataURL(params, callback)`

Save a base64 data URL to the device.

- **Request**:

| Param | Type | Required | Description |
| --- | --- | --- | --- |
| `dataURL` | `string` | Yes | Base64 encoded data URL |
| `filename` | `string` | Yes | File name without extension |
| `extension` | `string` | Yes | File extension (e.g. `'png'`, `'jpg'`) |
| `saveToAlbum` | `'image' \| 'video'` | No | Save to photo album as image or video |

- **Response**: `{ code: number; msg: string; data?: { filePath: string } }`

Example:

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
      console.log('Saved to:', res.data?.filePath);
    }
  }
);
```

## Native method names

This package calls:
- `media.chooseMedia`
- `media.uploadFile`
- `media.uploadImage`
- `media.downloadFile`
- `media.saveDataURL`

Your host app must register native implementations for these methods. See
[Sparkling Method SDK](../sparkling-method-android.md) / [iOS](../sparkling-method-ios.md).

All methods use the shared [Sparkling Method response codes](../response-codes.md).
