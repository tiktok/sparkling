// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import * as p from '@clack/prompts';
import deepmerge from 'deepmerge';
import fs from 'node:fs';
import path from 'node:path';

import { isEmptyDir } from '../../utils/common';
import { FileTemplater, type VariablesMap } from '../../utils/file-templater';

export class UserCancelledError extends Error {
  constructor(message = 'Operation cancelled by user') {
    super(message);
    this.name = 'UserCancelledError';
  }
}

export function checkCancel<T>(value: T | symbol): T {
  if (p.isCancel(value)) {
    throw new UserCancelledError();
  }
  return value as T;
}

export function formatProjectName(input: string): { packageName: string; targetDir: string } {
  if (!input || typeof input !== 'string') {
    throw new Error('Project name must be a non-empty string');
  }

  const formatted = input.trim().replaceAll(/\/+$|\/$/g, '');

  if (!formatted) {
    throw new Error('Project name cannot be empty or contain only whitespace');
  }

  const packageName = formatted.startsWith('@') ? formatted : path.basename(formatted);

  if (!packageName) {
    throw new Error('Could not derive package name from input');
  }

  return {
    packageName,
    targetDir: formatted,
  };
}

function sortObjectKeys(obj: Record<string, unknown>): Record<string, unknown> {
  const sorted = Object.keys(obj).sort();
  const result: Record<string, unknown> = {};
  for (const key of sorted) {
    result[key] = obj[key];
  }
  return result;
}

export function mergePackageJson(targetPackage: string, extraPackage: string): void {
  if (!targetPackage || typeof targetPackage !== 'string') {
    console.warn('mergePackageJson: targetPackage must be a non-empty string');
    return;
  }
  if (!extraPackage || typeof extraPackage !== 'string') {
    console.warn('mergePackageJson: extraPackage must be a non-empty string');
    return;
  }

  if (!fs.existsSync(targetPackage)) {
    return;
  }
  if (!fs.existsSync(extraPackage)) {
    console.warn(`mergePackageJson: extraPackage not found at ${extraPackage}`);
    return;
  }

  let targetJson: Record<string, unknown>;
  let extraJson: Record<string, unknown>;

  try {
    const targetContent = fs.readFileSync(targetPackage, 'utf8');
    targetJson = JSON.parse(targetContent);
  } catch (error) {
    console.warn(`mergePackageJson: Failed to parse target package.json: ${error instanceof Error ? error.message : String(error)}`);
    return;
  }

  try {
    const extraContent = fs.readFileSync(extraPackage, 'utf8');
    extraJson = JSON.parse(extraContent);
  } catch (error) {
    console.warn(`mergePackageJson: Failed to parse extra package.json: ${error instanceof Error ? error.message : String(error)}`);
    return;
  }

  const mergedJson = deepmerge(targetJson, extraJson) as Record<string, unknown>;

  mergedJson.name = targetJson.name || extraJson.name;

  for (const key of ['scripts', 'dependencies', 'devDependencies']) {
    if (Object.hasOwn(mergedJson, key) && mergedJson[key] && typeof mergedJson[key] === 'object') {
      mergedJson[key] = sortObjectKeys(mergedJson[key] as Record<string, unknown>);
    }
  }

  fs.writeFileSync(targetPackage, `${JSON.stringify(mergedJson, null, 2)}\n`);
}

export async function copyTemplateWithVariables({
  checkEmpty = true,
  from,
  isMergePackageJson,
  override = false,
  packageName,
  renameFiles = { gitignore: '.gitignore' },
  skipFiles = [],
  to,
  variables = {},
  version,
}: {
  checkEmpty?: boolean;
  from: string;
  isMergePackageJson?: boolean;
  override?: boolean;
  packageName?: string;
  renameFiles?: Record<string, string>;
  skipFiles?: string[];
  to: string;
  variables?: VariablesMap;
  version?: Record<string, string> | string;
}): Promise<void> {
  if (checkEmpty && !override && fs.existsSync(to) && !isEmptyDir(to)) {
    const option = await p.select({
      message: `"${path.basename(to)}" is not empty, please choose:`,
      options: [
        { label: 'Continue and override files', value: 'yes' },
        { label: 'Cancel operation', value: 'no' },
      ],
    });
    checkCancel(option);

    if (option === 'no') {
      throw new UserCancelledError();
    }
  }

  const allSkipFiles = new Set(['dist', 'node_modules', ...skipFiles]);

  fs.mkdirSync(to, { recursive: true });

  for (const file of fs.readdirSync(from)) {
    if (allSkipFiles.has(file)) {
      continue;
    }

    const srcFile = path.resolve(from, file);
    let distFileName = renameFiles[file] || file;

    for (const key in variables) {
      if (Object.hasOwn(variables, key)) {
        const placeholder = `{{${key.trim()}}}`;
        const regex = new RegExp(placeholder.replaceAll(/[.*+?^${}()|[\]\\]/g, String.raw`\$&`), 'g');
        distFileName = distFileName.replace(regex, String(variables[key]));
      }
    }

    const distFile = path.resolve(to, distFileName);
    const distDir = path.dirname(distFile);
    if (distDir !== to) {
      fs.mkdirSync(distDir, { recursive: true });
    }

    const stat = fs.statSync(srcFile);

    if (stat.isDirectory()) {
      await copyTemplateWithVariables({
        checkEmpty,
        from: srcFile,
        renameFiles,
        skipFiles,
        to: distFile,
        variables,
      });
    } else if (file === 'package.json') {
      const targetPackage = path.resolve(to, 'package.json');

      if (isMergePackageJson && fs.existsSync(targetPackage)) {
        mergePackageJson(targetPackage, srcFile);
      } else {
        fs.copyFileSync(srcFile, distFile);
        updatePackageJson(distFile, version, packageName);
      }
    } else {
      fs.copyFileSync(srcFile, distFile);

      if (Object.keys(variables).length > 0) {
        const isBinaryFile = await isBinary(distFile);

        if (!isBinaryFile) {
          try {
            await FileTemplater.replaceInFileAndUpdate(distFile, variables);
          } catch (error) {
            console.warn(`Failed to replace variables in file ${distFile}: ${error}`);
          }
        }
      }
    }
  }
}

const isStableVersion = (version: string) => ['alpha', 'beta', 'rc', 'canary', 'nightly'].every(
  (tag) => !version.includes(tag),
);

export function updatePackageJson(
  pkgJsonPath: string,
  version?: Record<string, string> | string,
  name?: string,
): void {
  let content = fs.readFileSync(pkgJsonPath, 'utf8');

  if (typeof version === 'string') {
    const targetVersion = isStableVersion(version) ? `^${version}` : version;
    content = content.replaceAll('workspace:*', targetVersion);
  }

  const pkg = JSON.parse(content);

  if (typeof version === 'object' && version !== null) {
    for (const [pkgName, ver] of Object.entries(version)) {
      if (pkg.dependencies?.[pkgName]) {
        pkg.dependencies[pkgName] = ver;
      }

      if (pkg.devDependencies?.[pkgName]) {
        pkg.devDependencies[pkgName] = ver;
      }
    }
  }

  if (name && name !== '.') {
    pkg.name = name;
  }

  fs.writeFileSync(pkgJsonPath, `${JSON.stringify(pkg, null, 2)}\n`);
}

async function isBinary(filePath: string): Promise<boolean> {
  try {
    const buffer = Buffer.alloc(512);
    const fd = await fs.promises.open(filePath, 'r');
    const { bytesRead } = await fd.read(buffer, 0, 512, 0);
    await fd.close();

    for (let i = 0; i < bytesRead; i++) {
      if (buffer[i] === 0) {
        return true;
      }
    }

    return false;
  } catch {
    return false;
  }
}
