// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import * as p from '@clack/prompts';
import { UserCancelledError, checkCancel } from '../core/project-builder/template';
import { CUSTOM_TEMPLATE_OPTION, DEFAULT_PROJECT_NAME, DEFAULT_TEMPLATE_NAME } from './constants';
import { DEFAULT_ANDROID_DSL, type AndroidDslChoice } from './android-dsl';
import { isValidNamespace } from './namespace';

export async function askProjectName(flags: { yes?: boolean }, defaultName = DEFAULT_PROJECT_NAME): Promise<string> {
  if (!flags.yes) {
    const projectName = await p.text({
      message: 'Project name',
      defaultValue: defaultName,
      placeholder: defaultName,
      validate(value) {
        if (!value || value.length === 0) return 'Project name is required';
      },
    });
    return checkCancel(projectName);
  }
  return defaultName;
}

export async function askTemplate(flags: { yes?: boolean }, initial?: string): Promise<string> {
  if (initial) return initial;
  if (!flags.yes) {
    const template = await p.select({
      message: 'Choose a template',
      options: [
        { value: DEFAULT_TEMPLATE_NAME, label: 'sparkling-default (Official Sparkling starter project)' },
        { value: CUSTOM_TEMPLATE_OPTION, label: 'Custom template (Local path, Git URL, or npm package)' },
      ],
    });
    return checkCancel(template);
  }
  return DEFAULT_TEMPLATE_NAME;
}

export async function askCustomTemplatePath(flags: { yes?: boolean }): Promise<string> {
  if (flags.yes) {
    throw new Error('Custom template requires interactive input or a direct --template path.');
  }
  const templatePath = await p.text({
    message: 'Enter custom template path, GitHub URL, or npm package',
    validate(value) {
      if (!value || value.length === 0) return 'Template path is required';
    },
  });
  return checkCancel(templatePath);
}

export async function askAndroidDsl(flags: { yes?: boolean }): Promise<AndroidDslChoice> {
  if (!flags.yes) {
    const androidDsl = await p.select({
      message: 'Android Gradle build files',
      options: [
        { value: 'kts' as const, label: 'Kotlin DSL (.gradle.kts)' },
        { value: 'groovy' as const, label: 'Groovy (.gradle)' },
      ],
    });
    return checkCancel(androidDsl) === 'groovy' ? 'groovy' : 'kts';
  }
  return DEFAULT_ANDROID_DSL;
}

export async function askDevTools(flags: { yes?: boolean }): Promise<string[]> {
  if (!flags.yes) {
    const devTools = await p.multiselect({
      message: 'Select development tools',
      required: false,
      options: [{ value: 'testing', label: 'Add ReactLynx Testing Library for unit testing' }],
    });
    return checkCancel(devTools);
  }
  return [];
}

export async function askAdditionalTools(flags: { yes?: boolean }): Promise<string[]> {
  if (!flags.yes) {
    const tools = await p.multiselect({
      message: 'Select optional tooling',
      required: false,
      options: [
        { value: 'eslint', label: 'ESLint (Standard linting configuration)' },
        { value: 'prettier', label: 'Prettier (Auto-formatting defaults)' },
        { value: 'biome', label: 'Biome (Biome + Biome formatter)' },
      ],
    });
    return checkCancel(tools);
  }
  return [];
}

export async function askNamespace(defaultNamespace: string, flags: { yes?: boolean; namespace?: string; ['app-id']?: string }): Promise<string> {
  const provided = flags.namespace ?? flags['app-id'];
  if (provided) return provided;
  if (!flags.yes) {
    const namespace = await p.text({
      message: 'Package namespace (Android package / iOS bundle id)',
      defaultValue: defaultNamespace,
      placeholder: defaultNamespace,
      validate(value) {
        if (!isValidNamespace(value)) return 'Use reverse-DNS format, e.g. com.example.app';
      },
    });
    return checkCancel(namespace);
  }
  return defaultNamespace;
}

export async function confirmInstall(packageManager: string, flags: { yes?: boolean; install?: boolean }): Promise<boolean> {
  if (flags.install !== undefined) return flags.install !== false;
  if (!flags.yes) {
    const installNow = await p.confirm({
      message: `Install JS dependencies now with ${packageManager}?`,
      initialValue: true,
    });
    return checkCancel(installNow);
  }
  return true;
}

export async function confirmInitGit(flags: { yes?: boolean; git?: boolean }): Promise<boolean> {
  if (flags.git !== undefined) return flags.git !== false;
  if (!flags.yes) {
    const initGit = await p.confirm({
      message: 'Initialize a git repository?',
      initialValue: true,
    });
    return checkCancel(initGit);
  }
  return true;
}

export async function confirmRemoveExistingDir(targetDir: string, flags: { yes?: boolean }): Promise<boolean> {
  if (flags.yes) return true;
  const removeDir = await p.confirm({
    message: `Target directory "${targetDir}" exists and is not empty. Remove existing files?`,
    initialValue: true,
  });
  return checkCancel(removeDir);
}
