// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from 'fs-extra';
import path from 'node:path';
import { relativeTo } from '../utils/paths';
import { isVerboseEnabled, verboseLog } from '../utils/verbose';
import { ui } from '../utils/ui';

export interface CopyAssetsOptions {
  source?: string;
  androidDest?: string;
  iosDest?: string;
  webDest?: string;
  /** Directory names to exclude from native (android/ios) copies (e.g., ['web']) */
  excludeDirs?: string[];
  cwd: string;
}

function copyDir(src: string, dest: string, excludeDirs?: string[]) {
  if (!fs.existsSync(src)) {
    console.warn(ui.warn(`Skip copy: missing source ${src}`));
    return;
  }
  fs.ensureDirSync(dest);

  if (excludeDirs?.length) {
    copyDirFiltered(src, dest, excludeDirs);
  } else {
    fs.cpSync(src, dest, { recursive: true, force: true, dereference: true });
  }
}

function copyDirFiltered(src: string, dest: string, excludeDirs: string[]) {
  const entries = fs.readdirSync(src, { withFileTypes: true });
  for (const entry of entries) {
    if (entry.isDirectory() && excludeDirs.includes(entry.name)) {
      if (isVerboseEnabled()) {
        verboseLog(`Excluding directory ${entry.name} from native copy`);
      }
      continue;
    }

    const srcPath = path.join(src, entry.name);
    const destPath = path.join(dest, entry.name);

    if (entry.isDirectory()) {
      copyDirFiltered(srcPath, destPath, excludeDirs);
    } else {
      fs.ensureDirSync(path.dirname(destPath));
      fs.copyFileSync(srcPath, destPath);
    }
  }
}

export async function copyAssets(options: CopyAssetsOptions): Promise<void> {
  const source = path.resolve(options.cwd, options.source ?? 'dist');
  const androidDest = path.resolve(options.cwd, options.androidDest ?? 'android/app/src/main/assets');
  const iosDest = path.resolve(options.cwd, options.iosDest ?? 'ios/LynxResources/Assets');
  if (isVerboseEnabled()) {
    verboseLog(`Copy assets source: ${source}`);
    verboseLog(`Android assets destination: ${androidDest}`);
    verboseLog(`iOS assets destination: ${iosDest}`);
    if (options.excludeDirs?.length) {
      verboseLog(`Excluding directories: ${options.excludeDirs.join(', ')}`);
    }
  }

  for (const dest of [androidDest, iosDest]) {
    console.log(ui.info(`Copying ${relativeTo(options.cwd, source)} -> ${relativeTo(options.cwd, dest)}`));
    copyDir(source, dest, options.excludeDirs);
  }

  if (options.webDest) {
    const webDest = path.resolve(options.cwd, options.webDest);
    const webSource = path.resolve(source, 'web');
    console.log(ui.info(`Copying ${relativeTo(options.cwd, webSource)} -> ${relativeTo(options.cwd, webDest)}`));
    copyDir(webSource, webDest);
  }

  console.log(ui.success('Assets copied successfully.'));
}
