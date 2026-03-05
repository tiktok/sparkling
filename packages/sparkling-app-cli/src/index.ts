#!/usr/bin/env node
// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import { Command } from 'commander';
import path from 'node:path';
import { autolink } from './commands/autolink';
import { buildProject } from './commands/build';
import type { BuildPlatform } from './commands/build';
import { copyAssets } from './commands/copy-assets';
import { devProject } from './commands/dev';
import type { DevPlatform } from './commands/dev';
import { doctor } from './commands/doctor';
import { runAndroid } from './commands/run-android';
import { runIos } from './commands/run-ios';
import { runWeb } from './commands/run-web';
import { ui } from './utils/ui';
import { enableVerboseLogging, isVerboseEnabled, verboseLog } from './utils/verbose';
import type { AppConfig } from './types';

const program = new Command();
program.name('sparkling-app-cli').description('Sparkling workspace helper CLI');
program.option('-v, --verbose', 'Enable verbose logging for debugging');
program.hook('preAction', (thisCommand) => {
  const opts = thisCommand.optsWithGlobals<{ verbose?: boolean }>();
  enableVerboseLogging(opts.verbose);
  if (isVerboseEnabled()) {
    verboseLog('Verbose logging enabled');
  }
});

/**
 * Resolve skipCopy option from --copy and --skip-copy flags
 * @param opts Command options
 * @returns Whether to skip copying
 */
function resolveSkipCopy(opts: { copy?: boolean; skipCopy?: boolean }): boolean {
  // If --copy is explicitly set, don't skip
  if (opts.copy) return false;
  // If --skip-copy is explicitly set, skip
  if (opts.skipCopy) return true;
  // Default: skip copy (recommended for development)
  return true;
}

const BUILD_PLATFORMS = ['android', 'ios', 'web', 'native', 'all'];
const DEV_PLATFORMS = ['web', 'native', 'all'];
const AUTOLINK_PLATFORMS = ['android', 'ios', 'web', 'all'];
const DOCTOR_PLATFORMS = ['android', 'ios', 'web', 'all'];

program
  .command('build')
  .description('Build Lynx bundle using app.config.ts (no-copy by default)')
  .option('--config <path>', 'Path to app.config.ts', 'app.config.ts')
  .option('--platform <platform>', 'Target platform: android|ios|web|native|all', 'all')
  .option('--copy', 'Copy assets to native shells')
  .option('--skip-copy', 'Skip copying assets to native shells')
  .action(async opts => {
    const cwd = process.cwd();
    const skipCopy = resolveSkipCopy(opts);
    const raw = String(opts.platform ?? 'all').toLowerCase();
    const platform = (BUILD_PLATFORMS.includes(raw) ? raw : 'all') as BuildPlatform;
    if (!BUILD_PLATFORMS.includes(raw)) {
      console.warn(ui.warn(`Unknown platform "${opts.platform}", defaulting to 'all'.`));
    }
    await buildProject({ cwd, configFile: opts.config, skipCopy, platform });
  });

program
  .command('dev')
  .description('Start Rspeedy dev server using app.config.ts')
  .option('--config <path>', 'Path to app.config.ts', 'app.config.ts')
  .option('--platform <platform>', 'Target platform: web|native|all', 'all')
  .option('--port <number>', 'Dev server port (default: 5969)', '5969')
  .action(async opts => {
    const cwd = process.cwd();
    const raw = String(opts.platform ?? 'all').toLowerCase();
    const platform = (DEV_PLATFORMS.includes(raw) ? raw : 'all') as DevPlatform;
    if (!DEV_PLATFORMS.includes(raw)) {
      console.warn(ui.warn(`Unknown platform "${opts.platform}", defaulting to 'all'.`));
    }
    await devProject({ cwd, configFile: opts.config, port: Number(opts.port), platform });
  });

program
  .command('copy-assets')
  .description('Copy dist assets into Android/iOS resource locations')
  .option('--source <path>', 'Path to compiled assets', 'dist')
  .option('--android-dest <path>', 'Android asset destination', 'android/app/src/main/assets')
  .option('--ios-dest <path>', 'iOS asset destination', 'ios/LynxResources/Assets')
  .option('--exclude-web', 'Exclude web/ subdirectory from native copies')
  .action(async opts => {
    const cwd = process.cwd();
    await copyAssets({
      cwd,
      source: opts.source,
      androidDest: opts.androidDest,
      iosDest: opts.iosDest,
      excludeDirs: opts.excludeWeb ? ['web'] : undefined,
    });
  });

program
  .command('autolink')
  .description('Autolink Sparkling method modules for Android, iOS, and Web')
  .option('--platform <platform>', 'Platform to autolink: android|ios|web|all', 'all')
  .action(async (opts) => {
    const cwd = process.cwd();
    const raw = String(opts.platform ?? 'all').toLowerCase();
    const platform = (AUTOLINK_PLATFORMS.includes(raw) ? raw : 'all') as 'android' | 'ios' | 'web' | 'all';
    if (!AUTOLINK_PLATFORMS.includes(raw)) {
      console.warn(ui.warn(`Unknown platform "${opts.platform}", defaulting to 'all'.`));
    }
    await autolink({ cwd, platform });
  });

program
  .command('run:android')
  .description('Build Lynx bundle, copy assets, and install Android debug build')
  .option('--copy', 'Copy assets to native shells')
  .option('--skip-copy', 'Skip copying assets to native shells')
  .action(async (opts) => {
    const cwd = process.cwd();
    const skipCopy = resolveSkipCopy(opts);
    await runAndroid({ cwd, skipCopy });
  });

program
  .command('run:ios')
  .description('Build Lynx bundle, copy assets, and launch iOS simulator build')
  .option('--copy', 'Copy assets to native shells')
  .option('--skip-copy', 'Skip copying assets to native shells')
  .option('--device <nameOrId>', 'Simulator name or UDID to run')
  .option('--skip-pod-install', 'Skip running pod install before building iOS')
  .action(async (opts) => {
    const cwd = process.cwd();
    const skipCopy = resolveSkipCopy(opts);
    await runIos({
      cwd,
      skipCopy,
      device: opts.device,
      skipPodInstall: opts.skipPodInstall,
    });
  });

program
  .command('run:web')
  .description('Build web bundles and launch in browser')
  .option('--config <path>', 'Path to app.config.ts', 'app.config.ts')
  .option('--port <number>', 'Web server port (default: 4200)', '4200')
  .option('--no-open', 'Do not auto-open browser')
  .action(async (opts) => {
    const cwd = process.cwd();
    await runWeb({
      cwd,
      configFile: opts.config,
      port: Number(opts.port),
      open: opts.open,
    });
  });

program
  .command('doctor')
  .description('Check if your environment is ready to build a Sparkling app')
  .option('--platform <platform>', 'Platform to check: android|ios|web|all', 'all')
  .action(async (opts) => {
    const raw = String(opts.platform ?? 'all').toLowerCase();
    const platform = (DOCTOR_PLATFORMS.includes(raw) ? raw : 'all') as 'android' | 'ios' | 'web' | 'all';
    if (!DOCTOR_PLATFORMS.includes(raw)) {
      console.warn(ui.warn(`Unknown platform "${opts.platform}", defaulting to 'all'.`));
    }
    await doctor({ platform });
  });

program.parse(process.argv);

export type { AppConfig };
