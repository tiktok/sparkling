// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import path from 'node:path';
import { createDevLynxConfig } from '../config';
import { DEV_SERVER_PORT } from '../constants';
import { runCommand } from '../utils/exec';
import { ui } from '../utils/ui';
import { isVerboseEnabled, verboseLog } from '../utils/verbose';

export type DevPlatform = 'web' | 'native' | 'all';

export interface DevOptions {
  cwd: string;
  configFile?: string;
  port?: number;
  platform?: DevPlatform;
}

export async function devProject(options: DevOptions): Promise<void> {
  const configPath = path.resolve(options.cwd, options.configFile ?? 'app.config.ts');
  const port = options.port ?? DEV_SERVER_PORT;
  const platform = options.platform ?? 'all';
  const tempConfigPath = createDevLynxConfig(options.cwd, configPath, port);

  if (isVerboseEnabled()) {
    verboseLog(`App config path: ${configPath}`);
    verboseLog(`Temp Lynx config: ${tempConfigPath}`);
    verboseLog(`Dev server port: ${port}`);
    verboseLog(`Dev platform: ${platform}`);
  }

  const rspeedyArgs = ['dev', '--config', tempConfigPath];

  if (platform === 'web') {
    rspeedyArgs.push('--environment', 'web');
  } else if (platform === 'native') {
    rspeedyArgs.push('--environment', 'lynx');
  }
  // platform === 'all' → no --environment flag → serves all environments

  console.log(ui.headline(`Starting Rspeedy dev server (${platform}) on port ${port} with config from ${path.relative(options.cwd, configPath)}`));
  await runCommand('rspeedy', rspeedyArgs, { cwd: options.cwd });
}
