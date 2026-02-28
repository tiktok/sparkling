// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

/**
 * Global properties injected by the Sparkling native SDK into every Lynx
 * container. Access these at runtime via `lynx.__globalProps`.
 *
 * The native layer (iOS / Android) populates these values before the Lynx
 * bundle starts executing. Some fields are **stable** (set once on app
 * launch) and some are **unstable** (may change per container or over time).
 */
export interface GlobalProps {
  // ── Device / Screen ────────────────────────────────────────────────

  /** Screen width in logical points (dp on Android, pt on iOS). */
  screenWidth: number;

  /** Screen height in logical points (dp on Android, pt on iOS). */
  screenHeight: number;

  /** Height of the system status bar in logical points. */
  statusBarHeight: number;

  /**
   * Height of the system navigation bar in logical points.
   * @platform Android
   */
  navigationBarHeight: number;

  /**
   * Height of the top safe-area inset in logical points.
   * Equivalent to `safeAreaInsets.top` on iOS.
   * @platform iOS
   */
  topHeight: number;

  /**
   * Height of the bottom safe-area inset in logical points.
   * Equivalent to `safeAreaInsets.bottom` on iOS.
   * @platform iOS
   */
  bottomHeight: number;

  /**
   * Safe-area height. On iOS this equals `safeAreaInsets.top`;
   * on Android it is derived from screen metrics.
   */
  safeAreaHeight: number;

  /**
   * Available content height, excluding top and bottom safe areas.
   * Computed as `screenHeight - topHeight - bottomHeight`.
   * @platform iOS
   */
  contentHeight: number;

  /**
   * Device pixel ratio (e.g. `2.0`, `3.0`).
   * @platform Android
   */
  pixelRatio: number;

  // ── Device Info ────────────────────────────────────────────────────

  /**
   * Operating system identifier.
   * - `"ios"` on iOS
   * - `"android"` on Android
   */
  os: string;

  /** OS version string (e.g. `"17.4"`, `"14"`). */
  osVersion: string;

  /** Device model identifier in lowercase (e.g. `"iphone15,2"`, `"pixel 8"`). */
  deviceModel: string;

  /** `1` if the device is a tablet (iPad / Android tablet), `0` otherwise. */
  isPad: number;

  /**
   * `1` if the device is an iPhone X-series (or later with notch / Dynamic Island),
   * `0` otherwise. Always `0` on Android.
   * @platform iOS
   */
  isIPhoneX: number;

  /**
   * `1` if the device is an iPhone X-series (or later with notch / Dynamic Island),
   * `0` otherwise. Always `0` on Android.
   * @remarks Identical to {@link isIPhoneX} in current implementations.
   * @platform iOS
   */
  isIPhoneXMax: number;

  // ── System State ───────────────────────────────────────────────────

  /** `1` if the device is in low-power / battery-saver mode, `0` otherwise. */
  isLowPowerMode: number;

  /** `true` if the app is currently in the background. */
  isAppBackground: boolean;

  /**
   * Accessibility mode bitmask.
   * - Bit 0 (`1`): VoiceOver (iOS) or TalkBack (Android) is active.
   */
  accessibleMode: number;

  // ── Localization ───────────────────────────────────────────────────

  /** Preferred language code from the OS (e.g. `"en-US"`, `"zh-Hans"`). */
  language: string;

  /**
   * App-level language setting.
   * @platform Android
   */
  appLanguage?: string;

  /**
   * App-level locale setting.
   * @platform Android
   */
  appLocale?: string;

  // ── UI State ───────────────────────────────────────────────────────

  /**
   * Current screen orientation as a string.
   *
   * Possible values: `"Portrait"`, `"PortraitUpsideDown"`, `"LandscapeLeft"`,
   * `"LandscapeRight"`, `"Landscape"`, `"Unknown"`.
   */
  screenOrientation: string;

  /**
   * Numeric orientation indicator.
   * `0` = portrait, `1` = landscape.
   * @platform Android
   */
  orientation?: number;

  /** Current UI theme: `"dark"` or `"light"`. */
  theme: string;

  /**
   * User-preferred theme setting, if different from the current applied theme.
   * May be `undefined` if no preference has been set.
   */
  preferredTheme?: string;

  /**
   * Whether the device has a screen notch or cutout.
   */
  isNotchScreen: boolean;

  // ── Container / Runtime ────────────────────────────────────────────

  /** Unique identifier for this Sparkling container instance. */
  containerID: string;

  /** Timestamp (as string) when the container was initialized. */
  containerInitTime: string;

  /**
   * Version of the underlying Lynx SDK.
   * @platform Android
   */
  lynxSdkVersion: string;

  /**
   * Template resource data passed to the container.
   * Contents depend on the app's template configuration.
   * @platform Android
   */
  templateResData: string;

  /**
   * Query parameters parsed from the container's scheme URL.
   * Each key-value pair corresponds to a query parameter.
   * @platform Android
   */
  queryItems: Record<string, string>;
}
