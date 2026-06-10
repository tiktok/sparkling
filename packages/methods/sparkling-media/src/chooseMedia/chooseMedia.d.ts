// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

export type MediaType = 'image' | 'video';
export type SourceType = 'album' | 'camera';
export type CameraType = 'front' | 'back';
export type CompressOption = 0 | 1 | 2 | 3 | 4;
export type PermissionDenyAction = 0 | 1;

export interface ChooseMediaRequest {
  /** Types of media to select */
  mediaTypes: MediaType[];
  /** Source type: album or camera */
  sourceType: SourceType;
  /** Maximum number of files to select, default: 1 */
  maxCount?: number;
  /** Camera type when sourceType is 'camera' */
  cameraType?: CameraType;
  /** Whether to compress images, default: false */
  compressImage?: boolean;
  /** Whether to save to photo album, default: false */
  saveToPhotoAlbum?: boolean;
  /** Whether to crop the image, default: false */
  isNeedCut?: boolean;
  /** Crop ratio width, default: 0 */
  cropRatioWidth?: number;
  /** Crop ratio height, default: 0 */
  cropRatioHeight?: number;
  /** Whether to return base64 data, default: false */
  needBase64Data?: boolean;
  /** Compress option: 0-default, 1-both, 2-onlyBase64, 3-onlyImage, 4-none */
  compressOption?: CompressOption;
  /** Compress width, default: 0 */
  compressWidth?: number;
  /** Compress height, default: 0 */
  compressHeight?: number;
  /** Permission deny action: 0-show alert, 1-no alert */
  permissionDenyAction?: PermissionDenyAction;
  /** Whether to allow multi-select, default: false */
  isMultiSelect?: boolean;
  /** Whether to use new compress solution, default: false */
  useNewCompressSolution?: boolean;
  /** Whether to keep original format */
  shouldKeepOriginalFormat?: boolean;
  /** Compress quality (0-100), default: 100 */
  compressQuality?: number;
}

export interface TempFile {
  /** Temporary file path (relative) */
  tempFilePath: string;
  /** Absolute file path */
  tempFileAbsolutePath?: string;
  /** File size in bytes */
  size: number;
  /** Media type: 'image' or 'video' */
  mediaType: MediaType;
  /** MIME type of the file */
  mimeType?: string;
  /** Base64 encoded data (if needBase64Data is true) */
  base64Data?: string;
}

export interface ChooseMediaResponseData {
  /** Selected files */
  tempFiles?: TempFile[];
}

export interface ChooseMediaResponse {
  /** Response code: 1 for success, 0 for failure, negative for errors */
  code: number;
  /** Response message */
  msg: string;
  /** Response data */
  data?: ChooseMediaResponseData;
}

declare function chooseMedia(params: ChooseMediaRequest, callback: (result: ChooseMediaResponse) => void): void;
