// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from 'fs-extra';
import path from 'path';

import type { ModuleConfig } from './types';
import { androidGradleTemplate, androidManifestTemplate, iosPodspecTemplate, iosUnitTestTemplate } from './templates';
import { normalizePackageName, toPascalCase } from './utils';
import { isVerboseEnabled, verboseLog } from '../verbose';

const DEFAULT_TEMPLATE_DIR = path.resolve(__dirname, './template');

export async function resolveTemplateDir(customTemplate?: string): Promise<string> {
  const templateDir = customTemplate
    ? path.resolve(process.cwd(), customTemplate)
    : DEFAULT_TEMPLATE_DIR;

  if (!await fs.pathExists(templateDir)) {
    throw new Error(`Template directory not found at ${templateDir}`);
  }

  if (isVerboseEnabled()) {
    verboseLog(`Using template directory: ${templateDir}`);
  }
  return templateDir;
}

export async function copyTemplateDirectory(templateDir: string, targetDir: string, force?: boolean): Promise<void> {
  const exists = await fs.pathExists(targetDir);
  if (exists) {
    if (!force) {
      throw new Error(`Directory ${targetDir} already exists. Use --force to overwrite.`);
    }
    if (isVerboseEnabled()) {
      verboseLog(`Removing existing directory ${targetDir} due to --force`);
    }
    await fs.remove(targetDir);
  }

  if (isVerboseEnabled()) {
    verboseLog(`Copying template from ${templateDir} to ${targetDir}`);
  }
  await fs.copy(templateDir, targetDir);
}

export async function createPackageJson(projectName: string, targetDir: string): Promise<void> {
  const pkgPath = path.join(targetDir, 'package.json');
  const normalized = normalizePackageName(projectName);
  const packageJson = {
    name: normalized,
    version: '0.1.0',
    private: true,
    scripts: {
      build: 'tsc',
      codegen: 'npx sparkling-method-cli codegen',
      test: 'echo "Add your tests"',
    },
    files: ['dist', 'android', 'ios', 'generated'],
    devDependencies: {},
    dependencies: {},
  };
  await fs.writeJson(pkgPath, packageJson, { spaces: 2 });
  if (isVerboseEnabled()) {
    verboseLog(`Wrote package.json to ${pkgPath}`);
  }
}

export async function ensurePlatformScaffolds(config: ModuleConfig, projectDir: string): Promise<void> {
  const androidRoot = path.join(projectDir, 'android', 'src', 'main', 'java');
  const packagePath = config.packageName.split('.').filter(Boolean).join(path.sep);
  const moduleSegment = config.moduleName.replace(/\s+/g, '').toLowerCase();
  const androidDir = path.join(androidRoot, packagePath, moduleSegment);
  await fs.ensureDir(androidDir);

  const iosDir = path.join(projectDir, 'ios', 'Source', 'Core', moduleSegment);
  await fs.ensureDir(iosDir);
  if (isVerboseEnabled()) {
    verboseLog(`Ensured platform scaffolds at ${androidDir} and ${iosDir}`);
  }
}

export async function writeAndroidConfigs(config: ModuleConfig, projectDir: string): Promise<void> {
  const androidDir = path.join(projectDir, 'android');
  const manifestDir = path.join(androidDir, 'src', 'main');
  await fs.ensureDir(manifestDir);

  const manifestPath = path.join(manifestDir, 'AndroidManifest.xml');
  if (!await fs.pathExists(manifestPath)) {
    await fs.writeFile(manifestPath, androidManifestTemplate(config.packageName), 'utf8');
  }

  const moduleSegment = config.moduleName.replace(/\s+/g, '');
  const useKotlinDsl = config.androidDsl === 'kts';
  const gradleFileName = useKotlinDsl ? 'build.gradle.kts' : 'build.gradle';
  const gradlePath = path.join(androidDir, gradleFileName);
  const alternatePath = path.join(androidDir, useKotlinDsl ? 'build.gradle' : 'build.gradle.kts');

  if (await fs.pathExists(alternatePath)) {
    await fs.remove(alternatePath);
  }

  if (!await fs.pathExists(gradlePath)) {
    const namespace = `${config.packageName}.${moduleSegment.toLowerCase()}`;
    await fs.writeFile(gradlePath, androidGradleTemplate(namespace, config.androidDsl), 'utf8');
  }

  if (isVerboseEnabled()) {
    verboseLog(`Android config written: manifest=${manifestPath}, gradle=${gradlePath}`);
  }
}

export async function writeIosConfigs(config: ModuleConfig, projectDir: string): Promise<void> {
  const iosDir = path.join(projectDir, 'ios');
  await fs.ensureDir(iosDir);
  const moduleId = toPascalCase(config.moduleName);
  const podspecName = `Sparkling-${moduleId}.podspec`;
  const podspecPath = path.join(iosDir, podspecName);
  if (!await fs.pathExists(podspecPath)) {
    await fs.writeFile(podspecPath, iosPodspecTemplate(moduleId), 'utf8');
  }
  const testsDir = path.join(iosDir, 'SparklingMethodTests');
  await fs.ensureDir(testsDir);
  const testPath = path.join(testsDir, `SPK${moduleId}IndependentTests.swift`);
  if (!await fs.pathExists(testPath)) {
    await fs.writeFile(testPath, iosUnitTestTemplate(moduleId), 'utf8');
  }
  if (isVerboseEnabled()) {
    verboseLog(`iOS podspec ensured at ${podspecPath}`);
  }
}

export async function writeModuleConfig(projectName: string, config: Omit<ModuleConfig, 'projectName'>, dir: string): Promise<ModuleConfig> {
  const resolved: ModuleConfig = {
    ...config,
    projectName,
  };

  await fs.writeJson(path.join(dir, 'module.config.json'), resolved, { spaces: 2 });
  if (isVerboseEnabled()) {
    verboseLog(`module.config.json written with project ${projectName} at ${dir}`);
  }
  return resolved;
}
