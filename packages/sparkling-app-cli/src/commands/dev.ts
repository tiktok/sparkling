// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from 'node:fs';
import path from 'node:path';
import { spawn, type ChildProcess } from 'node:child_process';
import { createDevLynxConfig, loadAppConfig } from '../config';
import { DEV_SERVER_PORT } from '../constants';
import { ui } from '../utils/ui';
import { isVerboseEnabled, verboseLog } from '../utils/verbose';

export interface DevOptions {
  cwd: string;
  configFile?: string;
  port?: number;
}

function getEntryKeys(config: { lynxConfig: unknown }): string[] {
  const lynxConfig = config.lynxConfig as Record<string, unknown>;
  const source = lynxConfig?.source as Record<string, unknown> | undefined;
  const entry = source?.entry;
  if (!entry || typeof entry !== 'object') return [];
  return Object.keys(entry).sort();
}

/**
 * Clear require cache for the app config so loadAppConfig picks up fresh content.
 */
function clearConfigRequireCache(cwd: string, configPath: string): void {
  const resolvedConfig = path.resolve(configPath);
  const tempLoader = path.resolve(cwd, '.sparkling', 'app.config.cjs');
  delete require.cache[resolvedConfig];
  delete require.cache[tempLoader];
}

export async function devProject(options: DevOptions): Promise<void> {
  const configFile = options.configFile ?? 'app.config.ts';
  const configPath = path.resolve(options.cwd, configFile);
  const port = options.port ?? DEV_SERVER_PORT;

  if (isVerboseEnabled()) {
    verboseLog(`App config path: ${configPath}`);
    verboseLog(`Dev server port: ${port}`);
  }

  // Load initial config to track entry keys
  let currentEntryKeys: string[];
  try {
    const { config } = await loadAppConfig(options.cwd, configFile);
    currentEntryKeys = getEntryKeys(config);
  } catch {
    currentEntryKeys = [];
  }

  console.log(
    ui.headline(
      `Starting Rspeedy dev server on port ${port} with config from ${path.relative(options.cwd, configPath)}`,
    ),
  );

  let restarting = false;
  let child: ChildProcess | null = null;

  // Watch app.config.ts for entry changes and restart Rspeedy when needed
  let debounceTimer: ReturnType<typeof setTimeout> | undefined;
  const watcher = fs.watch(configPath, () => {
    if (debounceTimer) clearTimeout(debounceTimer);
    debounceTimer = setTimeout(async () => {
      try {
        clearConfigRequireCache(options.cwd, configPath);
        const { config: newConfig } = await loadAppConfig(options.cwd, configFile);
        const newEntryKeys = getEntryKeys(newConfig);

        if (
          newEntryKeys.length === currentEntryKeys.length
          && newEntryKeys.every((key, i) => key === currentEntryKeys[i])
        ) {
          if (isVerboseEnabled()) {
            verboseLog('app.config.ts changed but entries unchanged, skipping restart');
          }
          return;
        }

        console.log(
          ui.headline('Detected entry change in app.config.ts, restarting dev server...'),
        );
        currentEntryKeys = newEntryKeys;
        restarting = true;
        if (child) {
          child.kill('SIGTERM');
        }
      } catch (err) {
        if (isVerboseEnabled()) {
          verboseLog(`Failed to reload app.config.ts: ${err}`);
        }
      }
    }, 300);
  });

  try {
    // Main loop: spawn Rspeedy, restart on entry changes
    while (true) {
      const tempConfigPath = createDevLynxConfig(options.cwd, configPath, port);
      child = spawn('rspeedy', ['dev', '--config', tempConfigPath], {
        cwd: options.cwd,
        env: process.env,
        stdio: 'inherit',
        shell: false,
      });

      await new Promise<void>((resolve, reject) => {
        child!.on('error', reject);
        child!.on('close', (code) => {
          if (restarting) {
            resolve();
          } else if (code) {
            reject(new Error(`rspeedy exited with code ${code}`));
          } else {
            resolve();
          }
        });
      });

      if (!restarting) break;
      restarting = false;
    }
  } finally {
    watcher.close();
    if (debounceTimer) clearTimeout(debounceTimer);
  }
}
