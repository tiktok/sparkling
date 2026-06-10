// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

export type ParamsOption = 0 | 1 | 2;

export interface FormDataBody {
  key: string;
  value: string;
}

export interface UploadImageRequest {
  /** Upload URL (required) */
  url: string;
  /** Local image file path to upload */
  filePath?: string;
  /** Additional parameters */
  params?: Record<string, any>;
  /** HTTP headers */
  header?: Record<string, any>;
  /** Params option: 0, 1, or 2 */
  paramsOption?: ParamsOption;
  /** Form data body */
  formDataBody?: FormDataBody[];
}

export interface UploadImageResponseData {
  /** Uploaded image URL */
  url?: string;
  /** Uploaded image URI */
  uri?: string;
  /** Server response data */
  response?: Record<string, any>;
  /** Client code */
  clientCode?: number;
}

export interface UploadImageResponse {
  /** Response code: 1 for success, 0 for failure, negative for errors */
  code: number;
  /** Response message */
  msg: string;
  /** Response data */
  data?: UploadImageResponseData;
}

declare function uploadImage(params: UploadImageRequest, callback: (result: UploadImageResponse) => void): void;
