// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import * as p from '@clack/prompts';

import type { ModuleConfig } from './types';
import { normalizePackageName } from './utils';

function checkCancel<T>(value: T | symbol): T {
  if (p.isCancel(value)) {
    process.exit(0);
  }
  return value as T;
}

export async function promptProjectName(): Promise<string> {
  const projectName = await p.text({
    message: 'Project name (directory / package name):',
    defaultValue: 'sparkling-method-module',
    placeholder: 'sparkling-method-module',
    validate: (input) => (!input.trim() ? 'Project name is required.' : undefined),
  });

  return normalizePackageName(checkCancel(projectName));
}

export async function promptModuleInfo(
  defaults: { packageName: string; moduleName: string },
): Promise<Omit<ModuleConfig, 'projectName'>> {
  const packageName = await p.text({
    message: 'Namespace / bundle identifier (e.g. com.example):',
    defaultValue: defaults.packageName,
    placeholder: defaults.packageName,
    validate: (input) => (!input.trim() ? 'Package name is required.' : undefined),
  });
  checkCancel(packageName);

  const moduleName = await p.text({
    message: 'Module name (PascalCase, e.g. Storage):',
    defaultValue: defaults.moduleName,
    placeholder: defaults.moduleName,
    validate: (input) => (!/[A-Za-z]/.test(input) ? 'Module name must contain letters.' : undefined),
  });
  checkCancel(moduleName);

  const androidDsl = await p.select({
    message: 'Android Gradle DSL:',
    options: [
      { value: 'kts' as const, label: 'Kotlin (.gradle.kts)' },
      { value: 'groovy' as const, label: 'Groovy (.gradle)' },
    ],
  });
  checkCancel(androidDsl);

  return {
    packageName: (packageName as string).trim(),
    moduleName: (moduleName as string).trim(),
    androidDsl: (androidDsl as 'kts' | 'groovy') ?? 'kts',
  };
}
