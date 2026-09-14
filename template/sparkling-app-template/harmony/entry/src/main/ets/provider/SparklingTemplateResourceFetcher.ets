import {
  LynxResourceRequest,
  LynxTemplateResourceFetcher,
  TemplateProviderResult,
} from '@lynx/lynx';
import { AsyncCallback, BusinessError } from '@ohos.base';
import http from '@ohos.net.http';
import resourceManager from '@ohos.resourceManager';

export class SparklingTemplateResourceFetcher extends LynxTemplateResourceFetcher {
  fetchTemplate(
    request: LynxResourceRequest,
    callback: AsyncCallback<TemplateProviderResult, void>,
  ): void {
    if (request.url.startsWith('http')) {
      const httpRequest = http.createHttp();
      httpRequest.request(
        request.url,
        { expectDataType: http.HttpDataType.ARRAY_BUFFER },
        (error: BusinessError, response: http.HttpResponse) => {
          callback(error, { binary: response?.result as ArrayBuffer });
          httpRequest.destroy();
        },
      );
      return;
    }

    const context: Context = getContext(this);
    const manager: resourceManager.ResourceManager = context.resourceManager;
    manager.getRawFileContent(request.url, (error: BusinessError, data: Uint8Array) => {
      callback(error, { binary: data?.buffer as ArrayBuffer });
    });
  }

  fetchSSRData(
    request: LynxResourceRequest,
    callback: AsyncCallback<ArrayBuffer, void>,
  ): void {
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
}
