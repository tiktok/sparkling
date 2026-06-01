// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from 'node:fs';
import path from 'node:path';

const DEFAULT_ANDROID_NAMESPACE = 'com.example.sparkling.go';
const DEFAULT_IOS_BUNDLE_ID = 'com.sparkling.app.SparklingGo';
const DEFAULT_IOS_TEST_BUNDLE_ID = 'com.sparkling.app.SparklingGoTests';
const DEFAULT_IOS_UITEST_BUNDLE_ID = 'com.sparkling.app.SparklingGoUITests';
const DEFAULT_IOS_METHOD_TEST_BUNDLE_ID = 'com.sparkling.app.SparklingMethodTests';

export function deriveDefaultNamespace(packageName: string): string {
  const sanitized = packageName
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '.')
    .replace(/^\.+|\.+$/g, '')
    .replace(/\.+/g, '.');

  const finalName = sanitized.length > 0 ? sanitized : 'sparkling.app';
  return finalName.startsWith('com.') ? finalName : `com.${finalName}`;
}

export function isValidNamespace(namespace: string): boolean {
  return /^[a-zA-Z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+$/.test(namespace);
}

function replaceInTree(targetPath: string, replacements: Record<string, string>): void {
  if (!fs.existsSync(targetPath)) {
    return;
  }

  const stat = fs.statSync(targetPath);
  if (stat.isDirectory()) {
    for (const entry of fs.readdirSync(targetPath)) {
      replaceInTree(path.join(targetPath, entry), replacements);
    }
    return;
  }

  // Apply longer keys first so a shorter key that is a prefix of a longer one
  // (e.g. main bundle id vs `${bundleId}Tests`) does not mangle the longer one.
  const ordered = Object.entries(replacements)
    .filter(([from]) => from)
    .sort(([a], [b]) => b.length - a.length);

  let content = fs.readFileSync(targetPath, 'utf8');
  let updated = content;

  for (const [from, to] of ordered) {
    updated = updated.split(from).join(to);
  }

  if (updated !== content) {
    fs.writeFileSync(targetPath, updated, 'utf8');
  }
}

function removeEmptyParentDirs(startDir: string, stopDir: string): void {
  let current = startDir;
  const stop = path.resolve(stopDir);
  while (path.resolve(current) !== stop) {
    if (!fs.existsSync(current)) {
      current = path.dirname(current);
      continue;
    }
    const stat = fs.statSync(current);
    if (!stat.isDirectory()) {
      return;
    }
    if (fs.readdirSync(current).length > 0) {
      return;
    }
    fs.rmdirSync(current);
    current = path.dirname(current);
  }
}

function moveAndroidPackageDirs(appDir: string, oldNamespace: string, newNamespace: string): void {
  if (oldNamespace === newNamespace) {
    return;
  }

  const oldParts = oldNamespace.split('.');
  const newParts = newNamespace.split('.');
  const srcRoots = ['main', 'androidTest', 'test'];

  for (const root of srcRoots) {
    const javaRoot = path.join(appDir, 'src', root, 'java');
    const oldPath = path.join(javaRoot, ...oldParts);
    const newPath = path.join(javaRoot, ...newParts);

    if (!fs.existsSync(oldPath) || oldPath === newPath) {
      continue;
    }

    fs.mkdirSync(path.dirname(newPath), { recursive: true });
    fs.renameSync(oldPath, newPath);
    removeEmptyParentDirs(path.dirname(oldPath), javaRoot);
  }
}

function applyAndroidNamespace(projectDir: string, namespace: string): void {
  const appDir = path.join(projectDir, 'android', 'app');
  if (!fs.existsSync(appDir)) {
    return;
  }

  const replacements: Record<string, string> = {
    [DEFAULT_ANDROID_NAMESPACE]: namespace,
  };

  // `replaceInTree` walks the entire `app` source set (main, test, androidTest,
  // gradle/manifest files), so unit-test sources that import or declare the
  // default package get rewritten alongside production code.
  replaceInTree(appDir, replacements);
  moveAndroidPackageDirs(appDir, DEFAULT_ANDROID_NAMESPACE, namespace);
}

function applyIosBundleIdentifiers(projectDir: string, namespace: string): void {
  const pbxproj = path.join(projectDir, 'ios', 'SparklingGo.xcodeproj', 'project.pbxproj');
  if (!fs.existsSync(pbxproj)) {
    return;
  }

  // Order matters: longer / more specific test bundle ids must be replaced
  // before the shorter main bundle id (which is their prefix). `replaceInTree`
  // sorts replacements by key length descending, so we just declare them all here.
  const replacements: Record<string, string> = {
    [DEFAULT_IOS_BUNDLE_ID]: namespace,
    [DEFAULT_IOS_TEST_BUNDLE_ID]: `${namespace}.tests`,
    [DEFAULT_IOS_UITEST_BUNDLE_ID]: `${namespace}.uitests`,
    [DEFAULT_IOS_METHOD_TEST_BUNDLE_ID]: `${namespace}.methodtests`,
  };

  replaceInTree(pbxproj, replacements);
}

export function applyPackageNamespace(projectDir: string, namespace: string): void {
  applyAndroidNamespace(projectDir, namespace);
  applyIosBundleIdentifiers(projectDir, namespace);
}
