// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import { Command } from 'commander';

import { createSparklingApp, type CreateAppFlags } from './create-app';
import { enableVerboseLogging } from './utils/verbose';

function parseFlags(argv: string[]): { name?: string; flags: CreateAppFlags } {
  const program = new Command();
  program
    .exitOverride()
    .argument('[name]')
    .option('-t, --template <name>', 'Template name or path')
    .option('--template-version <version>', 'Template version or tag')
    .option('--with-zephyr', 'Scaffold Zephyr deploy support')
    .option('-y, --yes', 'Skip all prompts')
    .option('-f, --force', 'Force overwrite existing directory')
    .option('--pm <pm>', 'Package manager to use')
    .option('--install', 'Install dependencies after creation')
    .option('--no-install', 'Skip installing dependencies after creation')
    .option('--git', 'Initialize git repository')
    .option('--no-git', 'Skip git initialization')
    .option('--namespace <namespace>', 'Android package / iOS bundle id')
    .option('--app-id <id>', 'Alias for namespace')
    .option('-v, --verbose', 'Enable verbose logging');

  const parsed = program.parse(argv, { from: 'user' });
  const opts = parsed.opts<{
    template?: string;
    templateVersion?: string;
    withZephyr?: boolean;
    yes?: boolean;
    force?: boolean;
    pm?: string;
    install?: boolean;
    git?: boolean;
    namespace?: string;
    appId?: string;
    verbose?: boolean;
  }>();

  const [name] = parsed.args as string[];

  const flags: CreateAppFlags = {
    template: opts.template,
    withZephyr: opts.withZephyr,
    yes: opts.yes,
    force: opts.force,
    pm: opts.pm,
    install: opts.install,
    git: opts.git,
    namespace: opts.namespace,
    'app-id': opts.appId,
    templateVersion: opts.templateVersion,
    verbose: opts.verbose,
  };

  return { name, flags };
}

export default async function init(argv: string[] = [], options?: { verbose?: boolean }): Promise<void> {
  const { name, flags } = parseFlags(argv);
  if (options?.verbose && !flags.verbose) {
    flags.verbose = true;
  }

  if (flags.verbose) {
    enableVerboseLogging(true);
  }

  await createSparklingApp({
    args: { name },
    flags,
    cwd: process.cwd(),
  });
}
