// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import path from 'path';

import { promptModuleInfo, promptProjectName } from './prompts';
import {
  copyTemplateDirectory,
  createPackageJson,
  ensurePlatformScaffolds,
  resolveTemplateDir,
  writeAndroidConfigs,
  writeHarmonyConfigs,
  writeIosConfigs,
  writeModuleConfig,
} from './scaffold';
import type { AndroidDsl, InitOptions, ModuleConfig } from './types';
import { toPascalCase } from './utils';
import { ui } from '../ui';
import { isVerboseEnabled, verboseLog } from '../verbose';

/**
 * Validate the androidDsl option value.
 * Throws if the value is not 'kts' or 'groovy'.
 */
function validateAndroidDsl(value: string): AndroidDsl {
  if (value !== 'kts' && value !== 'groovy') {
    throw new Error(`Invalid --android-dsl value "${value}". Must be "kts" or "groovy".`);
  }
  return value;
}

/**
 * Resolve module configuration.
 *
 * In **non-interactive mode** (when `--package-name` and `--module-name` are
 * both provided via CLI), all prompts are skipped and the values from CLI
 * options are used directly.
 *
 * In **interactive mode** (the default), the user is prompted for any values
 * not supplied via CLI options.
 */
async function resolveModuleConfig(
  defaults: { packageName: string; moduleName: string },
  options: InitOptions,
): Promise<Omit<ModuleConfig, 'projectName' | 'harmony'>> {
  const hasPackageName = options.packageName != null && options.packageName.trim() !== '';
  const hasModuleName = options.moduleName != null && options.moduleName.trim() !== '';

  // Non-interactive: all required module options are provided via CLI
  if (hasPackageName && hasModuleName) {
    const androidDsl = validateAndroidDsl(options.androidDsl ?? 'kts');
    return {
      packageName: options.packageName!.trim(),
      moduleName: options.moduleName!.trim(),
      androidDsl,
    };
  }

  // Interactive: fall through to prompts, using CLI values as defaults where provided
  const promptDefaults = {
    packageName: hasPackageName ? options.packageName!.trim() : defaults.packageName,
    moduleName: hasModuleName ? options.moduleName!.trim() : defaults.moduleName,
  };
  return promptModuleInfo(promptDefaults);
}

export async function runInit(projectName: string | undefined, options: InitOptions): Promise<void> {
  const normalizedProjectName = projectName?.trim() || await promptProjectName();
  const templateDir = await resolveTemplateDir(options.template);
  const targetDir = path.resolve(process.cwd(), normalizedProjectName);

  if (isVerboseEnabled()) {
    verboseLog(`init -> project: ${normalizedProjectName}, targetDir: ${targetDir}`);
    verboseLog(`init -> templateDir: ${templateDir}, force: ${options.force === true}`);
  }

  const moduleDefaults = {
    packageName: 'org.sparkling',
    moduleName: toPascalCase(normalizedProjectName),
  };
  const moduleConfig = await resolveModuleConfig(moduleDefaults, options);
  if (isVerboseEnabled()) {
    verboseLog(`init -> module config: package=${moduleConfig.packageName}, name=${moduleConfig.moduleName}, dsl=${moduleConfig.androidDsl}`);
  }

  console.log(ui.headline(`\n✨ Initializing sparkling method project in ${targetDir}`));
  await copyTemplateDirectory(templateDir, targetDir, options.force);
  await createPackageJson(normalizedProjectName, targetDir);

  const persisted: ModuleConfig = await writeModuleConfig(normalizedProjectName, moduleConfig, targetDir);
  await ensurePlatformScaffolds(persisted, targetDir);
  await writeAndroidConfigs(persisted, targetDir);
  await writeIosConfigs(persisted, targetDir);
  await writeHarmonyConfigs(persisted, targetDir);

  console.log(ui.success('\n✅ Project created successfully.'));
  console.log(ui.tip(`cd ${normalizedProjectName}`));
  console.log(ui.tip('Edit and rename src/method.d.ts to describe your APIs.'));
  console.log(ui.tip('Run `npm run codegen` to generate native stubs.'));
}
