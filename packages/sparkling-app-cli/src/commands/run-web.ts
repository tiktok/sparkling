// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import path from 'node:path';
import fs from 'fs-extra';
import { WEB_SHELL_PORT } from '../constants';
import { buildProject } from './build';
import { autolink } from './autolink';
import { runCommand } from '../utils/exec';
import { ui } from '../utils/ui';
import { isVerboseEnabled, verboseLog } from '../utils/verbose';

export interface RunWebOptions {
  cwd: string;
  configFile?: string;
  port?: number;
  open?: boolean;
}

/**
 * Locate the sparkling-web-shell package.
 * Checks node_modules resolution first, then common workspace locations.
 */
function resolveWebShellDir(cwd: string): string | null {
  // Try require.resolve to find the package entry
  try {
    const pkgJsonPath = require.resolve('sparkling-web-shell/package.json', {
      paths: [cwd],
    });
    return path.dirname(pkgJsonPath);
  } catch {
    // Not found via node resolution
  }

  // Fallback: check common workspace sibling locations
  const candidates = [
    path.resolve(cwd, '../sparkling-web-shell'),
    path.resolve(cwd, '../../packages/sparkling-web-shell'),
  ];
  for (const candidate of candidates) {
    if (fs.existsSync(path.join(candidate, 'package.json'))) {
      return candidate;
    }
  }

  return null;
}

export async function runWeb(options: RunWebOptions): Promise<void> {
  const cwd = options.cwd;
  const port = options.port ?? WEB_SHELL_PORT;
  const shouldOpen = options.open !== false;

  // Step 1: Run web autolink to generate web method imports
  console.log(ui.headline('Autolinking web method modules...'));
  await autolink({ cwd, platform: 'web', configFile: options.configFile });

  // Step 2: Build web bundles only
  console.log(ui.headline('Building web bundles...'));
  await buildProject({
    cwd,
    configFile: options.configFile,
    platform: 'web',
    skipCopy: true,
  });

  // Step 3: Resolve sparkling-web-shell location
  const webShellDir = resolveWebShellDir(cwd);
  if (!webShellDir) {
    throw new Error(
      'Could not find sparkling-web-shell. Install it with:\n' +
      '  pnpm add -D sparkling-web-shell',
    );
  }

  if (isVerboseEnabled()) {
    verboseLog(`Web shell resolved at: ${webShellDir}`);
  }

  // Step 4: Verify build output exists
  const distDir = path.resolve(cwd, 'dist/web');
  if (!fs.existsSync(distDir)) {
    throw new Error(`Web build output not found at ${distDir}. Did the build succeed?`);
  }

  // Step 5: Start the web shell dev server pointing at app's web dist
  console.log(ui.headline(`Starting web preview at http://localhost:${port}`));

  await runCommand('npx', ['rsbuild', 'dev'], {
    cwd: webShellDir,
    env: {
      LYNX_BUNDLE_DIR: distDir,
      PORT: String(port),
      BROWSER: shouldOpen ? 'true' : 'none',
    },
  });
}
