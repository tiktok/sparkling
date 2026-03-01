// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

/// <reference types="sparkling-types" />
import type { BaseEvent, StandardProps } from '@lynx-js/types';

declare module '@lynx-js/types' {
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
