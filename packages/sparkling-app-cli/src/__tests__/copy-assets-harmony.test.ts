// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

import { copyAssets } from '../commands/copy-assets';

describe('copyAssets HarmonyOS support', () => {
  let work: string;

  beforeEach(() => {
    work = fs.mkdtempSync(path.join(os.tmpdir(), 'spk-copy-assets-'));
    fs.mkdirSync(path.join(work, 'dist'), { recursive: true });
    fs.writeFileSync(path.join(work, 'dist', 'main.lynx.bundle'), 'bundle');
  });

  afterEach(() => {
    fs.rmSync(work, { recursive: true, force: true });
  });

  it('copies bundles to all three native projects by default', async () => {
    fs.mkdirSync(path.join(work, 'harmony'), { recursive: true });
    await copyAssets({ cwd: work });

    for (const destination of [
      'android/app/src/main/assets',
      'ios/LynxResources/Assets',
      'harmony/entry/src/main/resources/rawfile',
    ]) {
      expect(fs.readFileSync(path.join(work, destination, 'main.lynx.bundle'), 'utf8')).toBe('bundle');
    }
  });

  it('does not create a HarmonyOS project in an existing Android/iOS app', async () => {
    await copyAssets({ cwd: work });

    expect(fs.existsSync(path.join(work, 'harmony'))).toBe(false);
  });

  it('honors an explicit HarmonyOS destination even without a generated shell', async () => {
    await copyAssets({ cwd: work, harmonyDest: 'native-harmony/rawfile' });

    expect(fs.readFileSync(path.join(work, 'native-harmony/rawfile/main.lynx.bundle'), 'utf8'))
      .toBe('bundle');
  });
});
