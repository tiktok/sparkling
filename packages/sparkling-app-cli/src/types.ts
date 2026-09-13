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

/** One `<data>` element inside an Android intent filter. */
export interface AndroidIntentFilterData {
  scheme?: string;
  host?: string;
  port?: string;
  path?: string;
  pathPrefix?: string;
  pathPattern?: string;
  mimeType?: string;
}

export interface AndroidIntentFilter {
  /** Bare name (`VIEW`) or fully qualified (`android.intent.action.VIEW`). */
  action: string | string[];
  /** Bare names are prefixed with `android.intent.category.`. */
  categories?: string[];
  data?: AndroidIntentFilterData[];
  /** Defaults to the launcher activity. */
  activity?: string;
  autoVerify?: boolean;
}

export interface AndroidAppConfig {
  /** Bare names are prefixed with `android.permission.`. */
  permissions?: string[];
  intentFilters?: AndroidIntentFilter[];
}

export interface IosAppConfig {
  /** Sugar for a `CFBundleURLTypes` entry per scheme. */
  urlSchemes?: string[];
  /** Merged into the root dict of Info.plist. */
  infoPlist?: Record<string, unknown>;
  /** Merged into the root dict of the entitlements file. */
  entitlements?: Record<string, unknown>;
}

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
  /**
   * Native project configuration applied by `sparkling prebuild`.
   *
   * The native projects are source, not build output, so prebuild writes into
   * marked regions and leaves everything else alone. Removing a value here
   * removes it from the project on the next run.
   */
  android?: AndroidAppConfig;
  ios?: IosAppConfig;
  /**
   * Config plugins, applied in order after the declarative fields above.
   *
   * Each entry is a module name, a path relative to the project, or a function.
   * A tuple passes options: `['sparkling-plugin-x', { ... }]`.
   */
  plugins?: (string | [string, Record<string, unknown>] | ConfigPlugin)[];
  /** Enable sparkling-debug-tool integration. Defaults to true. Set to false to exclude the debug-tool module from all builds. */
  devtool?: boolean;
}

/**
 * A config plugin: given the config, register modifications and return it.
 *
 * Plugins do not write files. They add mods with the `withX` helpers, which run
 * in a fixed order per file after every plugin has been applied, so two plugins
 * touching the same file compose instead of racing.
 */
export type ConfigPlugin = (
  config: AppConfig,
  options?: Record<string, unknown>,
) => AppConfig | Promise<AppConfig>;

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
}
