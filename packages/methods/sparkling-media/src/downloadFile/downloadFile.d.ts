// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

export type SaveToAlbum = 'image' | 'video';

export interface DownloadFileRequest {
  /** Download URL (required) */
  url: string;
  /** Additional parameters */
  params?: Record<string, any>;
  /** HTTP headers */
  header?: Record<string, any>;
  /** File extension (required) */
  extension: string;
  /** Save to album: 'image' or 'video' */
  saveToAlbum?: SaveToAlbum;
  /** Whether to add common params, default: true */
  needCommonParams?: boolean;
  /** Timeout interval in seconds */
  timeoutInterval?: number;
}

export interface DownloadFileResponseData {
  /** HTTP status code */
  httpCode?: number;
  /** Client code */
  clientCode?: number;
  /** Response headers */
  header?: Record<string, any>;
  /** Downloaded file path */
  filePath?: string;
  /** Server response data */
  response?: Record<string, any>;
}

export interface DownloadFileResponse {
  /** Response code: 1 for success, 0 for failure, negative for errors */
  code: number;
  /** Response message */
  msg: string;
  /** Response data */
  data?: DownloadFileResponseData;
}

declare function downloadFile(params: DownloadFileRequest, callback: (result: DownloadFileResponse) => void): void;
