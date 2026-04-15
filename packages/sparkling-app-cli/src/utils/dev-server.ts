// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import path from 'node:path';
import { spawn } from 'node:child_process';
import { DEV_SERVER_PORT } from '../constants';
import { ui } from './ui';
import { isVerboseEnabled, verboseLog } from './verbose';

function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function pingDevServer(port: number): Promise<boolean> {
  const hosts = ['127.0.0.1', 'localhost'];
  for (const host of hosts) {
    if (await pingDevServerHost(host, port)) {
      return true;
    }
  }
  return false;
}

async function pingDevServerHost(host: string, port: number): Promise<boolean> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 1000);
  try {
    await fetch(`http://${host}:${port}/`, {
      method: 'GET',
      signal: controller.signal,
    });
    return true;
  } catch {
    return false;
  } finally {
    clearTimeout(timer);
  }
}

export async function isDevServerRunning(port = DEV_SERVER_PORT): Promise<boolean> {
  return pingDevServer(port);
}

export async function ensureDevServerRunning(cwd: string, port = DEV_SERVER_PORT, host = '127.0.0.1'): Promise<void> {
  if (await isDevServerRunning(port)) {
    if (isVerboseEnabled()) {
      verboseLog(`Detected running sparkling dev server on port ${port}.`);
    }
    return;
  }

  const cliEntry = process.argv[1];
  if (!cliEntry) {
    throw new Error('Unable to determine sparkling-app-cli entry script from argv.');
  }

  const cliPath = path.resolve(cliEntry);
  console.log(ui.info(`No sparkling dev server found on port ${port}; starting one...`));

  const isDarwin = process.platform === 'darwin';
  const command = isDarwin ? 'script' : process.execPath;
  const args = isDarwin
    ? ['-q', '/dev/null', process.execPath, cliPath, 'dev', '--port', String(port), '--host', host]
    : [cliPath, 'dev', '--port', String(port), '--host', host];

  const child = spawn(command, args, {
    cwd,
    detached: true,
    stdio: 'ignore',
    env: process.env,
  });
  child.unref();

  for (let i = 0; i < 30; i += 1) {
    if (await isDevServerRunning(port)) {
      console.log(ui.success(`Sparkling dev server is running on port ${port}.`));
      return;
    }
    await sleep(500);
  }

  console.warn(ui.warn(
    `Timed out waiting for sparkling dev server on port ${port}. ` +
    'run:ios/run:android will continue, but localhost bundle loading may fail.',
  ));
}
