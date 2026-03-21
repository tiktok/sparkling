// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import * as p from '@clack/prompts';
import fs from 'node:fs';
import path from 'node:path';

import { defaultLogger } from '../../logger';
import { isEmptyDir } from '../../utils/common';
import type { VariablesMap } from '../../utils/file-templater';
import { ActionRunner } from '../actions/action-runner';
import type { Action, ActionContext, ActionResult } from '../actions/action';
import { checkCancel, UserCancelledError } from './template';
import { copyTemplateWithVariables } from './template';

export interface TemplateStep {
  checkEmpty?: boolean;
  from?: string;
  isMergePackageJson?: boolean;
  override?: boolean;
  packageName?: string;
  postHook?: (config: ProjectBuilderConfig, step: TemplateStep) => Promise<TemplateStep[] | void> | TemplateStep[] | void;
  preHook?: (config: ProjectBuilderConfig, step: TemplateStep) => Promise<TemplateStep[] | void> | TemplateStep[] | void;
  renameFiles?: Record<string, string>;
  skipFiles?: string[];
  to?: string;
  variables?: VariablesMap;
  version?: Record<string, string> | string;
}

export interface ProjectBuilderConfig {
  checkEmpty?: boolean;
  override?: boolean;
  packageName?: string;
  targetDir: string;
  version?: Record<string, string> | string;
}

export class ProjectBuilder {
  private readonly config: ProjectBuilderConfig;
  private readonly steps: TemplateStep[] = [];

  constructor(config: ProjectBuilderConfig) {
    this.config = config;
  }

  static create(config: ProjectBuilderConfig): ProjectBuilder {
    return new ProjectBuilder(config);
  }

  addStep(step: TemplateStep): ProjectBuilder {
    if (!step.from && !step.preHook && !step.postHook) {
      throw new Error('Template step must define a source or hook.');
    }

    this.steps.push(step);
    return this;
  }

  addSteps(steps: TemplateStep[]): ProjectBuilder {
    this.steps.push(...steps);
    return this;
  }

  async build(): Promise<void> {
    if (this.steps.length === 0) {
      throw new Error('No template steps configured.');
    }

    const shouldCheckEmpty = this.config.checkEmpty ?? true;
    if (shouldCheckEmpty && !this.config.override && fs.existsSync(this.config.targetDir) && !isEmptyDir(this.config.targetDir)) {
      const option = await p.select({
        message: `"${path.basename(this.config.targetDir)}" is not empty, please choose:`,
        options: [
          { label: 'Continue and override files', value: 'yes' },
          { label: 'Cancel operation', value: 'no' },
        ],
      });
      checkCancel(option);

      if (option === 'no') {
        throw new UserCancelledError();
      }
    }

    for (let i = 0; i < this.steps.length; i++) {
      await this.executeStepWithHooks(this.steps[i], i === 0);
    }
  }

  async buildWithActionRunner(context: ActionContext): Promise<void> {
    const runner = new ActionRunner(context);
    runner.addAction(this.toSingleAction());
    await runner.run();
  }

  toActions(): Action[] {
    const actions: Action[] = [];
    const shouldCheckEmpty = this.config.checkEmpty ?? true;

    if (shouldCheckEmpty) {
      actions.push(this.createDirectoryCheckAction());
    }

    for (const [index, step] of this.steps.entries()) {
      actions.push(this.createStepAction(step, index));
    }

    return actions;
  }

  toSingleAction(name = 'project-builder', description?: string): Action {
    if (this.steps.length === 0) {
      throw new Error('No template steps configured.');
    }

    const { config } = this;
    const steps = [...this.steps];
    const executeStepWithHooks = this.executeStepWithHooks.bind(this);

    defaultLogger.info(`Building project with ${steps.length} steps`);

    return {
      description: description ?? `Build project with ${steps.length} steps`,
      name,
      async execute(_context: ActionContext): Promise<ActionResult<void>> {
        const shouldCheckEmpty = config.checkEmpty ?? true;

        if (shouldCheckEmpty && !isEmptyDir(config.targetDir)) {
          const shouldContinue = await p.confirm({
            message: `Target directory ${config.targetDir} is not empty. Continue?`,
            initialValue: false,
          });
          checkCancel(shouldContinue);

          if (!shouldContinue) {
            throw new UserCancelledError();
          }
        }

        for (let i = 0; i < steps.length; i++) {
          await executeStepWithHooks(steps[i], i === 0);
        }

        return {
          result: undefined,
          crucialOutputPaths: [config.targetDir],
        };
      },
    };
  }

  private createDirectoryCheckAction(): Action {
    const { targetDir } = this.config;

    return {
      name: 'check-target-directory',
      description: 'Ensure target directory is empty before scaffolding',
      async execute(): Promise<ActionResult<void>> {
        if (fs.existsSync(targetDir) && !isEmptyDir(targetDir)) {
          const shouldContinue = await p.confirm({
            message: `Target directory ${targetDir} is not empty. Continue?`,
            initialValue: false,
          });
          checkCancel(shouldContinue);

          if (!shouldContinue) {
            throw new UserCancelledError();
          }
        }

        return { result: undefined };
      },
    };
  }

  private createStepAction(step: TemplateStep, index: number): Action {
    return {
      name: `template-step-${index + 1}`,
      description: step.from ? `Copy template from ${step.from}` : 'Execute template hook',
      execute: async (context) => {
        await this.executeStepWithHooks(step, index === 0, context);
        return { result: undefined };
      },
    };
  }

  private async executeStepWithHooks(step: TemplateStep, isFirstStep: boolean, context?: ActionContext): Promise<void> {
    const preHookSteps = step.preHook ? await step.preHook(this.config, step) : undefined;
    if (preHookSteps) {
      for (const hookStep of Array.isArray(preHookSteps) ? preHookSteps : [preHookSteps]) {
        await this.executeStepWithHooks(hookStep, isFirstStep, context);
      }
    }

    if (step.from) {
      await this.executeStep(step, isFirstStep);
    }

    const postHookSteps = step.postHook ? await step.postHook(this.config, step) : undefined;
    if (postHookSteps) {
      for (const hookStep of Array.isArray(postHookSteps) ? postHookSteps : [postHookSteps]) {
        await this.executeStepWithHooks(hookStep, false, context);
      }
    }
  }

  private async executeStep(step: TemplateStep, isFirstStep: boolean): Promise<void> {
    if (!step.from) {
      return;
    }

    const targetDir = step.to ? path.resolve(this.config.targetDir, step.to) : this.config.targetDir;

    await copyTemplateWithVariables({
      checkEmpty: isFirstStep ? this.config.checkEmpty : step.checkEmpty,
      from: step.from,
      isMergePackageJson: step.isMergePackageJson,
      override: step.override ?? this.config.override,
      packageName: step.packageName ?? this.config.packageName,
      renameFiles: step.renameFiles,
      skipFiles: step.skipFiles,
      to: targetDir,
      variables: step.variables,
      version: step.version ?? this.config.version,
    });
  }
}
