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
  web?: {
    port?: number;
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
  appName?: string;
  platform?: PlatformConfig;
  paths?: {
    androidAssets?: string;
    iosAssets?: string;
    webAssets?: string;
  };
  appIcon?: string;
  router?: RouterConfig;
  plugin?: PluginConfig[];
}

export interface MethodModuleConfig {
  name: string;
  root: string;
  platforms?: string[];
  android?: {
    packageName?: string;
    className?: string;
    projectDir?: string;
    buildGradle?: string;
  };
  ios?: {
    moduleName?: string;
    className?: string;
    podspecPath?: string;
  };
  web?: {
    entryPoint?: string;
    subpath?: string;
  };
}
