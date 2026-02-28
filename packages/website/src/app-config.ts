// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

/**
 * Configuration schema for `app.config.ts`, the top-level config file of a
 * Sparkling application.
 *
 * `app.config.ts` is a **superset** of the Lynx build config. It wraps the
 * standard Rspeedy / Lynx config (the same object you would export from a
 * `lynx.config.ts`) and adds Sparkling-specific fields such as platform
 * identifiers, asset paths, and native plugin settings.
 *
 * **Relationship to `lynx.config.ts`:**
 *
 * | | `app.config.ts` | `lynx.config.ts` |
 * |---|---|---|
 * | Used by | Sparkling CLI (`sparkling dev / build / run`) | Rspeedy directly |
 * | Contains | `lynxConfig` + Sparkling fields | Rspeedy build config only |
 * | When to use | Full Sparkling app with native shell | Lynx-only project (e.g. Playground) |
 *
 * At build time the Sparkling CLI extracts the `lynxConfig` property and
 * passes it to Rspeedy, so you never need a separate `lynx.config.ts` when
 * using `app.config.ts`.
 *
 * @example
 * ```ts
 * import { defineConfig } from '@lynx-js/rspeedy';
 * import type { AppConfig } from 'sparkling-app-cli';
 *
 * const lynxConfig = defineConfig({
 *   source: {
 *     entry: {
 *       main: './src/pages/main/index.tsx',
 *       detail: './src/pages/detail/index.tsx',
 *     },
 *   },
 *   output: {
 *     assetPrefix: 'asset:///',
 *     filename: { bundle: '[name].lynx.bundle' },
 *   },
 *   plugins: [ ... ],
 * });
 *
 * const config: AppConfig = {
 *   lynxConfig,
 *   appName: 'MyApp',
 *   platform: {
 *     android: { packageName: 'com.example.myapp' },
 *     ios: { bundleIdentifier: 'com.example.myapp' },
 *   },
 * };
 *
 * export default config;
 * ```
 */
export interface AppConfig {
  /**
   * The Rspeedy / Lynx build configuration — the same object returned by
   * `defineConfig()` from `@lynx-js/rspeedy`.
   *
   * This is the **only required field**. The Sparkling CLI extracts it at
   * build time and passes it to Rspeedy, so you do not need a separate
   * `lynx.config.ts` file.
   *
   * @remarks
   * Configure build entries, output settings, and Rspeedy plugins here.
   * See the [Rspeedy documentation](https://rspeedy.dev/) for the
   * full set of options.
   */
  lynxConfig: unknown;

  /**
   * Display name of the application.
   *
   * Used by the Sparkling CLI when scaffolding or displaying the app.
   */
  appName?: string;

  /** Platform-specific identifiers for Android and iOS builds. */
  platform?: PlatformConfig;

  /**
   * Paths where built bundles are copied into the native projects.
   *
   * After `sparkling build`, the CLI copies the output bundles to these
   * directories so the native apps can load them as local assets.
   *
   * @defaultValue `{ androidAssets: 'android/app/src/main/assets', iosAssets: 'ios/LynxResources/Assets' }`
   */
  paths?: {
    /** Path to the Android assets directory relative to the project root. */
    androidAssets?: string;
    /** Path to the iOS assets directory relative to the project root. */
    iosAssets?: string;
  };

  /** Path to the app icon image, relative to the project root. */
  appIcon?: string;

  /**
   * Static route entry mapping.
   *
   * Maps named routes to their bundle paths. This is available for metadata
   * purposes; at runtime, navigation is performed via the `navigate()`
   * function from `sparkling-navigation`.
   */
  router?: RouterConfig;

  /**
   * Native plugin configurations.
   *
   * Each entry is a tuple of `[pluginName, pluginOptions]`. The Sparkling
   * CLI and native SDKs read these to configure native-side features.
   *
   * @example
   * ```ts
   * plugin: [
   *   ['splash-screen', {
   *     backgroundColor: '#232323',
   *     image: './resource/icon.png',
   *     imageWidth: 200,
   *   }],
   * ]
   * ```
   */
  plugin?: PluginConfig[];
}

// ── Supporting types ──────────────────────────────────────────────────

/** Platform-specific identifiers and settings. */
export interface PlatformConfig {
  /** Android platform settings. */
  android?: {
    /**
     * Android application package name (e.g. `"com.example.myapp"`).
     *
     * Used by `sparkling autolink` to configure native dependencies.
     */
    packageName?: string;
  };
  /** iOS platform settings. */
  ios?: {
    /**
     * iOS bundle identifier (e.g. `"com.example.myapp"`).
     *
     * Used by `sparkling autolink` to configure native dependencies.
     */
    bundleIdentifier?: string;
    /**
     * iOS simulator identifier to use for `sparkling run:ios`.
     *
     * If not set, the CLI uses the default booted simulator.
     */
    simulator?: string;
  };
}

/** A single route entry pointing to a Lynx page bundle. */
export interface RouterEntry {
  /** Path to the Lynx page source or bundle. */
  path: string;
}

/**
 * Route configuration mapping.
 *
 * Keys are route names and values are {@link RouterEntry} objects.
 */
export interface RouterConfig {
  /** The main (initial) route of the application. */
  main?: RouterEntry;
  /** Additional named routes. */
  [name: string]: RouterEntry | undefined;
}

/**
 * Splash screen plugin configuration.
 *
 * Controls the native splash screen shown while the Lynx bundle loads.
 */
export interface SplashScreenPluginConfig {
  /** Background color of the splash screen (hex string, e.g. `"#FFFFFF"`). */
  backgroundColor?: string;
  /** Path to the splash image, relative to the project root. */
  image?: string;
  /** Display width of the splash image in logical points. */
  imageWidth?: number;
  /** Dark-mode overrides for the splash screen. */
  dark?: {
    /** Splash image for dark mode. */
    image?: string;
    /** Background color for dark mode. */
    backgroundColor?: string;
  };
}

/**
 * Plugin configuration tuple.
 *
 * Each plugin is specified as `[name, options?]`. The built-in
 * `"splash-screen"` plugin uses {@link SplashScreenPluginConfig};
 * custom plugins accept an arbitrary options object.
 */
export type PluginConfig =
  | ['splash-screen', SplashScreenPluginConfig]
  | [string, Record<string, unknown>?];
