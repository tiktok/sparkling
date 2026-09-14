import {
  LynxError,
  LynxGenericResourceFetcher,
  LynxResourceRequest,
  LynxResourceType,
  LynxStreamDelegate,
  LynxSubErrorCode,
} from '@lynx/lynx';
import { AsyncCallback, BusinessError } from '@ohos.base';
import { fileIo as fs } from '@kit.CoreFileKit';
import http from '@ohos.net.http';
import { CacheStrategy, ImageKnife, ImageKnifeOption } from '@ohos/imageknife';

export class SparklingGenericResourceFetcher extends LynxGenericResourceFetcher {
  private context: Context;

  constructor(context: Context) {
    super();
    this.context = context;
  }

  fetchResource(request: LynxResourceRequest, callback: AsyncCallback<ArrayBuffer, void>): void {
    const rawFilePath = this.getRawFilePath(request.url);
    if (rawFilePath) {
      this.context.resourceManager.getRawFileContent(rawFilePath, (error: BusinessError, data: Uint8Array) => {
        callback(error, data?.buffer as ArrayBuffer);
      });
      return;
    }

    const httpRequest = http.createHttp();
    httpRequest.request(
      request.url,
      { expectDataType: http.HttpDataType.ARRAY_BUFFER },
      (error: BusinessError, response: http.HttpResponse) => {
        callback(error, response?.result as ArrayBuffer);
        httpRequest.destroy();
      },
    );
  }

  fetchResourcePath(request: LynxResourceRequest, callback: AsyncCallback<string, void>): void {
    if (request.type !== LynxResourceType.LYNX_RESOURCE_TYPE_IMAGE) {
      callback({
        code: LynxError.LYNX_ERROR_CODE_RESOURCE,
        message: `Unsupported resource type: ${request.type}`,
        name: 'Resource Error',
      }, '');
      return;
    }

    const rawFilePath = this.getRawFilePath(request.url);
    if (rawFilePath) {
      this.copyRawFileToCache(rawFilePath, callback);
      return;
    }

    const option = new ImageKnifeOption();
    option.loadSrc = request.url;
    option.writeCacheStrategy = CacheStrategy.File;
    ImageKnife.getInstance().preLoadCache(option).then((data: string) => {
      if (data.length > 0) {
        callback(undefined, data);
        return;
      }
      callback({
        code: LynxSubErrorCode.E_RESOURCE_IMAGE_PIC_SOURCE,
        message: 'Image path is invalid',
        name: 'Image Error',
      }, '');
    }).catch((error: string) => {
      callback({
        code: LynxSubErrorCode.E_RESOURCE_IMAGE_FROM_NETWORK_OR_OTHERS,
        message: error,
        name: 'Image Error',
      }, '');
    });
  }

  fetchStream(_request: LynxResourceRequest, delegate: LynxStreamDelegate): void {
    delegate.onStart(0);
    delegate.onEnd();
  }

  cancel(_request: LynxResourceRequest): void {}

  private getRawFilePath(resourceUrl: string): string | undefined {
    let path = resourceUrl.trim();
    if (path.startsWith('asset:///')) {
      path = path.slice('asset:///'.length);
    } else if (path.startsWith('asset://')) {
      path = path.slice('asset://'.length);
    } else if (path.includes('://')) {
      return undefined;
    }

    path = path.replace(/^\/+/, '');
    if (!path || path.split('/').includes('..')) {
      return undefined;
    }
    return path;
  }

  private copyRawFileToCache(rawFilePath: string, callback: AsyncCallback<string, void>): void {
    const targetPath = `${this.context.cacheDir}/sparkling-assets/${rawFilePath}`;
    const separator = targetPath.lastIndexOf('/');
    const targetDir = targetPath.slice(0, separator);

    this.context.resourceManager.getRawFileContent(rawFilePath, (error: BusinessError, data: Uint8Array) => {
      if (error) {
        callback(error, '');
        return;
      }

      try {
        try {
          fs.mkdirSync(targetDir, true);
        } catch (_) {
        }
        const file = fs.openSync(
          targetPath,
          fs.OpenMode.READ_WRITE | fs.OpenMode.CREATE | fs.OpenMode.TRUNC,
        );
        fs.writeSync(file.fd, data.buffer);
        fs.closeSync(file);
        callback(undefined, targetPath);
      } catch (writeError) {
        const failure = writeError as BusinessError;
        callback(failure, '');
      }
    });
  }
}
