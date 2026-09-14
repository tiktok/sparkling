import { LynxMediaResourceFetcher, LynxOptionalBool, LynxResourceRequest } from '@lynx/lynx';
import { fileIo as fs, fileUri } from '@kit.CoreFileKit';

export class SparklingMediaResourceFetcher extends LynxMediaResourceFetcher {
  private context: Context;

  constructor(context: Context) {
    super();
    this.context = context;
  }

  shouldRedirectUrl(request: LynxResourceRequest): string {
    const rawFilePath = this.getRawFilePath(request.url);
    if (rawFilePath) {
      try {
        const targetPath = `${this.context.cacheDir}/sparkling-assets/${rawFilePath}`;
        try {
          fs.mkdirSync(targetPath.substring(0, targetPath.lastIndexOf('/')), true);
        } catch (_) {
        }
        const data = this.context.resourceManager.getRawFileContentSync(rawFilePath);
        const file = fs.openSync(targetPath, fs.OpenMode.CREATE | fs.OpenMode.WRITE_ONLY | fs.OpenMode.TRUNC);
        fs.writeSync(file.fd, data.buffer);
        fs.closeSync(file);
        return fileUri.getUriFromPath(targetPath);
      } catch (_) {
        return request.url;
      }
    }
    return request.url;
  }

  isLocalResource(url: string): LynxOptionalBool {
    if (this.getRawFilePath(url) || url.startsWith('file://')) {
      return LynxOptionalBool.TRUE;
    }
    return LynxOptionalBool.UNDEFINED;
  }

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
    return !path || path.split('/').includes('..') ? undefined : path;
  }
}
