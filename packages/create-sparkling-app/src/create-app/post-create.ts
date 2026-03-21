// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import { execSync } from 'node:child_process';
import * as p from '@clack/prompts';
import { ui } from '../ui';
import { createSpinner } from '../utils/spinner';
import { isVerboseEnabled, verboseLog } from '../utils/verbose';

const parsePackageManagerFromUserAgent = (userAgent: string | undefined): string | undefined => {
  if (!userAgent) return undefined;
  const agent = userAgent.toLowerCase();
  if (agent.includes('pnpm')) return 'pnpm';
  if (agent.includes('yarn')) return 'yarn';
  if (agent.includes('bun')) return 'bun';
  if (agent.includes('npm')) return 'npm';
  return undefined;
};

export async function installDependencies(distFolder: string, packageManager?: string): Promise<boolean> {
  if (!packageManager) {
    return false;
  }

  const s = createSpinner();
  s.start(`Installing with ${packageManager}...`);
  const stdioMode = isVerboseEnabled() ? 'inherit' : 'pipe';

  try {
    if (isVerboseEnabled()) {
      verboseLog(`Executing "${packageManager} install" in ${distFolder}`);
    }
    execSync(`${packageManager} install`, {
      cwd: distFolder,
      stdio: stdioMode,
    });
    s.stop(`Installed with ${packageManager}`);
    return true;
  } catch (error) {
    s.stop(`Failed to install dependencies with ${packageManager}`);
    if (isVerboseEnabled()) {
      verboseLog(`Install failed: ${error instanceof Error ? error.message : String(error)}`);
    }
    p.log.warn(`Failed to install dependencies. Run '${packageManager} install' manually.`);
    return false;
  }
}

export async function initializeGitRepo(distFolder: string): Promise<void> {
  const stdioMode = isVerboseEnabled() ? 'inherit' : 'pipe';
  const s = createSpinner();
  try {
    try {
      execSync('git --version', { stdio: stdioMode });
    } catch {
      p.log.warn('Git is not installed or not in PATH. Skipping git initialization.');
      return;
    }

    s.start('Initializing git repository...');
    if (isVerboseEnabled()) {
      verboseLog(`Initializing git repository in ${distFolder}`);
    }
    execSync('git init', { cwd: distFolder, stdio: stdioMode });
    s.stop('Initialized git repository');
  } catch (error) {
    s.stop('Failed to initialize git repository');
    p.log.warn('Failed to initialize git repository. Run "git init" manually.');
  }
}

export function detectPackageManager(): string {
  const fromUserAgent = parsePackageManagerFromUserAgent(process.env.npm_config_user_agent);
  if (fromUserAgent) {
    if (isVerboseEnabled()) {
      verboseLog(`Detected package manager from user agent: ${fromUserAgent}`);
    }
    return fromUserAgent;
  }

  try {
    execSync('pnpm --version', { stdio: 'ignore' });
    const pm = 'pnpm';
    if (isVerboseEnabled()) {
      verboseLog(`Detected package manager by probing executables: ${pm}`);
    }
    return pm;
  } catch {
    try {
      execSync('yarn --version', { stdio: 'ignore' });
      const pm = 'yarn';
      if (isVerboseEnabled()) {
        verboseLog(`Detected package manager by probing executables: ${pm}`);
      }
      return pm;
    } catch {
      try {
        execSync('bun --version', { stdio: 'ignore' });
        const pm = 'bun';
        if (isVerboseEnabled()) {
          verboseLog(`Detected package manager by probing executables: ${pm}`);
        }
        return pm;
      } catch {
        const pm = 'npm';
        if (isVerboseEnabled()) {
          verboseLog(`Defaulting package manager to ${pm}`);
        }
        return pm;
      }
    }
  }
}

export function showCompletionNotes(targetDir: string, packageManager?: string, didInstall = false): void {
  const formatScriptCommand = (script: string) => {
    const pm = packageManager ?? 'npm';
    return pm === 'npm' ? `${pm} run ${script}` : `${pm} ${script}`;
  };

  const nextSteps = [`cd ${targetDir}`];

  if (didInstall) {
    nextSteps.push('Dependencies installed. If you change tooling, re-run install manually.');
  } else {
    nextSteps.push(`Install dependencies: ${formatScriptCommand('install')}`);
  }

  nextSteps.push(formatScriptCommand('run:ios'));
  nextSteps.push(formatScriptCommand('run:android'));

  const tips = [
    'iOS: ensure Xcode Command Line Tools are installed.',
    'Android: ensure ANDROID_HOME and SDK platforms are set.',
  ];

  p.note(
    [...nextSteps, '', ...tips.map(t => ui.tip(t))].join('\n'),
    `✔ Project created at ${targetDir}`,
  );
}
