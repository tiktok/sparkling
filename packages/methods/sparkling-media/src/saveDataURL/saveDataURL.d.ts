// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

export interface SaveDataURLRequest {
  /** Base64 encoded data URL (required) */
  dataURL: string;
  /** File name without extension (required) */
  filename: string;
  /** File extension (required) */
  extension: string;
  /** Save to album: 'image' or 'video' */
  saveToAlbum?: string;
}

export interface SaveDataURLResponseData {
  /** Saved file path */
  filePath?: string;
}

export interface SaveDataURLResponse {
  /** Response code: 1 for success, 0 for failure, negative for errors */
  code: number;
  /** Response message */
  msg: string;
  /** Response data */
  data?: SaveDataURLResponseData;
}

declare function saveDataURL(params: SaveDataURLRequest, callback: (result: SaveDataURLResponse) => void): void;
