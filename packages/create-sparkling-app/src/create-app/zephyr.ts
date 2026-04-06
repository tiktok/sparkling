// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from 'node:fs';
import path from 'node:path';

function updatePackageJson(projectDir: string): void {
  const packageJsonPath = path.join(projectDir, 'package.json');
  if (!fs.existsSync(packageJsonPath)) return;

  const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8')) as {
    devDependencies?: Record<string, string>;
    scripts?: Record<string, string>;
  };

  packageJson.devDependencies ??= {};
  packageJson.scripts ??= {};

  packageJson.scripts['deploy:zephyr:ios'] ??=
    'sparkling-app-cli deploy:zephyr --target ios';
  packageJson.scripts['deploy:zephyr:android'] ??=
    'sparkling-app-cli deploy:zephyr --target android';

  fs.writeFileSync(packageJsonPath, `${JSON.stringify(packageJson, null, 2)}\n`);
}

function updateAppConfig(projectDir: string, packageName: string): void {
  const appConfigPath = path.join(projectDir, 'app.config.ts');
  if (!fs.existsSync(appConfigPath)) return;

  let source = fs.readFileSync(appConfigPath, 'utf8');

  if (!source.includes('zephyr: {')) {
    source = source.replace(
      "  router: {\n",
      `  zephyr: {\n    enabled: true,\n    appId: '${packageName}',\n    versionUrl: process.env.ZEPHYR_VERSION_URL,\n    strategy: 'next-launch',\n    fallback: 'bundled',\n    polling: {\n      enabled: true,\n      intervalMs: 15 * 60 * 1000,\n    },\n    ios: {\n      target: 'ios',\n    },\n    android: {\n      target: 'android',\n    },\n  },\n  router: {\n`,
    );
  }

  fs.writeFileSync(appConfigPath, source);
}

export function applyZephyrSupport(projectDir: string, packageName: string): void {
  updatePackageJson(projectDir);
  updateAppConfig(projectDir, packageName);
}
