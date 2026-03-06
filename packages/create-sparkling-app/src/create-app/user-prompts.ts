// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import inquirer from 'inquirer';
import { UserCancelledError, checkCancel } from '../core/project-builder/template';
import { ui } from '../ui';
import { CUSTOM_TEMPLATE_OPTION, DEFAULT_PROJECT_NAME, DEFAULT_TEMPLATE_NAME } from './constants';
import { DEFAULT_ANDROID_DSL, type AndroidDslChoice } from './android-dsl';
import { isValidNamespace } from './namespace';

function handlePromptError(error: unknown): never {
  if (error instanceof Error) {
    throw new UserCancelledError(error.message);
  }
  throw new UserCancelledError();
}

export async function askProjectName(flags: { yes?: boolean }, defaultName = DEFAULT_PROJECT_NAME): Promise<string> {
  if (!flags.yes) {
    try {
      const { projectName } = await inquirer.prompt<{ projectName: string }>([
        {
          type: 'input',
          name: 'projectName',
          message: ui.prompt('Project name'),
          default: defaultName,
          transformer(value: string) {
            const next = value && value.length > 0 ? value : defaultName;
            return next;
          },
          validate(value: string) {
            if (!value || value.length === 0) return 'Project name is required';
            return true;
          },
        },
      ]);
      return checkCancel(projectName);
    } catch (error) {
      handlePromptError(error);
    }
  }
  return defaultName;
}

export async function askTemplate(flags: { yes?: boolean }, initial?: string): Promise<string> {
  if (initial) return initial;
  if (!flags.yes) {
    try {
      const { template } = await inquirer.prompt<{ template: string }>([
        {
          type: 'list',
          name: 'template',
          message: ui.prompt('Choose a template'),
          choices: [
            { value: DEFAULT_TEMPLATE_NAME, name: 'sparkling-default (Official Sparkling starter project)' },
            { value: CUSTOM_TEMPLATE_OPTION, name: 'Custom template (Local path, Git URL, or npm package)' },
          ],
        },
      ]);
      return checkCancel(template);
    } catch (error) {
      handlePromptError(error);
    }
  }
  return DEFAULT_TEMPLATE_NAME;
}

export async function askCustomTemplatePath(flags: { yes?: boolean }): Promise<string> {
  if (flags.yes) {
    throw new Error('Custom template requires interactive input or a direct --template path.');
  }
  try {
    const { templatePath } = await inquirer.prompt<{ templatePath: string }>([
      {
        type: 'input',
        name: 'templatePath',
        message: ui.prompt('Enter custom template path, GitHub URL, or npm package'),
        default: '',
        validate(value: string) {
          if (!value || value.length === 0) return 'Template path is required';
          return true;
        },
      },
    ]);
    return checkCancel(templatePath);
  } catch (error) {
    handlePromptError(error);
  }
}

export async function askAndroidDsl(flags: { yes?: boolean }): Promise<AndroidDslChoice> {
  if (!flags.yes) {
    try {
      const { androidDsl } = await inquirer.prompt<{ androidDsl: string }>([
        {
          type: 'list',
          name: 'androidDsl',
          message: ui.prompt('Android Gradle build files'),
          choices: [
            { value: 'kts', name: 'Kotlin DSL (.gradle.kts)' },
            { value: 'groovy', name: 'Groovy (.gradle)' },
          ],
        },
      ]);
      return checkCancel(androidDsl) === 'groovy' ? 'groovy' : 'kts';
    } catch (error) {
      handlePromptError(error);
    }
  }
  return DEFAULT_ANDROID_DSL;
}

export async function askDevTools(flags: { yes?: boolean }): Promise<string[]> {
  if (!flags.yes) {
    try {
      const { devTools } = await inquirer.prompt<{ devTools: string[] }>([
        {
          type: 'checkbox',
          name: 'devTools',
          message: ui.prompt('Select development tools'),
          choices: [{ value: 'testing', name: 'Add ReactLynx Testing Library for unit testing' }],
        },
      ]);
      return checkCancel(devTools);
    } catch (error) {
      handlePromptError(error);
    }
  }
  return [];
}

export async function askAdditionalTools(flags: { yes?: boolean }): Promise<string[]> {
  if (!flags.yes) {
    try {
      const { tools } = await inquirer.prompt<{ tools: string[] }>([
        {
          type: 'checkbox',
          name: 'tools',
          message: ui.prompt('Select optional tooling'),
          choices: [
            { value: 'eslint', name: 'ESLint (Standard linting configuration)' },
            { value: 'prettier', name: 'Prettier (Auto-formatting defaults)' },
            { value: 'biome', name: 'Biome (Biome + Biome formatter)' },
          ],
        },
      ]);
      return checkCancel(tools);
    } catch (error) {
      handlePromptError(error);
    }
  }
  return [];
}

export async function askWebPlatform(flags: { yes?: boolean; web?: boolean }): Promise<boolean> {
  if (flags.web !== undefined) return flags.web;
  if (!flags.yes) {
    try {
      const { enableWeb } = await inquirer.prompt<{ enableWeb: boolean }>([
        {
          type: 'confirm',
          name: 'enableWeb',
          message: ui.prompt('Enable web platform support? (preview in browser)'),
          default: true,
        },
      ]);
      return checkCancel(enableWeb);
    } catch (error) {
      handlePromptError(error);
    }
  }
  return true;
}

export async function askNamespace(defaultNamespace: string, flags: { yes?: boolean; namespace?: string; ['app-id']?: string }): Promise<string> {
  const provided = flags.namespace ?? flags['app-id'];
  if (provided) return provided;
  if (!flags.yes) {
    try {
      const { namespace } = await inquirer.prompt<{ namespace: string }>([
        {
          type: 'input',
          name: 'namespace',
          message: ui.prompt('Package namespace (Android package / iOS bundle id)'),
          default: defaultNamespace,
          validate(value: string) {
            if (!isValidNamespace(value)) return 'Use reverse-DNS format, e.g. com.example.app';
            return true;
          },
        },
      ]);
      return checkCancel(namespace);
    } catch (error) {
      handlePromptError(error);
    }
  }
  return defaultNamespace;
}

export async function confirmInstall(packageManager: string, flags: { yes?: boolean; install?: boolean }): Promise<boolean> {
  if (flags.install !== undefined) return flags.install !== false;
  if (!flags.yes) {
    try {
      const { installNow } = await inquirer.prompt<{ installNow: boolean }>([
        {
          type: 'confirm',
          name: 'installNow',
          message: ui.prompt(`Install JS dependencies now with ${packageManager}?`),
          default: true,
        },
      ]);
      return checkCancel(installNow);
    } catch (error) {
      handlePromptError(error);
    }
  }
  return true;
}

export async function confirmInitGit(flags: { yes?: boolean; git?: boolean }): Promise<boolean> {
  if (flags.git !== undefined) return flags.git !== false;
  if (!flags.yes) {
    try {
      const { initGit } = await inquirer.prompt<{ initGit: boolean }>([
        {
          type: 'confirm',
          name: 'initGit',
          message: ui.prompt('Initialize a git repository?'),
          default: true,
        },
      ]);
      return checkCancel(initGit);
    } catch (error) {
      handlePromptError(error);
    }
  }
  return true;
}

export async function confirmRemoveExistingDir(targetDir: string, flags: { yes?: boolean }): Promise<boolean> {
  if (flags.yes) return true;
  try {
    const { removeDir } = await inquirer.prompt<{ removeDir: boolean }>([
      {
        type: 'confirm',
        name: 'removeDir',
        message: ui.prompt(`Target directory "${targetDir}" exists and is not empty. Remove existing files?`),
        default: true,
      },
    ]);
    return checkCancel(removeDir);
  } catch (error) {
    handlePromptError(error);
  }
}
