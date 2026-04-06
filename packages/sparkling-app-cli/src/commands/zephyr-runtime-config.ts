import fs from 'node:fs';
import path from 'node:path';
import type { AppConfig, ZephyrConfig } from '../types';

export const ZEPHYR_RUNTIME_CONFIG_FILE = 'sparkling.zephyr.json';
const DEFAULT_POLLING_INTERVAL_MS = 15 * 60 * 1000;

export interface ZephyrRuntimeConfig {
  version: 1;
  enabled: boolean;
  appId?: string;
  channel?: string;
  versionUrl?: string;
  strategy: 'next-launch' | 'prompt-restart';
  fallback: 'bundled';
  polling: {
    enabled: boolean;
    intervalMs: number;
  };
}

export interface WriteZephyrRuntimeConfigOptions {
  cwd: string;
  config: AppConfig;
  sourceDir?: string;
  versionUrlOverride?: string;
}

function normalizeVersionUrl(value?: string): string | undefined {
  const trimmed = value?.trim();
  if (!trimmed) {
    return undefined;
  }
  if (/^https?:\/\//i.test(trimmed)) {
    return trimmed.replace(/\/+$/, '');
  }
  return `https://${trimmed.replace(/^\/+/, '').replace(/\/+$/, '')}`;
}

function createRuntimeConfig(
  zephyr: ZephyrConfig,
  versionUrlOverride?: string
): ZephyrRuntimeConfig {
  return {
    version: 1,
    enabled: zephyr.enabled !== false,
    appId: zephyr.appId,
    channel: zephyr.channel,
    versionUrl: normalizeVersionUrl(versionUrlOverride ?? zephyr.versionUrl),
    strategy: zephyr.strategy ?? 'next-launch',
    fallback: zephyr.fallback ?? 'bundled',
    polling: {
      enabled: zephyr.polling?.enabled !== false,
      intervalMs: zephyr.polling?.intervalMs ?? DEFAULT_POLLING_INTERVAL_MS,
    },
  };
}

export function writeZephyrRuntimeConfig(
  options: WriteZephyrRuntimeConfigOptions
): string | null {
  const outputPath = path.resolve(
    options.cwd,
    options.sourceDir ?? 'dist',
    ZEPHYR_RUNTIME_CONFIG_FILE
  );
  const zephyr = options.config.zephyr;

  if (!zephyr?.enabled) {
    if (fs.existsSync(outputPath)) {
      fs.rmSync(outputPath, { force: true });
    }
    return null;
  }

  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  const runtimeConfig = createRuntimeConfig(zephyr, options.versionUrlOverride);
  fs.writeFileSync(outputPath, `${JSON.stringify(runtimeConfig, null, 2)}\n`);
  return outputPath;
}
