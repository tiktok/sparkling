// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import type { Config } from '../core/config';

export interface CreateAppArgs {
  name?: string;
}

export interface CreateAppFlags {
  template?: string;
  templateVersion?: string;
  withZephyr?: boolean;
  yes?: boolean;
  force?: boolean;
  pm?: string;
  install?: boolean;
  git?: boolean;
  namespace?: string;
  'app-id'?: string;
  verbose?: boolean;
}

export interface CreateSparklingAppOptions {
  args: CreateAppArgs;
  flags: CreateAppFlags;
  cwd?: string;
}

export interface BuiltinTemplateInfo {
  canonicalName: string;
  path?: string;
  packageName?: string;
}

export interface TemplateSelectionConfig {
  template?: string;
  yes?: boolean;
}

export interface TemplateResolutionResult {
  templateFolder: string;
  selectedTemplateName: string;
  isCustomTemplate: boolean;
  customTemplatePath?: string | null;
}

export interface SparklingConfigOptions {
  androidDsl: 'kts' | 'groovy';
  additionalTools: string[];
  customTemplatePath?: string | null;
  devTools: string[];
  isCustomTemplate: boolean;
  packageManager?: string;
  packageName: string;
  selectedTemplateName: string;
  template: string;
  version?: string;
}

export type SparklingConfigBuilder = (options: SparklingConfigOptions) => Config;
