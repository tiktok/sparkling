import {
  LynxGenericResourceFetcher,
  LynxMediaResourceFetcher,
  LynxTemplateResourceFetcher,
  LynxView,
  MetaData,
  ModuleClassWrapper,
  TemplateData,
} from '@lynx/lynx';
import { common } from '@kit.AbilityKit';
import { deviceInfo } from '@kit.BasicServicesKit';
import { display, window } from '@kit.ArkUI';
import { i18n, resourceManager } from '@kit.LocalizationKit';

import { SparklingNavigationHandler, SparklingPage, SparklingPipeModule } from '../bridge/SparklingPipeModule';
import { SparklingHostHooks } from '../bridge/SparklingHostHooks';
import { SparklingGenericResourceFetcher } from '../provider/SparklingGenericResourceFetcher';
import { SparklingMediaResourceFetcher } from '../provider/SparklingMediaResourceFetcher';
import { SparklingTemplateResourceFetcher } from '../provider/SparklingTemplateResourceFetcher';

@Entry
@Component
struct Index {
  @State private pages: SparklingPage[] = [SparklingPage.root('main.lynx.bundle')];
  @State private statusBarHeight: number = 0;
  @State private navigationBarHeight: number = 0;
  @State private hasDisplayCutout: boolean = false;
  private templateFetcher: LynxTemplateResourceFetcher = new SparklingTemplateResourceFetcher();
  private mediaFetcher: LynxMediaResourceFetcher = new SparklingMediaResourceFetcher(getContext(this));
  private genericFetcher: LynxGenericResourceFetcher = new SparklingGenericResourceFetcher(getContext(this));
  private hostHooks: SparklingHostHooks = new SparklingHostHooks();
  private navigationHandler: SparklingNavigationHandler = new SparklingNavigationHandler(
    (page: SparklingPage, replace: boolean, animated: boolean): void => this.openPage(page, replace, animated),
    (containerID: string | undefined, animated: boolean): boolean => this.closePage(containerID, animated),
    this.hostHooks,
  );
  private modules: Map<string, ModuleClassWrapper> = this.createModules();

  private createModules(): Map<string, ModuleClassWrapper> {
    const modules = new Map<string, ModuleClassWrapper>();
    const pipeModule: ModuleClassWrapper = {
      moduleClass: SparklingPipeModule,
      param: this.navigationHandler,
    };
    modules.set('spkPipe', pipeModule);
    return modules;
  }

  private openPage(page: SparklingPage, replace: boolean, animated: boolean): void {
    animateTo({ duration: animated ? 250 : 0, curve: Curve.EaseOut }, (): void => {
      this.pages = replace ? [...this.pages.slice(0, -1), page] : [...this.pages, page];
    });
    this.applyPageWindow(page);
  }

  private closePage(containerID?: string, animated: boolean = false): boolean {
    if (containerID) {
      const index = this.pages.findIndex((page: SparklingPage): boolean => page.id === containerID);
      if (index <= 0) {
        return false;
      }
      animateTo({ duration: animated ? 250 : 0, curve: Curve.EaseOut }, (): void => {
        this.pages = this.pages.filter((_: SparklingPage, pageIndex: number): boolean => pageIndex !== index);
      });
      this.applyPageWindow(this.pages[this.pages.length - 1]);
      return true;
    }
    if (this.pages.length <= 1) {
      return false;
    }
    animateTo({ duration: animated ? 250 : 0, curve: Curve.EaseOut }, (): void => {
      this.pages = this.pages.slice(0, -1);
    });
    this.applyPageWindow(this.pages[this.pages.length - 1]);
    return true;
  }

  private pageMetaData(page: SparklingPage): MetaData {
    const globalProps: Record<string, Object> = {};
    const currentDisplay = display.getDefaultDisplaySync();
    const density = currentDisplay.densityPixels;
    const screenWidth = currentDisplay.width / density;
    const screenHeight = currentDisplay.height / density;
    const theme = (getContext(this) as common.UIAbilityContext)
      .resourceManager.getConfigurationSync().colorMode === resourceManager.ColorMode.DARK
      ? 'dark'
      : 'light';
    globalProps['queryItems'] = page.queryItems;
    globalProps['originUrl'] = page.scheme;
    globalProps['fullUrl'] = page.scheme;
    globalProps['platform'] = 'harmony';
    globalProps['os'] = 'harmony';
    globalProps['osVersion'] = deviceInfo.osFullName;
    globalProps['deviceModel'] = deviceInfo.productModel;
    globalProps['language'] = i18n.System.getSystemLanguage();
    globalProps['screenWidth'] = screenWidth;
    globalProps['screenHeight'] = screenHeight;
    globalProps['statusBarHeight'] = this.statusBarHeight;
    globalProps['navigationBarHeight'] = this.navigationBarHeight;
    globalProps['topHeight'] = this.statusBarHeight;
    globalProps['bottomHeight'] = this.navigationBarHeight;
    globalProps['safeAreaHeight'] = this.statusBarHeight;
    globalProps['contentHeight'] = Math.max(0, screenHeight - this.statusBarHeight - this.navigationBarHeight);
    globalProps['pixelRatio'] = density;
    globalProps['screenOrientation'] = currentDisplay.width <= currentDisplay.height ? 'Portrait' : 'Landscape';
    globalProps['orientation'] = currentDisplay.width <= currentDisplay.height ? 0 : 1;
    globalProps['isPad'] = currentDisplay.width / density >= 600 ? 1 : 0;
    globalProps['isIPhoneX'] = 0;
    globalProps['isIPhoneXMax'] = 0;
    globalProps['isLowPowerMode'] = 0;
    globalProps['isAppBackground'] = false;
    globalProps['accessibleMode'] = 0;
    globalProps['theme'] = theme;
    globalProps['isNotchScreen'] = this.hasDisplayCutout;
    globalProps['containerID'] = page.id;
    globalProps['containerInitTime'] = page.createdAt;
    globalProps['lynxSdkVersion'] = '3.8.1';
    globalProps['sparklingVersion'] = '{{version}}';
    globalProps['templateResData'] = '{}';
    return new MetaData(new TemplateData({}), new TemplateData(globalProps));
  }

  private queryString(page: SparklingPage, key: string, fallback: string): string {
    const value = page.queryItems[key];
    return value === undefined ? fallback : String(value);
  }

  private queryFlag(page: SparklingPage, key: string): boolean {
    const value = this.queryString(page, key, '').toLowerCase();
    return value === '1' || value === 'true' || value === 'yes';
  }

  private themedQueryString(page: SparklingPage, key: string, fallback: string): string {
    const theme = this.queryString(page, 'force_theme_style', '').toLowerCase();
    if (theme === 'dark' || theme === 'light') {
      return this.queryString(page, `${key}_${theme}`, this.queryString(page, key, fallback));
    }
    return this.queryString(page, key, fallback);
  }

  private applyPageWindow(page: SparklingPage): void {
    const context = getContext(this) as common.UIAbilityContext;
    window.getLastWindow(context).then((appWindow: window.Window): void => {
      const density = display.getDefaultDisplaySync().densityPixels;
      const systemArea = appWindow.getWindowAvoidArea(window.AvoidAreaType.TYPE_SYSTEM);
      const navigationArea = appWindow.getWindowAvoidArea(window.AvoidAreaType.TYPE_NAVIGATION_INDICATOR);
      const cutoutArea = appWindow.getWindowAvoidArea(window.AvoidAreaType.TYPE_CUTOUT);
      this.statusBarHeight = systemArea.visible ? systemArea.topRect.height / density : 0;
      this.navigationBarHeight = navigationArea.visible
        ? navigationArea.bottomRect.height / density
        : systemArea.visible ? systemArea.bottomRect.height / density : 0;
      this.hasDisplayCutout = cutoutArea.visible && cutoutArea.topRect.height > 0;
      const hideStatusBar = this.queryFlag(page, 'hide_status_bar');
      appWindow.setWindowSystemBarEnable(hideStatusBar ? ['navigation'] : ['status', 'navigation']);
      const statusBarColor = this.queryFlag(page, 'trans_status_bar')
        ? '#00000000'
        : this.themedQueryString(page, 'nav_bar_color', '#FFFFFF');
      appWindow.setWindowSystemBarProperties({ statusBarColor });
      const orientation = this.queryString(page, 'screen_orientation', '').toLowerCase();
      if (orientation === 'portrait') {
        appWindow.setPreferredOrientation(window.Orientation.PORTRAIT);
      } else if (orientation === 'landscape') {
        appWindow.setPreferredOrientation(window.Orientation.LANDSCAPE);
      } else {
        appWindow.setPreferredOrientation(window.Orientation.UNSPECIFIED);
      }
    });
  }

  aboutToAppear(): void {
    this.applyPageWindow(this.pages[this.pages.length - 1]);
  }

  private showNavigationBar(page: SparklingPage): boolean {
    return !this.queryFlag(page, 'hide_nav_bar');
  }

  onBackPress(): boolean {
    return this.closePage(undefined, true);
  }

  build() {
    Stack() {
      ForEach(this.pages, (page: SparklingPage, index: number) => {
        Column() {
          if (this.showNavigationBar(page)) {
            Row() {
              if (!this.queryFlag(page, 'hide_back_button')) {
                Button('<')
                  .type(ButtonType.Normal)
                  .fontSize(24)
                  .fontColor(this.themedQueryString(page, 'title_color', '#111111'))
                  .backgroundColor(Color.Transparent)
                  .width(56)
                  .height(56)
                  .onClick((): void => {
                    this.closePage();
                  });
              } else {
                Blank().width(56);
              }
              Text(this.queryString(page, 'title', ''))
                .fontSize(18)
                .fontWeight(FontWeight.Medium)
                .fontColor(this.themedQueryString(page, 'title_color', '#111111'))
                .maxLines(1)
                .textOverflow({ overflow: TextOverflow.Ellipsis })
                .layoutWeight(1);
              Blank().width(56);
            }
              .width('100%')
              .height(56)
              .backgroundColor(this.themedQueryString(page, 'nav_bar_color', '#FFFFFF'));
          }
          LynxView({
            templateResourceFetcher: this.templateFetcher,
            mediaResourceFetcher: this.mediaFetcher,
            genericResourceFetcher: this.genericFetcher,
            modules: this.modules,
            metaData: this.pageMetaData(page),
            url: page.source,
          })
            .width('100%')
            .layoutWeight(1)
            .backgroundColor(this.themedQueryString(page, 'container_bg_color', '#FFFFFF'));
        }
          .width('100%')
          .height('100%')
          .backgroundColor(this.themedQueryString(page, 'container_bg_color', '#FFFFFF'))
          .visibility(index === this.pages.length - 1 ? Visibility.Visible : Visibility.Hidden);
      }, (page: SparklingPage): string => page.id);
    }
      .width('100%')
      .height('100%');
  }
}
