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
  harmony?: {
    bundleName?: string;
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
    harmonyAssets?: string;
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
  harmony?: {
    methodNames?: string[];
    /** ArkTS source tree copied into the generated HarmonyOS application. */
    sourceDir?: string;
    /** Entry file relative to sourceDir that exports className. */
    entry?: string;
    /** Exported ArkTS handler class instantiated by the generated registry. */
    className?: string;
  };
}
