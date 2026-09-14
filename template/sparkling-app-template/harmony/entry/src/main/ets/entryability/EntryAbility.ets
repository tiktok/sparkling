import { AbilityConstant, ConfigurationConstant, UIAbility, Want } from '@kit.AbilityKit';
import { window } from '@kit.ArkUI';
import { LLog, LynxEnv, LynxServiceCenter, LynxServiceType } from '@lynx/lynx';
import { LynxBaseServiceCenter, LynxBaseServiceType } from '@lynx/lynx_base';
import { LynxDevToolService } from '@lynx/lynx_devtool_service';
import { LynxHttpService } from '@lynx/lynx_http_service';
import { LynxLogService } from '@lynx/lynx_log_service';

export default class EntryAbility extends UIAbility {
  onCreate(_want: Want, _launchParam: AbilityConstant.LaunchParam): void {
    this.context.getApplicationContext().setColorMode(
      ConfigurationConstant.ColorMode.COLOR_MODE_NOT_SET,
    );
    LynxServiceCenter.registerService(LynxServiceType.DevTool, LynxDevToolService.instance);
    LynxServiceCenter.registerService(LynxServiceType.Http, LynxHttpService.instance);
    LynxBaseServiceCenter.registerService(LynxBaseServiceType.Log, LynxLogService.instance);

    LLog.useSysLog(true);
    LynxEnv.initialize(this.context);
    const appInfo = new Map<string, string>();
    appInfo.set('App', '{{appNameCamel}}');
    appInfo.set('AppVersion', '1.0.0');
    LynxEnv.setAppInfo(appInfo);
    LynxEnv.enableDevtool(true);
  }

  onWindowStageCreate(windowStage: window.WindowStage): void {
    windowStage.loadContent('pages/Index');
  }
}
