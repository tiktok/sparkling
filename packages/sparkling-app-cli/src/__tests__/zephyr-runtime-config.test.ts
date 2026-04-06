import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { writeZephyrRuntimeConfig, ZEPHYR_RUNTIME_CONFIG_FILE } from '../commands/zephyr-runtime-config';

describe('writeZephyrRuntimeConfig', () => {
  it('writes a normalized runtime config into dist', () => {
    const cwd = fs.mkdtempSync(path.join(os.tmpdir(), 'sparkling-zephyr-runtime-'));

    const outputPath = writeZephyrRuntimeConfig({
      cwd,
      config: {
        lynxConfig: {},
        zephyr: {
          enabled: true,
          appId: 'demo-app',
          versionUrl: 'demo.zephyrcloud.app/',
          polling: {
            intervalMs: 30_000,
          },
        },
      },
    });

    expect(outputPath).toBe(path.join(cwd, 'dist', ZEPHYR_RUNTIME_CONFIG_FILE));
    const contents = JSON.parse(fs.readFileSync(outputPath!, 'utf8'));
    expect(contents).toEqual({
      version: 1,
      enabled: true,
      appId: 'demo-app',
      strategy: 'next-launch',
      fallback: 'bundled',
      versionUrl: 'https://demo.zephyrcloud.app',
      polling: {
        enabled: true,
        intervalMs: 30_000,
      },
    });
  });

  it('removes stale config when zephyr support is disabled', () => {
    const cwd = fs.mkdtempSync(path.join(os.tmpdir(), 'sparkling-zephyr-runtime-'));
    const distDir = path.join(cwd, 'dist');
    fs.mkdirSync(distDir, { recursive: true });
    const outputPath = path.join(distDir, ZEPHYR_RUNTIME_CONFIG_FILE);
    fs.writeFileSync(outputPath, '{}\n');

    const result = writeZephyrRuntimeConfig({
      cwd,
      config: {
        lynxConfig: {},
      },
    });

    expect(result).toBeNull();
    expect(fs.existsSync(outputPath)).toBe(false);
  });
});
