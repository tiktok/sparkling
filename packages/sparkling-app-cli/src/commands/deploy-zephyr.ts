// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import { loadAppConfig } from '../config';
import type { AppConfig, ZephyrDeployTarget } from '../types';
import { ui } from '../utils/ui';
import { buildProject } from './build';
import { copyAssets } from './copy-assets';
import { writeZephyrRuntimeConfig } from './zephyr-runtime-config';
import { uploadOutputToZephyr } from 'zephyr-agent';

const ALLOWED_TARGETS: ZephyrDeployTarget[] = ['android', 'ios', 'web'];

export interface DeployZephyrOptions {
  cwd: string;
  configFile?: string;
  target?: string;
  outputDir?: string;
  skipBuild?: boolean;
}

function getConfiguredTargets(config: AppConfig): ZephyrDeployTarget[] {
  const targets = [
    config.zephyr?.android?.target,
    config.zephyr?.ios?.target,
  ].filter((target): target is ZephyrDeployTarget => Boolean(target));

  return [...new Set(targets)];
}

export function resolveZephyrDeployTarget(config: AppConfig, explicitTarget?: string): ZephyrDeployTarget {
  if (explicitTarget) {
    const normalized = explicitTarget.toLowerCase();
    if (ALLOWED_TARGETS.includes(normalized as ZephyrDeployTarget)) {
      return normalized as ZephyrDeployTarget;
    }
    throw new Error(`Unsupported Zephyr deploy target "${explicitTarget}". Use android, ios, or web.`);
  }

  const configuredTargets = getConfiguredTargets(config);
  if (configuredTargets.length === 1) {
    return configuredTargets[0];
  }

  if (configuredTargets.length > 1) {
    throw new Error('Multiple Zephyr targets configured. Pass --target android or --target ios.');
  }

  throw new Error('Missing Zephyr deploy target. Pass --target android|ios|web or configure app.config.ts.');
}

export async function deployToZephyr(options: DeployZephyrOptions): Promise<void> {
  const configFile = options.configFile ?? 'app.config.ts';
  const { config } = await loadAppConfig(options.cwd, configFile);
  const target = resolveZephyrDeployTarget(config, options.target);

  if (options.skipBuild !== true) {
    await buildProject({
      cwd: options.cwd,
      configFile,
      skipCopy: true,
    });
  }

  console.log(ui.headline(`Deploying dist to Zephyr (${target})`));
  const result = await uploadOutputToZephyr({
    rootDir: options.cwd,
    outputDir: options.outputDir ?? 'dist',
    builder: 'rspack',
    target,
    ssr: false,
  });

  writeZephyrRuntimeConfig({
    cwd: options.cwd,
    config,
    sourceDir: options.outputDir ?? 'dist',
    versionUrlOverride: config.zephyr?.versionUrl ?? result.deploymentUrl ?? undefined,
  });
  await copyAssets({
    cwd: options.cwd,
    source: options.outputDir ?? 'dist',
    androidDest: config.paths?.androidAssets ?? 'android/app/src/main/assets',
    iosDest: config.paths?.iosAssets ?? 'ios/LynxResources/Assets',
  });

  if (result.deploymentUrl) {
    console.log(ui.success(`✔ Zephyr deploy complete: ${result.deploymentUrl}`));
    return;
  }

  console.log(ui.success('✔ Zephyr deploy complete'));
}
