import { common } from '@kit.AbilityKit';
import { preferences } from '@kit.ArkData';
import { url } from '@kit.ArkTS';
import { LynxModule } from '@lynx/lynx';
import { MediaRequestData, SparklingMediaHandler } from './SparklingMediaHandler';
import { dispatchSparklingMethod, isSparklingMethodLinked } from './SparklingAutolink';
import { SparklingHostHooks } from './SparklingHostHooks';

interface PipeRequestData extends MediaRequestData {
  scheme?: string;
  replace?: boolean;
  replaceType?: string;
  useSysBrowser?: boolean;
  animated?: boolean;
  interceptor?: string;
  key?: string;
  data?: Object;
  biz?: string;
  validDuration?: number;
  containerID?: string;
  extra?: Record<string, Object>;
}

interface PipeRequest {
  data?: PipeRequestData;
}

interface PipeResponse {
  code: number;
  msg: string;
  data?: Object;
}

interface StoredValue {
  value: Object;
  expiresAt: number;
}

interface StorageResultData {
  data?: Object;
}

type PipeCallback = (response: PipeResponse) => void;
type OpenPage = (page: SparklingPage, replace: boolean, animated: boolean) => void;
type ClosePage = (containerID: string | undefined, animated: boolean) => boolean;

let nextPageID: number = 1;

export class SparklingPage {
  id: string;
  source: string;
  scheme: string;
  queryItems: Record<string, Object>;
  createdAt: string;

  constructor(source: string, scheme: string, queryItems: Record<string, Object>) {
    this.id = `sparkling-harmony-${nextPageID++}`;
    this.source = source;
    this.scheme = scheme;
    this.queryItems = queryItems;
    this.createdAt = String(Date.now());
  }

  static root(source: string): SparklingPage {
    const queryItems: Record<string, Object> = {};
    queryItems['bundle'] = source;
    queryItems['hide_nav_bar'] = '1';
    return new SparklingPage(source, `hybrid://lynxview_page?bundle=${source}`, queryItems);
  }
}

export class SparklingNavigationHandler {
  private openPage: OpenPage;
  private closePage: ClosePage;
  private hostHooks: SparklingHostHooks;

  constructor(openPage: OpenPage, closePage: ClosePage, hostHooks: SparklingHostHooks) {
    this.openPage = openPage;
    this.closePage = closePage;
    this.hostHooks = hostHooks;
  }

  open(page: SparklingPage, replace: boolean, animated: boolean): void {
    this.openPage(page, replace, animated);
  }

  close(containerID: string | undefined, animated: boolean): boolean {
    return this.closePage(containerID, animated);
  }

  hasRouterInterceptor(name: string): boolean {
    return this.hostHooks.hasRouterInterceptor(name);
  }

  shouldOpenRoute(name: string, scheme: string, extra: Record<string, Object>): boolean {
    return this.hostHooks.shouldOpenRoute(name, scheme, extra);
  }

  commonParams(): Record<string, Object> {
    return this.hostHooks.getCommonParams();
  }
}

export class SparklingPipeModule extends LynxModule {
  private mediaHandler: SparklingMediaHandler = new SparklingMediaHandler();

  call(method: string, request: PipeRequest, callback: PipeCallback): void {
    const handler = this.param as SparklingNavigationHandler;
    if (!isSparklingMethodLinked(method)) {
      callback({ code: -1, msg: `Method is not linked: ${method}. Install its package and run sparkling autolink.` });
      return;
    }
    if (dispatchSparklingMethod(getContext(this), method, request, callback)) {
      return;
    }

    switch (method) {
      case 'router.open':
        this.open(request, handler, callback);
        return;
      case 'router.close':
        this.close(request, handler, callback);
        return;
      case 'storage.setItem':
        this.setItem(request, callback);
        return;
      case 'storage.getItem':
        this.getItem(request, callback);
        return;
      case 'media.chooseMedia':
        this.mediaHandler.chooseMedia(getContext(this) as common.UIAbilityContext, request.data ?? {}, callback);
        return;
      case 'media.downloadFile':
        this.mediaHandler.downloadFile(getContext(this), request.data ?? {}, handler.commonParams(), callback);
        return;
      case 'media.saveDataURL':
        this.mediaHandler.saveDataURL(getContext(this), request.data ?? {}, callback);
        return;
      case 'media.uploadFile':
      case 'media.uploadImage':
        this.mediaHandler.uploadFile(getContext(this), request.data ?? {}, callback);
        return;
      default:
        callback({ code: -1, msg: `Unsupported method: ${method}` });
    }
  }

  private open(request: PipeRequest, handler: SparklingNavigationHandler, callback: PipeCallback): void {
    const data = request.data;
    const scheme = data?.scheme?.trim();
    if (!scheme) {
      callback({ code: -1, msg: 'router.open requires a scheme' });
      return;
    }

    if (data?.useSysBrowser === true) {
      const context = getContext(this) as common.UIAbilityContext;
      context.startAbility({ action: 'ohos.want.action.viewData', uri: scheme })
        .then((): void => callback({ code: 1, msg: 'ok' }))
        .catch((error: Error): void => callback({ code: 0, msg: `Failed to open system browser: ${error.message}` }));
      return;
    }

    const validReplaceTypes: string[] = [
      'alwaysCloseAfterOpen',
      'alwaysCloseBeforeOpen',
      'onlyCloseAfterOpenSucceed',
    ];
    const replaceType = data?.replaceType ?? 'onlyCloseAfterOpenSucceed';
    if (validReplaceTypes.indexOf(replaceType) < 0) {
      callback({ code: -1, msg: `Invalid replaceType: ${replaceType}` });
      return;
    }

    try {
      const parsed = url.URL.parseURL(scheme);
      const source = parsed.params.get('url') ?? parsed.params.get('bundle');
      if (!source) {
        callback({ code: -1, msg: 'The scheme must contain url or bundle' });
        return;
      }

      const queryItems: Record<string, Object> = {};
      Object.entries(data?.extra ?? {}).forEach((entry: [string, Object]): void => {
        queryItems[entry[0]] = entry[1];
      });
      parsed.params.forEach((value: string, key: string): void => {
        queryItems[key] = value;
      });
      const interceptor = data?.interceptor?.trim() ?? '';
      const extra = data?.extra ?? {};
      if (interceptor && !handler.hasRouterInterceptor(interceptor)) {
        callback({ code: -1, msg: `Router interceptor is not registered: ${interceptor}` });
        return;
      }
      if (interceptor && !handler.shouldOpenRoute(interceptor, scheme, extra)) {
        callback({ code: 0, msg: `Route was blocked by interceptor: ${interceptor}` });
        return;
      }
      handler.open(new SparklingPage(source, scheme, queryItems), data?.replace === true, data?.animated === true);
      callback({ code: 1, msg: 'ok' });
    } catch (error) {
      callback({ code: -1, msg: `Invalid navigation scheme: ${String(error)}` });
    }
  }

  private close(request: PipeRequest, handler: SparklingNavigationHandler, callback: PipeCallback): void {
    if (handler.close(request.data?.containerID, request.data?.animated === true)) {
      callback({ code: 1, msg: 'ok' });
    } else {
      callback({ code: 0, msg: 'Cannot close the requested page' });
    }
  }

  private setItem(request: PipeRequest, callback: PipeCallback): void {
    const data = request.data;
    const key = data?.key?.trim();
    if (!key || data?.data === undefined) {
      callback({ code: -1, msg: 'storage.setItem requires key and data' });
      return;
    }

    const duration = data.validDuration;
    if (duration !== undefined && duration < 0) {
      callback({ code: -1, msg: 'validDuration must be non-negative' });
      return;
    }

    const stored: StoredValue = {
      value: data.data,
      expiresAt: duration === undefined ? 0 : Date.now() + duration * 1000,
    };
    const context: Context = getContext(this);
    preferences.getPreferences(context, 'sparkling_storage')
      .then((store: preferences.Preferences): Promise<void> => store.put(this.storageKey(data.biz, key), JSON.stringify(stored))
        .then((): Promise<void> => store.flush()))
      .then((): void => callback({ code: 1, msg: 'ok' }))
      .catch((error: Error): void => callback({ code: 0, msg: `Failed to store item: ${error.message}` }));
  }

  private getItem(request: PipeRequest, callback: PipeCallback): void {
    const data = request.data;
    const key = data?.key?.trim();
    if (!key) {
      callback({ code: -1, msg: 'storage.getItem requires key' });
      return;
    }

    const context: Context = getContext(this);
    preferences.getPreferences(context, 'sparkling_storage')
      .then((store: preferences.Preferences): Promise<string> =>
        store.get(this.storageKey(data?.biz, key), '') as Promise<string>)
      .then((serialized: string): void => {
        if (!serialized) {
          const result: StorageResultData = {};
          callback({ code: 1, msg: 'ok', data: result });
          return;
        }
        const stored = JSON.parse(serialized) as StoredValue;
        if (stored.expiresAt > 0 && stored.expiresAt <= Date.now()) {
          preferences.getPreferences(context, 'sparkling_storage')
            .then((store: preferences.Preferences): Promise<void> => store.delete(this.storageKey(data?.biz, key))
              .then((): Promise<void> => store.flush()));
          const result: StorageResultData = {};
          callback({ code: 1, msg: 'ok', data: result });
          return;
        }
        const result: StorageResultData = { data: stored.value };
        callback({ code: 1, msg: 'ok', data: result });
      })
      .catch((error: Error): void => callback({ code: 0, msg: `Failed to read item: ${error.message}` }));
  }

  private storageKey(biz: string | undefined, key: string): string {
    return `${biz?.trim() ?? ''}:${key}`;
  }
}
