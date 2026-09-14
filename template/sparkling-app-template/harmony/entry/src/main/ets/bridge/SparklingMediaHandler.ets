import { common } from '@kit.AbilityKit';
import { camera, cameraPicker } from '@kit.CameraKit';
import { fileIo as fs, fileUri } from '@kit.CoreFileKit';
import { photoAccessHelper } from '@kit.MediaLibraryKit';
import { image } from '@kit.ImageKit';
import { request } from '@kit.BasicServicesKit';
import { util, url } from '@kit.ArkTS';

export interface MediaRequestData {
  mediaTypes?: string[];
  sourceType?: string;
  maxCount?: number;
  cameraType?: string;
  isNeedCut?: boolean;
  cropRatioWidth?: number;
  cropRatioHeight?: number;
  needBase64Data?: boolean;
  shouldKeepOriginalFormat?: boolean;
  compressImage?: boolean;
  saveToPhotoAlbum?: boolean;
  compressOption?: number;
  compressWidth?: number;
  compressHeight?: number;
  compressQuality?: number;
  permissionDenyAction?: number;
  isMultiSelect?: boolean;
  useNewCompressSolution?: boolean;
  url?: string;
  params?: Record<string, Object>;
  header?: Record<string, Object>;
  extension?: string;
  timeoutInterval?: number;
  needCommonParams?: boolean;
  saveToAlbum?: string;
  dataURL?: string;
  filename?: string;
  filePath?: string;
  formDataBody?: FormDataBody[];
  paramsOption?: number;
}

interface FormDataBody {
  key: string;
  value: string;
}

interface MediaResponse {
  code: number;
  msg: string;
  data?: Object;
}

interface TempFile {
  tempFilePath: string;
  tempFileAbsolutePath: string;
  size: number;
  mediaType: string;
  mimeType: string;
  base64Data?: string;
}

interface ChooseMediaData {
  tempFiles: TempFile[];
}

interface FilePathData {
  filePath: string;
  clientCode?: number;
  httpCode?: number;
  header?: Record<string, Object>;
  response?: Object;
}

interface UploadData {
  url: string;
  uri: string;
  clientCode: number;
}

type MediaCallback = (response: MediaResponse) => void;

export class SparklingMediaHandler {
  chooseMedia(context: common.UIAbilityContext, data: MediaRequestData, callback: MediaCallback): void {
    if (!data.mediaTypes?.length || !data.sourceType) {
      callback({ code: -1, msg: 'media.chooseMedia requires mediaTypes and sourceType' });
      return;
    }

    const operation = data.sourceType === 'camera'
      ? this.pickFromCamera(context, data)
      : this.pickFromAlbum(data);
    operation
      .then((uris: string[]): Promise<string[]> => this.prepareChosenMedia(context, uris, data))
      .then((uris: string[]): void => {
        const files = uris.map((uri: string): TempFile => this.describeFile(uri, data.needBase64Data === true));
        const result: ChooseMediaData = { tempFiles: files };
        callback({ code: 1, msg: 'ok', data: result });
      })
      .catch((error: Error): void => callback({ code: 0, msg: `Failed to choose media: ${error.message}` }));
  }

  downloadFile(
    context: Context,
    data: MediaRequestData,
    commonParams: Record<string, Object>,
    callback: MediaCallback,
  ): void {
    const remoteURL = this.withParams(
      data.url?.trim() ?? '',
      data.needCommonParams === false ? {} : commonParams,
      data.params,
    );
    const extension = this.safeExtension(data.extension);
    if (!remoteURL || !extension) {
      callback({ code: -1, msg: 'media.downloadFile requires url and extension' });
      return;
    }

    const directory = this.ensureMediaDirectory(context);
    const filePath = `${directory}/download-${Date.now()}.${extension}`;
    const config: request.agent.Config = {
      action: request.agent.Action.DOWNLOAD,
      url: remoteURL,
      headers: data.header,
      method: 'GET',
      mode: request.agent.Mode.FOREGROUND,
      overwrite: true,
      saveas: filePath,
    };
    if (data.timeoutInterval !== undefined && data.timeoutInterval > 0) {
      config.timeout = {
        connectionTimeout: data.timeoutInterval,
        totalTimeout: data.timeoutInterval,
      };
    }
    let httpCode: number | undefined;
    let responseHeaders: Record<string, Object> | undefined;
    request.agent.create(context, config)
      .then((task: request.agent.Task): Promise<void> => {
        task.on('response', (response: request.agent.HttpResponse): void => {
          httpCode = response.statusCode;
          responseHeaders = this.headerRecord(response.headers);
        });
        task.on('completed', (progress: request.agent.Progress): void => {
          this.saveToAlbumIfRequested(context, filePath, extension, data.saveToAlbum)
            .then((): void => {
              const result: FilePathData = {
                filePath,
                clientCode: 0,
                httpCode,
                header: responseHeaders,
                response: progress.extras,
              };
              callback({ code: 1, msg: 'ok', data: result });
            })
            .catch((error: Error): void => callback({ code: 0, msg: `Failed to save download: ${error.message}` }));
        });
        task.on('failed', (progress: request.agent.Progress): void => {
          const result: FilePathData = { filePath, httpCode, header: responseHeaders };
          callback({ code: 0, msg: `Download failed with state ${progress.state}`, data: result });
        });
        return task.start();
      })
      .catch((error: Error): void => callback({ code: 0, msg: `Failed to start download: ${error.message}` }));
  }

  saveDataURL(context: Context, data: MediaRequestData, callback: MediaCallback): void {
    const dataURL = data.dataURL?.trim() ?? '';
    const filename = this.safeFilename(data.filename);
    const extension = this.safeExtension(data.extension);
    const comma = dataURL.indexOf(',');
    if (!dataURL.startsWith('data:') || comma < 0 || !filename || !extension) {
      callback({ code: -1, msg: 'media.saveDataURL requires a valid data URL, filename, and extension' });
      return;
    }

    try {
      const bytes = new util.Base64Helper().decodeSync(dataURL.substring(comma + 1));
      const filePath = `${this.ensureMediaDirectory(context)}/${filename}.${extension}`;
      const file = fs.openSync(filePath, fs.OpenMode.CREATE | fs.OpenMode.WRITE_ONLY | fs.OpenMode.TRUNC);
      fs.writeSync(file.fd, bytes.buffer);
      fs.closeSync(file);
      this.saveToAlbumIfRequested(context, filePath, extension, data.saveToAlbum)
        .then((): void => {
          const result: FilePathData = { filePath };
          callback({ code: 1, msg: 'ok', data: result });
        })
        .catch((error: Error): void => callback({ code: 0, msg: `Failed to save media: ${error.message}` }));
    } catch (error) {
      callback({ code: 0, msg: `Failed to save data URL: ${String(error)}` });
    }
  }

  uploadFile(context: Context, data: MediaRequestData, callback: MediaCallback): void {
    const remoteURL = data.url?.trim();
    const sourcePaths: string[] = [];
    const uploadFiles: request.File[] = [];
    if (!remoteURL) {
      callback({ code: -1, msg: 'media upload requires url' });
      return;
    }

    try {
      const sourcePath = data.filePath?.trim();
      if (sourcePath) {
        sourcePaths.push(sourcePath);
        uploadFiles.push(this.prepareUploadFile(context, sourcePath, 'file'));
      }
      data.formDataBody?.forEach((entry: FormDataBody): void => {
        const path = entry.value.trim();
        if (path) {
          sourcePaths.push(path);
          uploadFiles.push(this.prepareUploadFile(context, path, entry.key || 'file'));
        }
      });
      if (!uploadFiles.length) {
        callback({ code: -1, msg: 'media upload requires filePath or formDataBody' });
        return;
      }
      const formData: request.RequestData[] = [];
      if (data.params && data.paramsOption !== 2) {
        Object.entries(data.params).forEach((entry: [string, Object]): void => {
          formData.push({ name: entry[0], value: String(entry[1]) });
        });
      }
      const emptyHeader: Record<string, Object> = {};
      const config: request.UploadConfig = {
        url: remoteURL,
        header: data.header ?? emptyHeader,
        method: 'POST',
        files: uploadFiles,
        data: formData,
      };
      request.uploadFile(context, config)
        .then((task: request.UploadTask): void => {
          task.on('complete', (states: request.TaskState[]): void => {
            const state = states[0];
            const clientCode = state?.responseCode ?? 0;
            const result: UploadData = { url: remoteURL, uri: sourcePaths[0], clientCode };
            callback({ code: clientCode >= 200 && clientCode < 300 ? 1 : 0, msg: state?.message ?? 'ok', data: result });
          });
          task.on('fail', (states: request.TaskState[]): void => {
            callback({ code: 0, msg: states[0]?.message ?? 'Upload failed' });
          });
        })
        .catch((error: Error): void => callback({ code: 0, msg: `Failed to start upload: ${error.message}` }));
    } catch (error) {
      callback({ code: 0, msg: `Failed to prepare upload: ${String(error)}` });
    }
  }

  private pickFromAlbum(data: MediaRequestData): Promise<string[]> {
    const options = new photoAccessHelper.PhotoSelectOptions();
    const mediaTypes = data.mediaTypes ?? [];
    options.MIMEType = mediaTypes.length > 1
      ? photoAccessHelper.PhotoViewMIMETypes.IMAGE_VIDEO_TYPE
      : mediaTypes[0] === 'video'
        ? photoAccessHelper.PhotoViewMIMETypes.VIDEO_TYPE
        : photoAccessHelper.PhotoViewMIMETypes.IMAGE_TYPE;
    options.maxSelectNumber = data.isMultiSelect === true ? Math.max(1, data.maxCount ?? 1) : 1;
    options.isOriginalSupported = data.shouldKeepOriginalFormat === true;
    return new photoAccessHelper.PhotoViewPicker().select(options)
      .then((result: photoAccessHelper.PhotoSelectResult): string[] => result.photoUris);
  }

  private pickFromCamera(context: common.UIAbilityContext, data: MediaRequestData): Promise<string[]> {
    const profile = new cameraPicker.PickerProfile();
    profile.cameraPosition = data.cameraType === 'front'
      ? camera.CameraPosition.CAMERA_POSITION_FRONT
      : camera.CameraPosition.CAMERA_POSITION_BACK;
    const mediaTypes: cameraPicker.PickerMediaType[] = (data.mediaTypes ?? []).map(
      (type: string): cameraPicker.PickerMediaType => type === 'video'
        ? cameraPicker.PickerMediaType.VIDEO
        : cameraPicker.PickerMediaType.PHOTO,
    );
    return cameraPicker.pick(context, mediaTypes, profile)
      .then((result: cameraPicker.PickerResult): string[] => {
        if (result.resultCode !== 0 || !result.resultUri) {
          throw new Error('Camera selection was cancelled');
        }
        return [result.resultUri];
      });
  }

  private prepareChosenMedia(context: Context, uris: string[], data: MediaRequestData): Promise<string[]> {
    return Promise.all(uris.map((uri: string): Promise<string> => {
      const isImage = !this.mimeType(uri).startsWith('video/');
      const shouldCompress = isImage && data.compressImage === true && data.compressOption !== 4;
      const prepared = shouldCompress ? this.compressImage(context, uri, data) : Promise.resolve(uri);
      return prepared.then((resultUri: string): Promise<string> => {
        if (data.saveToPhotoAlbum !== true) {
          return Promise.resolve(resultUri);
        }
        const extension = this.extensionForPath(resultUri, isImage ? 'jpg' : 'mp4');
        return this.saveToAlbumIfRequested(context, resultUri, extension, isImage ? 'image' : 'video')
          .then((): string => resultUri);
      });
    }));
  }

  private compressImage(context: Context, uri: string, data: MediaRequestData): Promise<string> {
    const source = image.createImageSource(uri);
    return source.getImageInfo().then((info: image.ImageInfo): Promise<image.PixelMap> => {
      const original = info.size;
      const maxWidth = Math.max(0, Math.floor(data.compressWidth ?? 0));
      const maxHeight = Math.max(0, Math.floor(data.compressHeight ?? 0));
      const widthScale = maxWidth > 0 ? maxWidth / original.width : 1;
      const heightScale = maxHeight > 0 ? maxHeight / original.height : 1;
      const scale = Math.min(1, widthScale, heightScale);
      const options: image.DecodingOptions = {};
      if (scale < 1) {
        options.desiredSize = {
          width: Math.max(1, Math.round(original.width * scale)),
          height: Math.max(1, Math.round(original.height * scale)),
        };
      }
      return source.createPixelMap(options);
    }).then((pixelMap: image.PixelMap): Promise<string> => {
      const outputPath = `${this.ensureMediaDirectory(context)}/image-${Date.now()}.jpg`;
      const output = fs.openSync(outputPath, fs.OpenMode.CREATE | fs.OpenMode.WRITE_ONLY | fs.OpenMode.TRUNC);
      const packer = image.createImagePacker();
      const quality = Math.min(100, Math.max(0, Math.floor(data.compressQuality ?? 100)));
      return packer.packToFile(pixelMap, output.fd, { format: 'image/jpeg', quality })
        .then((): string => outputPath)
        .finally((): void => {
          fs.closeSync(output);
          pixelMap.release();
          packer.release();
        });
    }).finally((): Promise<void> => source.release());
  }

  private describeFile(uri: string, includeBase64: boolean): TempFile {
    const file = fs.openSync(uri, fs.OpenMode.READ_ONLY);
    try {
      const stat = fs.statSync(file.fd);
      const mediaType = this.mimeType(uri).startsWith('video/') ? 'video' : 'image';
      const result: TempFile = {
        tempFilePath: uri,
        tempFileAbsolutePath: file.path,
        size: stat.size,
        mediaType,
        mimeType: this.mimeType(uri),
      };
      if (includeBase64) {
        const buffer = new ArrayBuffer(stat.size);
        const bytesRead = fs.readSync(file.fd, buffer);
        result.base64Data = new util.Base64Helper().encodeToStringSync(new Uint8Array(buffer, 0, bytesRead));
      }
      return result;
    } finally {
      fs.closeSync(file);
    }
  }

  private withParams(
    remoteURL: string,
    commonParams?: Record<string, Object>,
    params?: Record<string, Object>,
  ): string {
    if (!remoteURL || (!commonParams && !params)) {
      return remoteURL;
    }
    const parsed = url.URL.parseURL(remoteURL);
    Object.entries(commonParams ?? {}).forEach((entry: [string, Object]): void => {
      if (!parsed.params.has(entry[0])) {
        parsed.params.append(entry[0], String(entry[1]));
      }
    });
    Object.entries(params ?? {}).forEach((entry: [string, Object]): void => parsed.params.set(entry[0], String(entry[1])));
    return parsed.toString();
  }

  private headerRecord(headers: Map<string, Array<string>>): Record<string, Object> {
    const result: Record<string, Object> = {};
    headers.forEach((values: Array<string>, key: string): void => {
      result[key] = values;
    });
    return result;
  }

  private ensureMediaDirectory(context: Context): string {
    const directory = `${context.cacheDir}/sparkling-media`;
    try {
      fs.mkdirSync(directory, true);
    } catch (_) {
    }
    return directory;
  }

  private copyToUploadCache(context: Context, sourcePath: string): string {
    const filename = `${Date.now()}-${this.safeFilename(sourcePath.substring(sourcePath.lastIndexOf('/') + 1)) || 'upload'}`;
    const target = `${this.ensureMediaDirectory(context)}/${filename}`;
    fs.copyFileSync(sourcePath, target);
    return target;
  }

  private prepareUploadFile(context: Context, sourcePath: string, name: string): request.File {
    const uploadPath = this.copyToUploadCache(context, sourcePath);
    const filename = uploadPath.substring(uploadPath.lastIndexOf('/') + 1);
    return {
      filename,
      name,
      uri: `internal://cache/sparkling-media/${filename}`,
      type: this.mimeType(sourcePath),
    };
  }

  private saveToAlbumIfRequested(
    context: Context,
    sourcePath: string,
    extension: string,
    saveToAlbum?: string,
  ): Promise<void> {
    if (saveToAlbum !== 'image' && saveToAlbum !== 'video') {
      return Promise.resolve();
    }
    const config: photoAccessHelper.PhotoCreationConfig = {
      title: `sparkling-${Date.now()}`,
      fileNameExtension: extension,
      photoType: saveToAlbum === 'video'
        ? photoAccessHelper.PhotoType.VIDEO
        : photoAccessHelper.PhotoType.IMAGE,
    };
    return photoAccessHelper.getPhotoAccessHelper(context)
      .showAssetsCreationDialog([fileUri.getUriFromPath(sourcePath)], [config])
      .then((targets: string[]): void => {
        if (!targets.length) {
          throw new Error('Album save was cancelled');
        }
        fs.copyFileSync(sourcePath, targets[0]);
      });
  }

  private safeFilename(value?: string): string {
    return (value ?? '').trim().replace(/[^a-zA-Z0-9._-]/g, '_').replace(/^\.+/, '');
  }

  private safeExtension(value?: string): string {
    return (value ?? '').trim().replace(/^\./, '').replace(/[^a-zA-Z0-9]/g, '').toLowerCase();
  }

  private extensionForPath(path: string, fallback: string): string {
    const cleanPath = path.split('?')[0];
    const separator = cleanPath.lastIndexOf('.');
    return separator >= 0 ? this.safeExtension(cleanPath.substring(separator + 1)) || fallback : fallback;
  }

  private mimeType(path: string): string {
    const extension = path.split('?')[0].substring(path.lastIndexOf('.') + 1).toLowerCase();
    switch (extension) {
      case 'jpg':
      case 'jpeg': return 'image/jpeg';
      case 'png': return 'image/png';
      case 'gif': return 'image/gif';
      case 'webp': return 'image/webp';
      case 'mp4': return 'video/mp4';
      case 'mov': return 'video/quicktime';
      default: return 'application/octet-stream';
    }
  }
}
