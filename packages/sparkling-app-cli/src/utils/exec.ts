// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import { spawn } from 'node:child_process';
import { isVerboseEnabled, verboseLog } from './verbose';

export interface RunCommandOptions {
  cwd?: string;
  env?: NodeJS.ProcessEnv;
  ignoreFailure?: boolean;
  /** Kill the process after this many milliseconds. Ignored when undefined. */
  timeoutMs?: number;
}

export async function runCommand(
  command: string,
  args: string[],
  options: RunCommandOptions = {},
): Promise<void> {
  if (isVerboseEnabled()) {
    const envKeys = Object.keys(options.env ?? {});
    const cwd = options.cwd ?? process.cwd();
    const envLabel = envKeys.length ? ` env:${envKeys.join(',')}` : '';
    const timeoutLabel = options.timeoutMs != null ? ` timeout:${options.timeoutMs}ms` : '';
    verboseLog(`Running "${command} ${args.join(' ')}" (cwd: ${cwd})${envLabel}${timeoutLabel}`);
  }

  const child = spawn(command, args, {
    cwd: options.cwd,
    env: { ...process.env, ...options.env },
    stdio: 'inherit',
    shell: process.platform === 'win32' ? true : false,
  });

  await new Promise<void>((resolve, reject) => {
    let timedOut = false;
    let timer: ReturnType<typeof setTimeout> | undefined;

    if (options.timeoutMs != null) {
      timer = setTimeout(() => {
        timedOut = true;
        child.kill('SIGTERM');
        // Give the process a moment to exit gracefully, then force-kill
        setTimeout(() => {
          if (!child.killed) child.kill('SIGKILL');
        }, 3000);
      }, options.timeoutMs);
    }

    child.on('error', (err) => {
      if (timer) clearTimeout(timer);
      reject(err);
    });

    child.on('close', code => {
      if (timer) clearTimeout(timer);
      if (isVerboseEnabled()) {
        verboseLog(`${command} exited with code ${code ?? 0}${timedOut ? ' (timed out)' : ''}`);
      }
      if (timedOut) {
        const err = new Error(`${command} timed out after ${options.timeoutMs}ms`);
        if (options.ignoreFailure) {
          resolve();
        } else {
          reject(err);
        }
        return;
      }
      if (code && !options.ignoreFailure) {
        reject(new Error(`${command} exited with code ${code}`));
        return;
      }
      resolve();
    });
  });
}
