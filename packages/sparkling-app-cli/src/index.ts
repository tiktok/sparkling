#!/usr/bin/env node
// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import { Command } from 'commander';
import path from 'node:path';
import { autolink } from './commands/autolink';
import { buildProject } from './commands/build';
import { copyAssets } from './commands/copy-assets';
import { devProject } from './commands/dev';
import { doctor } from './commands/doctor';
import { runAndroid } from './commands/run-android';
import { runIos } from './commands/run-ios';
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

program
  .command('build')
  .description('Build Lynx bundle using app.config.ts (no-copy by default)')
  .option('--config <path>', 'Path to app.config.ts', 'app.config.ts')
  .option('--copy', 'Copy assets to native shells')
  .option('--skip-copy', 'Skip copying assets to native shells')
  .action(async opts => {
    const cwd = process.cwd();
    const skipCopy = resolveSkipCopy(opts);
    await buildProject({ cwd, configFile: opts.config, skipCopy });
  });

program
  .command('dev')
  .description('Start Rspeedy dev server using app.config.ts')
  .option('--config <path>', 'Path to app.config.ts', 'app.config.ts')
  .option('--port <number>', 'Dev server port (default: 5969)', '5969')
  .action(async opts => {
    const cwd = process.cwd();
    await devProject({ cwd, configFile: opts.config, port: Number(opts.port) });
  });

program
  .command('copy-assets')
  .description('Copy dist assets into Android/iOS resource locations')
  .option('--source <path>', 'Path to compiled assets', 'dist')
  .option('--android-dest <path>', 'Android asset destination', 'android/app/src/main/assets')
  .option('--ios-dest <path>', 'iOS asset destination', 'ios/LynxResources/Assets')
  .action(async opts => {
    const cwd = process.cwd();
    await copyAssets({
      cwd,
      source: opts.source,
      androidDest: opts.androidDest,
      iosDest: opts.iosDest,
    });
  });

program
  .command('autolink')
  .description('Autolink Sparkling method modules for Android and iOS')
  .option('--platform <platform>', 'Platform to autolink: android|ios|all', 'all')
  .action(async (opts) => {
    const cwd = process.cwd();
    const raw = String(opts.platform ?? 'all').toLowerCase();
    const allowed = ['android', 'ios', 'all'];
    const platform = (allowed.includes(raw) ? raw : 'all') as 'android' | 'ios' | 'all';
    if (!allowed.includes(raw)) {
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
  .command('doctor')
  .description('Check if your environment is ready to build a Sparkling app')
  .option('--platform <platform>', 'Platform to check: android|ios|all', 'all')
  .action(async (opts) => {
    const raw = String(opts.platform ?? 'all').toLowerCase();
    const allowed = ['android', 'ios', 'all'];
    const platform = (allowed.includes(raw) ? raw : 'all') as 'android' | 'ios' | 'all';
    if (!allowed.includes(raw)) {
      console.warn(ui.warn(`Unknown platform "${opts.platform}", defaulting to 'all'.`));
    }
    await doctor({ platform });
  });

program.parse(process.argv);

export type { AppConfig };
