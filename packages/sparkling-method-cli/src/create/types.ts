// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
export type AndroidDsl = 'kts' | 'groovy';

export interface InitOptions {
  template?: string;
  force?: boolean;
  /** Namespace / bundle identifier (e.g. com.example). Skips the interactive prompt when provided. */
  packageName?: string;
  /** Module name in PascalCase (e.g. Storage). Skips the interactive prompt when provided. */
  moduleName?: string;
  /** Android Gradle DSL: 'kts' or 'groovy'. Skips the interactive prompt when provided. */
  androidDsl?: AndroidDsl;
}

export interface ModuleConfig {
  packageName: string;
  moduleName: string;
  projectName: string;
  androidDsl: AndroidDsl;
  harmony: {
    methodNames: string[];
    sourceDir: string;
    entry: string;
    className: string;
  };
}
