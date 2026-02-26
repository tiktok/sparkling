// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import path from 'node:path';
import { createDevLynxConfig } from '../config';
import { DEV_SERVER_PORT } from '../constants';
import { runCommand } from '../utils/exec';
import { ui } from '../utils/ui';
import { isVerboseEnabled, verboseLog } from '../utils/verbose';

export interface DevOptions {
  cwd: string;
  configFile?: string;
  port?: number;
}

export async function devProject(options: DevOptions): Promise<void> {
  const configPath = path.resolve(options.cwd, options.configFile ?? 'app.config.ts');
  const port = options.port ?? DEV_SERVER_PORT;
  const tempConfigPath = createDevLynxConfig(options.cwd, configPath, port);

  if (isVerboseEnabled()) {
    verboseLog(`App config path: ${configPath}`);
    verboseLog(`Temp Lynx config: ${tempConfigPath}`);
    verboseLog(`Dev server port: ${port}`);
  }

  console.log(ui.headline(`Starting Rspeedy dev server on port ${port} with config from ${path.relative(options.cwd, configPath)}`));
  await runCommand('rspeedy', ['dev', '--config', tempConfigPath], { cwd: options.cwd });
}
