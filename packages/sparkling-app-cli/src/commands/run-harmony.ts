// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import fg from 'fast-glob';

import { loadAppConfig } from '../config';
import { runCommand } from '../utils/exec';
import {
  resolveHarmonyJavaHome,
  resolveHarmonySdkHome,
  resolveHarmonyTool,
} from '../utils/harmony-tools';
import { ui } from '../utils/ui';
import { isVerboseEnabled, verboseLog } from '../utils/verbose';
import { buildProject } from './build';
import { autolink } from './autolink';

export interface RunHarmonyOptions {
  cwd: string;
  skipCopy?: boolean;
}

function resolveHvigor(harmonyDir: string): string {
  const localCommand = process.platform === 'win32' ? 'hvigorw.bat' : 'hvigorw';
  const localPath = path.join(harmonyDir, localCommand);
  return fs.existsSync(localPath) ? localPath : resolveHarmonyTool('hvigorw');
}

function findBuiltHap(harmonyDir: string): string | undefined {
  return fg
    .sync('entry/build/**/outputs/**/*.hap', { absolute: true, cwd: harmonyDir })
    .map(hap => ({
      hap,
      modifiedAt: fs.statSync(hap).mtimeMs,
      signed: !hap.endsWith('-unsigned.hap'),
    }))
    .sort((left, right) =>
      right.modifiedAt - left.modifiedAt || Number(right.signed) - Number(left.signed))
    .at(0)?.hap;
}

function connectedHarmonyTargets(hdc: string): string[] {
  try {
    return execFileSync(hdc, ['list', 'targets'], {
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe'],
      timeout: 15_000,
    })
      .split(/\r?\n/)
      .map(target => target.trim())
      .filter(target => target.length > 0 && target !== '[Empty]');
  } catch {
    return [];
  }
}

export async function runHarmony(options: RunHarmonyOptions): Promise<void> {
  const harmonyDir = path.resolve(options.cwd, 'harmony');
  if (!fs.existsSync(path.join(harmonyDir, 'build-profile.json5'))) {
    throw new Error(`HarmonyOS project not found at ${harmonyDir}`);
  }

  const { config } = await loadAppConfig(options.cwd);
  await autolink({ cwd: options.cwd, platform: 'harmony' });
  await buildProject({ cwd: options.cwd, skipCopy: options.skipCopy });
  await runCommand(resolveHarmonyTool('ohpm'), ['install'], { cwd: harmonyDir });

  const hvigor = resolveHvigor(harmonyDir);
  const hvigorArgs = [
    'assembleHap',
    '--mode',
    'module',
    '-p',
    'product=default',
    '-p',
    'module=entry@default',
    '-p',
    'buildMode=debug',
    '--no-daemon',
  ];
  if (isVerboseEnabled()) {
    verboseLog(`HarmonyOS project directory: ${harmonyDir}`);
    verboseLog(`Resolved Hvigor command: ${hvigor}`);
  }
  const sdkHome = resolveHarmonySdkHome();
  const javaHome = resolveHarmonyJavaHome();
  await runCommand(hvigor, hvigorArgs, {
    cwd: harmonyDir,
    env: {
      ...(sdkHome ? { DEVECO_SDK_HOME: sdkHome } : {}),
      ...(javaHome ? { JAVA_HOME: javaHome } : {}),
    },
  });

  const hapPath = findBuiltHap(harmonyDir);
  if (!hapPath) {
    console.log(ui.success('HarmonyOS debug build completed. Open harmony/ in DevEco Studio to install it.'));
    return;
  }

  const isUnsigned = hapPath.endsWith('-unsigned.hap');
  if (isUnsigned) {
    console.log(
      ui.tip('The HAP is unsigned. Emulators usually accept it; physical devices may require signing in DevEco Studio.'),
    );
  }

  const hdc = resolveHarmonyTool('hdc');
  if (connectedHarmonyTargets(hdc).length === 0) {
    console.log(ui.success(`HarmonyOS debug build ready: ${hapPath}`));
    console.log(
      ui.tip(
        'No HarmonyOS device found. Start an emulator or connect a device, then run pnpm run:harmony again.',
      ),
    );
    return;
  }

  console.log(ui.tip(`Installing ${path.relative(harmonyDir, hapPath)} on a connected device...`));
  try {
    await runCommand(hdc, ['install', '-r', hapPath], {
      cwd: harmonyDir,
    });
  } catch (error) {
    if (isUnsigned) {
      throw new Error(
        `Failed to install the unsigned HAP. Configure signing in DevEco Studio and retry. ${String(error)}`,
      );
    }
    throw error;
  }

  const bundleName = config.platform?.harmony?.bundleName;
  if (!bundleName) {
    console.log(ui.success('HarmonyOS app installed successfully.'));
    console.log(ui.tip('Set platform.harmony.bundleName in app.config.ts to launch it automatically.'));
    return;
  }

  await runCommand(
    hdc,
    ['shell', 'aa', 'start', '-a', 'EntryAbility', '-b', bundleName],
    { cwd: harmonyDir },
  );
  console.log(ui.success('HarmonyOS app installed and launched successfully.'));
}
