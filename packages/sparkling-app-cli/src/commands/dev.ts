// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import path from 'node:path';
import {
  createDevLynxConfig,
  getConfiguredDevServerPorts,
  loadAppConfig,
  resolveDevServerPort,
  updateDevServerPortInAppConfig,
} from '../config';
import { runCommand } from '../utils/exec';
import { ui } from '../utils/ui';
import { isVerboseEnabled, verboseLog } from '../utils/verbose';

export interface DevOptions {
  cwd: string;
  configFile?: string;
  port?: number;
  host?: string;
}

export async function devProject(options: DevOptions): Promise<void> {
  const { config, configPath } = await loadAppConfig(options.cwd, options.configFile ?? 'app.config.ts');
  const { devPort, lynxPort } = getConfiguredDevServerPorts(config);
  if (devPort !== undefined && lynxPort !== undefined && devPort !== lynxPort) {
    console.warn(ui.warn(
      `Port config mismatch detected: dev.server.port=${devPort} and lynxConfig.server.port=${lynxPort}. ` +
      `sparkling-app-cli uses dev.server.port (${devPort}).`,
    ));
  }

  const configuredPort = resolveDevServerPort(config);
  const port = options.port ?? configuredPort;

  if (options.port !== undefined && options.port !== configuredPort) {
    const updated = updateDevServerPortInAppConfig(configPath, options.port);
    if (updated) {
      console.log(ui.info(`Updated app.config.ts dev.server.port to ${options.port}.`));
    }
  }

  const tempConfigPath = createDevLynxConfig(options.cwd, configPath, port, options.host);

  if (isVerboseEnabled()) {
    verboseLog(`App config path: ${configPath}`);
    verboseLog(`Temp Lynx config: ${tempConfigPath}`);
    verboseLog(`Dev server port: ${port}`);
    if (options.host) {
      verboseLog(`Dev server host: ${options.host}`);
    }
  }

  console.log(ui.headline(`Starting Rspeedy dev server on port ${port} with config from ${path.relative(options.cwd, configPath)}`));
  await runCommand('rspeedy', ['dev', '--config', tempConfigPath], { cwd: options.cwd });
}
