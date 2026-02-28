// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import type { BaseEvent, StandardProps } from '@lynx-js/types';

declare module '@lynx-js/types' {
  /**
   * Global properties injected by the Sparkling native SDK into every Lynx
   * container. Access these at runtime via `lynx.__globalProps`.
   *
   * @see {@link https://tiktok.github.io/sparkling/apis/global-props/GlobalProps | Full documentation}
   */
  interface GlobalProps {
    // ── Device / Screen ──────────────────────────────────────────────

    /** Screen width in logical points (dp on Android, pt on iOS). */
    screenWidth: number;
    /** Screen height in logical points (dp on Android, pt on iOS). */
    screenHeight: number;
    /** Height of the system status bar in logical points. */
    statusBarHeight: number;
    /** Height of the system navigation bar in logical points. @platform Android */
    navigationBarHeight: number;
    /** Height of the top safe-area inset in logical points. @platform iOS */
    topHeight: number;
    /** Height of the bottom safe-area inset in logical points. @platform iOS */
    bottomHeight: number;
    /** Safe-area height. */
    safeAreaHeight: number;
    /** Available content height, excluding top and bottom safe areas. @platform iOS */
    contentHeight: number;
    /** Device pixel ratio (e.g. `2.0`, `3.0`). @platform Android */
    pixelRatio: number;

    // ── Device Info ──────────────────────────────────────────────────

    /** Operating system identifier: `"ios"` or `"android"`. */
    os: string;
    /** OS version string (e.g. `"17.4"`, `"14"`). */
    osVersion: string;
    /** Device model identifier in lowercase. */
    deviceModel: string;
    /** `1` if tablet, `0` otherwise. */
    isPad: number;
    /** `1` if iPhone X-series or later, `0` otherwise. @platform iOS */
    isIPhoneX: number;
    /** `1` if iPhone X-series or later, `0` otherwise. @platform iOS */
    isIPhoneXMax: number;

    // ── System State ─────────────────────────────────────────────────

    /** `1` if low-power mode enabled, `0` otherwise. */
    isLowPowerMode: number;
    /** `true` if the app is currently in the background. */
    isAppBackground: boolean;
    /** Accessibility mode bitmask. Bit 0: VoiceOver / TalkBack active. */
    accessibleMode: number;

    // ── Localization ─────────────────────────────────────────────────

    /** Preferred language code from the OS. */
    language: string;
    /** App-level language setting. @platform Android */
    appLanguage?: string;
    /** App-level locale setting. @platform Android */
    appLocale?: string;

    // ── UI State ─────────────────────────────────────────────────────

    /** Current screen orientation string. */
    screenOrientation: string;
    /** Numeric orientation: `0` = portrait, `1` = landscape. @platform Android */
    orientation?: number;
    /** Current UI theme: `"dark"` or `"light"`. */
    theme: string;
    /** User-preferred theme setting. */
    preferredTheme?: string;
    /** Whether the device has a screen notch or cutout. */
    isNotchScreen: boolean;

    // ── Container / Runtime ──────────────────────────────────────────

    /** Unique identifier for this Sparkling container instance. */
    containerID: string;
    /** Timestamp (as string) when the container was initialized. */
    containerInitTime: string;
    /** Version of the underlying Lynx SDK. @platform Android */
    lynxSdkVersion: string;
    /** Template resource data passed to the container. @platform Android */
    templateResData: string;
    /** Query parameters parsed from the container's scheme URL. @platform Android */
    queryItems: Record<string, string>;
  }

  interface IntrinsicElements extends Lynx.IntrinsicElements {
    input: InputProps;
  }
}

export interface InputProps extends StandardProps {
  /**
   * CSS class name for the input element
   */
  className?: string;

  value?: string;

  /**
   * Event handler for input changes
   */
  bindinput?: (e: InputEvent) => void;

  /**
   * Event handler for blur events
   */
  bindblur?: (e: BlurEvent) => void;

  /**
   * Placeholder text when input is empty
   */
  placeholder?: string;

  /**
   * Text color of the input
   */
  'text-color'?: string;
}

export type InputEvent = BaseEvent<'input', { value: string }>;

// 提供工作区内包到新命名包的类型声明映射，避免编辑器无法解析模块名
declare module 'sparkling-navigation' {
  export * from '../../methods/sparkling-navigation/dist';
}

declare module 'sparkling-storage' {
  export * from '../../methods/sparkling-storage/dist';
}
