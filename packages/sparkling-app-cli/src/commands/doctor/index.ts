// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import chalk from 'chalk';
import { ui } from '../../utils/ui';
import { verboseLog } from '../../utils/verbose';
import {
  checkNodeVersion,
  checkJdk,
  checkAndroidSdk,
  checkAdb,
  checkXcode,
  checkCocoaPods,
  checkSimulator,
  checkWebCore,
  checkWebShell,
} from './checks';
import type { CheckResult } from './types';

const STATUS_ICON: Record<string, string> = {
  pass: chalk.green('✓'),
  fail: chalk.red('✗'),
  warn: chalk.yellow('!'),
  skip: chalk.dim('-'),
};

function formatCheckLine(result: CheckResult): string {
  const icon = STATUS_ICON[result.status] ?? '?';
  let detail = result.name;

  if (result.version) {
    detail += ` ${result.version}`;
  }

  if (result.expected && (result.status === 'pass' || result.status === 'fail')) {
    detail += chalk.dim(` (required: ${result.expected})`);
  }

  if (result.status === 'fail' && result.message) {
    detail += ` — ${result.message}`;
  } else if (result.status === 'warn' && result.message) {
    detail += ` — ${result.message}`;
  } else if (result.status === 'skip' && result.message) {
    detail += chalk.dim(` — ${result.message}`);
  } else if (result.status === 'pass' && result.message && !result.version) {
    detail += chalk.dim(` — ${result.message}`);
  }

  return `  [${icon}] ${detail}`;
}

function buildAgentPrompt(failed: CheckResult[]): string {
  const lines: string[] = [];
  lines.push('My Sparkling project environment has the following issues:');
  failed.forEach((r, i) => {
    lines.push(`${i + 1}. ${r.fixHint ?? r.message ?? `${r.name} check failed`}`);
  });
  lines.push('Please help me fix my environment so I can build and run a Sparkling app.');
  return lines.join('\n');
}

export interface DoctorOptions {
  platform: 'android' | 'ios' | 'web' | 'all';
}

export async function doctor(opts: DoctorOptions): Promise<void> {
  const { platform } = opts;

  console.log('');
  console.log(ui.headline('Sparkling Doctor'));
  console.log(ui.headline('================'));

  const generalResults: CheckResult[] = [];
  generalResults.push(checkNodeVersion());

  console.log('');
  console.log(ui.info('General:'));
  for (const r of generalResults) {
    console.log(formatCheckLine(r));
  }

  const androidResults: CheckResult[] = [];
  if (platform === 'android' || platform === 'all') {
    androidResults.push(checkJdk());
    androidResults.push(checkAndroidSdk());
    androidResults.push(checkAdb());

    console.log('');
    console.log(ui.info('Android:'));
    for (const r of androidResults) {
      console.log(formatCheckLine(r));
    }
  }

  const iosResults: CheckResult[] = [];
  if (platform === 'ios' || platform === 'all') {
    iosResults.push(checkXcode());
    iosResults.push(checkCocoaPods());
    iosResults.push(checkSimulator());

    console.log('');
    console.log(ui.info('iOS:'));
    for (const r of iosResults) {
      console.log(formatCheckLine(r));
    }
  }

  const webResults: CheckResult[] = [];
  if (platform === 'web' || platform === 'all') {
    webResults.push(checkWebCore());
    webResults.push(checkWebShell());

    console.log('');
    console.log(ui.info('Web:'));
    for (const r of webResults) {
      console.log(formatCheckLine(r));
    }
  }

  const allResults = [...generalResults, ...androidResults, ...iosResults, ...webResults];
  const failed = allResults.filter((r) => r.status === 'fail');
  const warned = allResults.filter((r) => r.status === 'warn');

  console.log('');
  console.log('────────────────────────────────────────');

  if (failed.length === 0 && warned.length === 0) {
    console.log(ui.success('All checks passed! Your environment is ready to build a Sparkling app.'));
  } else {
    const issueCount = failed.length + warned.length;
    console.log(
      ui.warn(`${issueCount} issue${issueCount > 1 ? 's' : ''} found.`),
    );

    const promptItems = [...failed, ...warned];
    const prompt = buildAgentPrompt(promptItems);

    console.log('');
    console.log(
      `To fix them, copy the following prompt into your ${chalk.bold('Coding Agent')} (e.g. Cursor):`,
    );
    console.log('');

    const promptLines = prompt.split('\n');
    const maxLen = Math.max(...promptLines.map((l) => l.length));
    const boxWidth = maxLen + 4;
    const top = '┌' + '─'.repeat(boxWidth) + '┐';
    const bottom = '└' + '─'.repeat(boxWidth) + '┘';

    console.log(chalk.dim(top));
    for (const line of promptLines) {
      console.log(chalk.dim('│') + '  ' + line + ' '.repeat(maxLen - line.length) + '  ' + chalk.dim('│'));
    }
    console.log(chalk.dim(bottom));
  }

  console.log('');

  verboseLog(`Doctor completed: ${failed.length} failed, ${warned.length} warnings, ${allResults.length} total checks`);

  if (failed.length > 0) {
    process.exitCode = 1;
  }
}
