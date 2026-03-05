// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import path from 'node:path';
import { createTempLynxConfig, loadAppConfig } from '../config';
import { runCommand } from '../utils/exec';
import { ui } from '../utils/ui';
import { isVerboseEnabled, verboseLog } from '../utils/verbose';
import { copyAssets } from './copy-assets';

export type BuildPlatform = 'android' | 'ios' | 'web' | 'native' | 'all';

export interface BuildOptions {
  cwd: string;
  configFile?: string;
  skipCopy?: boolean;
  platform?: BuildPlatform;
}

export async function buildProject(options: BuildOptions): Promise<void> {
  const configPath = path.resolve(options.cwd, options.configFile ?? 'app.config.ts');
  const tempConfigPath = createTempLynxConfig(options.cwd, configPath);
  const platform = options.platform ?? 'all';

  if (isVerboseEnabled()) {
    verboseLog(`App config path: ${configPath}`);
    verboseLog(`Temp Lynx config: ${tempConfigPath}`);
    verboseLog(`Build platform: ${platform}`);
  }

  const rspeedyArgs = ['build', '--config', tempConfigPath];

  if (platform === 'web') {
    rspeedyArgs.push('--environment', 'web');
  } else if (platform === 'android' || platform === 'ios' || platform === 'native') {
    rspeedyArgs.push('--environment', 'lynx');
  }
  // platform === 'all' → no --environment flag → builds all environments

  console.log(ui.headline(`Building ${platform} bundle(s) with config from ${path.relative(options.cwd, configPath)}`));
  await runCommand('rspeedy', rspeedyArgs, { cwd: options.cwd });

  // Skip asset copy for web-only builds
  const shouldCopy = platform !== 'web' && options.skipCopy !== true;
  if (shouldCopy) {
    const { config } = await loadAppConfig(options.cwd, options.configFile ?? 'app.config.ts');
    await copyAssets({
      cwd: options.cwd,
      androidDest: config.paths?.androidAssets ?? 'android/app/src/main/assets',
      iosDest: config.paths?.iosAssets ?? 'ios/LynxResources/Assets',
      // When building all environments, exclude the web/ subdirectory from native copies
      excludeDirs: platform === 'all' ? ['web'] : undefined,
    });
  } else if (isVerboseEnabled()) {
    verboseLog(platform === 'web'
      ? 'Skipping asset copy for web-only build.'
      : 'Skipping asset copy because --skip-copy is in effect.');
  }
}
