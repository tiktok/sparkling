// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
// Avoid hard dependency on external type packages in library types; 
// use a minimal alias for Lynx config shape.

export interface PlatformConfig {
  android?: {
    packageName?: string;
  };
  ios?: {
    bundleIdentifier?: string;
    simulator?: string;
  };
}

export type LynxConfig = unknown;

export interface RouterEntry {
  path: string;
}

export interface RouterConfig {
  main?: RouterEntry;
  [name: string]: RouterEntry | undefined;
}

export interface SplashScreenPluginConfig {
  backgroundColor?: string;
  image?: string;
  imageWidth?: number;
  dark?: {
    image?: string;
    backgroundColor?: string;
  };
}

export type PluginConfig =
  | ['splash-screen', SplashScreenPluginConfig]
  | [string, Record<string, unknown>?];

export interface AppConfig {
  lynxConfig: LynxConfig;
  /** Sparkling CLI dev settings. */
  dev?: {
    server?: {
      /** Preferred dev server port for sparkling-app-cli dev/run commands. Defaults to 5969. */
      port?: number;
      /** Preferred dev server host for sparkling-app-cli dev/run commands. */
      host?: string;
    };
  };
  appName?: string;
  platform?: PlatformConfig;
  paths?: {
    androidAssets?: string;
    iosAssets?: string;
  };
  appIcon?: string;
  router?: RouterConfig;
  plugin?: PluginConfig[];
  /** Enable sparkling-debug-tool integration. Defaults to true. Set to false to exclude the debug-tool module from all builds. */
  devtool?: boolean;
}

export interface MethodModuleConfig {
  name: string;
  root: string;
  /** When true, the module is a devtool module: linked with debugImplementation on Android and excluded from release on iOS. */
  devtool?: boolean;
  /**
   * Node-API addon libraries this module ships, as basenames without the `lib`
   * prefix or the file extension.
   *
   * Lynx hosts a Node-API environment on the background JS thread and PrimJS
   * implements it, but nothing loads an addon's shared library: it is built and
   * packaged and then never opened, so `getNapiLoader().load(name)` finds
   * nothing. Naming the addons here lets autolink generate the load sequence,
   * which has an order that is not guessable - see `nodeApiLibraries` in the
   * generated registry.
   */
  nodeApiAddons?: string[];
  android?: {
    packageName?: string;
    className?: string;
    methodClassNames?: string[];
    mavenDependency?: string;
    projectDir?: string;
    buildGradle?: string;
  };
  ios?: {
    moduleName?: string;
    className?: string;
    podspecPath?: string;
  };
}
