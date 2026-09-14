export class SparklingHostHooks {
  hasRouterInterceptor(_name: string): boolean {
    return false;
  }

  shouldOpenRoute(_name: string, _scheme: string, _extra: Record<string, Object>): boolean {
    return false;
  }

  getCommonParams(): Record<string, Object> {
    return {};
  }
}
