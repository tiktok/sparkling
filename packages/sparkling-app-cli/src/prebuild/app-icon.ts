// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import path from 'node:path';
import fs from 'fs-extra';

import type { AppConfig } from '../types';
import { Bitmap, decodePng, encodePng, flatten, resize } from './png';

/**
 * Generate the launcher icon from `appIcon`.
 *
 * `appIcon` has been declared in AppConfig and read by no code, so the template
 * pointed at an icon that was never applied and every scaffolded app shipped the
 * stock Android robot and no iOS icon at all. This is the plugin that makes the
 * field mean something.
 */

/** Android launcher icon sizes, in density buckets. */
const ANDROID_DENSITIES: [string, number][] = [
  ['mdpi', 48],
  ['hdpi', 72],
  ['xhdpi', 96],
  ['xxhdpi', 144],
  ['xxxhdpi', 192],
];

/** iOS wants one 1024 master; the system derives the rest. */
const IOS_SIZE = 1024;

export interface AppIconResult {
  written: string[];
  warnings: string[];
}

function roundedMask(bitmap: Bitmap): Bitmap {
  const { width, height } = bitmap;
  const data = Buffer.from(bitmap.data);
  const cx = (width - 1) / 2;
  const cy = (height - 1) / 2;
  const radius = Math.min(width, height) / 2;

  for (let y = 0; y < height; y += 1) {
    for (let x = 0; x < width; x += 1) {
      const dx = x - cx;
      const dy = y - cy;
      const distance = Math.sqrt(dx * dx + dy * dy);
      // One pixel of feathering, so the circle does not stair-step.
      const coverage = Math.min(1, Math.max(0, radius - distance + 0.5));
      const index = (y * width + x) * 4;
      data[index + 3] = Math.round(data[index + 3] * coverage);
    }
  }
  return { width, height, data };
}

/**
 * Write the launcher icons for both platforms.
 *
 * Returns rather than throws when the source is unusable: a broken icon should
 * fail the command with a sentence that names the file, not a stack trace from
 * a PNG decoder.
 */
export async function generateAppIcons(
  cwd: string,
  config: AppConfig,
  platform: 'android' | 'ios' | 'all',
  /** Compute what would be written without writing it, for `--check`. */
  dryRun = false,
): Promise<AppIconResult> {
  const written: string[] = [];
  const warnings: string[] = [];

  const put = async (file: string, contents: Buffer | string): Promise<void> => {
    const relative = path.relative(cwd, file);
    const current = fs.existsSync(file) ? await fs.readFile(file) : null;
    const next = Buffer.isBuffer(contents) ? contents : Buffer.from(contents, 'utf8');
    if (current && current.equals(next)) {
      return;
    }
    written.push(relative);
    if (!dryRun) {
      await fs.ensureDir(path.dirname(file));
      await fs.writeFile(file, next);
    }
  };

  const drop = async (file: string): Promise<void> => {
    if (!fs.existsSync(file)) {
      return;
    }
    written.push(`${path.relative(cwd, file)} (removed)`);
    if (!dryRun) {
      await fs.remove(file);
    }
  };

  if (!config.appIcon) {
    return { written, warnings };
  }

  const source = path.resolve(cwd, config.appIcon);
  if (!fs.existsSync(source)) {
    return { written, warnings: [`appIcon ${config.appIcon} does not exist`] };
  }

  let master: Bitmap;
  try {
    master = decodePng(await fs.readFile(source));
  } catch (error) {
    return { written, warnings: [`appIcon ${config.appIcon}: ${(error as Error).message}`] };
  }

  if (master.width !== master.height) {
    warnings.push(
      `appIcon ${config.appIcon} is ${master.width}x${master.height}; a square image is expected ` +
      'and it will be squashed to fit',
    );
  }
  if (master.width < IOS_SIZE) {
    warnings.push(
      `appIcon ${config.appIcon} is ${master.width}px; 1024px is what the App Store wants and ` +
      'anything smaller is scaled up',
    );
  }

  if (platform === 'android' || platform === 'all') {
    const resDir = path.resolve(cwd, 'android/app/src/main/res');
    if (fs.existsSync(resDir)) {
      for (const [density, size] of ANDROID_DENSITIES) {
        const scaled = resize(master, size, size);
        const dir = path.join(resDir, `mipmap-${density}`);

        await put(path.join(dir, 'ic_launcher.png'), encodePng(scaled));
        await put(path.join(dir, 'ic_launcher_round.png'), encodePng(roundedMask(scaled)));
      }
      // A webp from the template would otherwise win over the png we just wrote.
      for (const [density] of ANDROID_DENSITIES) {
        for (const name of ['ic_launcher.webp', 'ic_launcher_round.webp']) {
          await drop(path.join(resDir, `mipmap-${density}`, name));
        }
      }
    } else {
      warnings.push('No android/app/src/main/res directory; Android icons were skipped');
    }
  }

  if (platform === 'ios' || platform === 'all') {
    const iconset = path.resolve(cwd, 'ios/SparklingGo/SparklingGo/Assets.xcassets/AppIcon.appiconset');
    if (fs.existsSync(path.dirname(iconset))) {
      // The App Store rejects an icon with an alpha channel, so the master is
      // composited onto white before it is written.
      const scaled = flatten(resize(master, IOS_SIZE, IOS_SIZE), [255, 255, 255]);
      await put(path.join(iconset, 'AppIcon-1024.png'), encodePng(scaled));
      await put(path.join(iconset, 'Contents.json'), iosContentsJson());
    } else {
      warnings.push('No ios Assets.xcassets directory; iOS icons were skipped');
    }
  }

  return { written, warnings };
}

/**
 * The asset catalogue entry.
 *
 * The template's Contents.json declared three appearances and gave none of them
 * a filename, which is an icon set Xcode reads as empty. One universal 1024
 * image is what a single-appearance app needs; dark and tinted are opt-in and
 * are left to the developer to add.
 */
function iosContentsJson(): string {
  return `${JSON.stringify(
    {
      images: [
        { filename: 'AppIcon-1024.png', idiom: 'universal', platform: 'ios', size: '1024x1024' },
      ],
      info: { author: 'sparkling', version: 1 },
    },
    null,
    2,
  )}\n`;
}
