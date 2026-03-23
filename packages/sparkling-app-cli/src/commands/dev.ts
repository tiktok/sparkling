// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import path from 'node:path';
import fs from 'fs-extra';
import { runCommand } from '../utils/exec';
import { ui } from '../utils/ui';
import { isVerboseEnabled, verboseLog } from '../utils/verbose';

export interface DevOptions {
  cwd: string;
  configFile?: string;
}

function createTempLynxConfig(cwd: string, appConfigPath: string): string {
  const tempDir = path.resolve(cwd, '.sparkling');
  fs.ensureDirSync(tempDir);
  const tempConfigPath = path.join(tempDir, 'lynx.config.ts');
  const rel = path.relative(tempDir, path.resolve(appConfigPath)).split(path.sep).join('/');
  const content = [
    `import cfgModule from '${rel.startsWith('.') ? rel : './' + rel}'`,
    'const cfg: any = (cfgModule as any).default ?? cfgModule',
    'export default (cfg.lynxConfig ?? cfg) as any',
  ].join('\n');
  fs.writeFileSync(tempConfigPath, content);
  return tempConfigPath;
}

export async function devProject(options: DevOptions): Promise<void> {
  const configPath = path.resolve(options.cwd, options.configFile ?? 'app.config.ts');
  const tempConfigPath = createTempLynxConfig(options.cwd, configPath);

  if (isVerboseEnabled()) {
    verboseLog(`App config path: ${configPath}`);
    verboseLog(`Temp Lynx config: ${tempConfigPath}`);
  }

  console.log(ui.headline(`Starting dev server with config from ${path.relative(options.cwd, configPath)}`));
  await runCommand('rspeedy', ['dev', '--config', tempConfigPath], { cwd: options.cwd });
}
