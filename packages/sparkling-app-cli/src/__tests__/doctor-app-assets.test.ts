// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
// @ts-nocheck
/// <reference types="jest" />
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { checkAppConfigAssets } from '../commands/doctor/checks';

const PNG_MAGIC = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);

function appDir(assets: Record<string, Buffer | null>, config: string): string {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'sparkling-doctor-'));
  fs.writeFileSync(path.join(dir, 'app.config.ts'), config, 'utf8');
  fs.mkdirSync(path.join(dir, 'resource'), { recursive: true });
  for (const [name, bytes] of Object.entries(assets)) {
    if (bytes) {
      fs.writeFileSync(path.join(dir, 'resource', name), bytes);
    }
  }
  return dir;
}

const CONFIG = `const config = {
  lynxConfig: {},
  appIcon: './resource/app_icon.png',
  plugin: [['splash-screen', { image: './resource/splash.png', dark: { image: './resource/splash_dark.png' } }]],
};
export default config;
`;

describe('doctor: app.config.ts assets', () => {
  it('skips outside a Sparkling app', async () => {
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'sparkling-doctor-'));
    const result = await checkAppConfigAssets(dir);
    expect(result.status).toBe('skip');
  });

  it('passes when every referenced image is a real PNG', async () => {
    const dir = appDir(
      {
        'app_icon.png': PNG_MAGIC,
        'splash.png': PNG_MAGIC,
        'splash_dark.png': PNG_MAGIC,
      },
      CONFIG,
    );
    const result = await checkAppConfigAssets(dir);
    expect(result.status).toBe('pass');
  });

  it('fails on a file that is named .png but is not one', async () => {
    // The shape the template shipped: a PNG read as text and re-encoded, so the
    // 0x89 signature byte became the UTF-8 replacement character.
    const mangled = Buffer.concat([Buffer.from([0xef, 0xbf, 0xbd]), Buffer.from('PNG\r\n\x1a\n')]);
    const dir = appDir(
      {
        'app_icon.png': mangled,
        'splash.png': PNG_MAGIC,
        'splash_dark.png': PNG_MAGIC,
      },
      CONFIG,
    );
    const result = await checkAppConfigAssets(dir);
    expect(result.status).toBe('fail');
    expect(result.message).toContain('app_icon.png is not a PNG');
  });

  it('fails on a referenced file that does not exist', async () => {
    const dir = appDir({ 'app_icon.png': PNG_MAGIC, 'splash.png': PNG_MAGIC }, CONFIG);
    const result = await checkAppConfigAssets(dir);
    expect(result.status).toBe('fail');
    expect(result.message).toContain('splash_dark.png is missing');
  });
});
