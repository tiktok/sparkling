// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

import * as p from '@clack/prompts';

import { ProjectBuilder } from '../core/project-builder/project-builder';
import {
  UserCancelledError,
  copyTemplateWithVariables,
  formatProjectName,
  mergePackageJson,
  updatePackageJson,
} from '../core/project-builder/template';

jest.mock('@clack/prompts', () => ({
  confirm: jest.fn(),
  isCancel: jest.fn(() => false),
  select: jest.fn(),
}));

describe('project builder and template utilities', () => {
  let work: string;

  beforeEach(() => {
    work = fs.mkdtempSync(path.join(os.tmpdir(), 'csa-builder-'));
    jest.clearAllMocks();
  });

  afterEach(() => {
    fs.rmSync(work, { recursive: true, force: true });
  });

  function templateDir(name = 'template'): string {
    const dir = path.join(work, name);
    fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(path.join(dir, 'package.json'), JSON.stringify({
      dependencies: { '@sparkling/core': 'workspace:*' },
      devDependencies: { '@sparkling/test': 'workspace:*' },
      name: 'template-name',
      scripts: { start: 'lynx start' },
    }));
    fs.writeFileSync(path.join(dir, 'gitignore'), 'dist\n');
    fs.writeFileSync(path.join(dir, '{{appName}}.txt'), 'hello {{appName}}');
    fs.mkdirSync(path.join(dir, 'node_modules'));
    fs.writeFileSync(path.join(dir, 'node_modules', 'ignored.txt'), 'ignored');
    return dir;
  }

  it('formats project names from paths and scopes', () => {
    expect(formatProjectName('apps/demo/')).toEqual({ packageName: 'demo', targetDir: 'apps/demo' });
    expect(formatProjectName('@scope/app')).toEqual({ packageName: '@scope/app', targetDir: '@scope/app' });
    expect(() => formatProjectName('   ')).toThrow('cannot be empty');
    expect(() => formatProjectName('')).toThrow('non-empty');
  });

  it('copies templates with variable replacement, package name, version, renames, and skips', async () => {
    const from = templateDir();
    const to = path.join(work, 'out');

    await copyTemplateWithVariables({
      from,
      packageName: 'demo-app',
      to,
      variables: { appName: 'Demo' },
      version: '2.1.0',
    });

    expect(JSON.parse(fs.readFileSync(path.join(to, 'package.json'), 'utf8'))).toMatchObject({
      dependencies: { '@sparkling/core': '^2.1.0' },
      devDependencies: { '@sparkling/test': '^2.1.0' },
      name: 'demo-app',
    });
    expect(fs.readFileSync(path.join(to, 'Demo.txt'), 'utf8')).toBe('hello Demo');
    expect(fs.existsSync(path.join(to, '.gitignore'))).toBe(true);
    expect(fs.existsSync(path.join(to, 'node_modules'))).toBe(false);
  });

  it('copies the app template with a unified package namespace in app config', async () => {
    const from = path.resolve(__dirname, '../../../../template/sparkling-app-template');
    const to = path.join(work, 'app-template-out');

    await copyTemplateWithVariables({
      from,
      packageName: 'demo-app',
      skipFiles: ['LICENSE'],
      to,
      variables: {
        appName: 'demo-app',
        appNameCamel: 'DemoApp',
        packageNamespace: 'com.demo.app',
      },
      version: '2.1.0',
    });

    const appConfig = fs.readFileSync(path.join(to, 'app.config.ts'), 'utf8');
    expect(appConfig).toContain("packageName: 'com.demo.app'");
    expect(appConfig).toContain("bundleIdentifier: 'com.demo.app'");
    expect(appConfig).not.toContain('com.example.sparkling.go');
    expect(appConfig).not.toContain('com.sparkling.app.SparklingGo');
    expect(appConfig).not.toContain('{{packageNamespace}}');
  });

  it('keeps prerelease versions exact and updates selected dependency versions', () => {
    const pkg = path.join(work, 'package.json');
    fs.writeFileSync(pkg, JSON.stringify({
      dependencies: { a: 'workspace:*', b: 'workspace:*' },
      devDependencies: { c: 'workspace:*' },
      name: 'old-name',
    }));

    updatePackageJson(pkg, '1.0.0-rc.1', 'new-name');
    expect(JSON.parse(fs.readFileSync(pkg, 'utf8'))).toMatchObject({
      dependencies: { a: '1.0.0-rc.1', b: '1.0.0-rc.1' },
      devDependencies: { c: '1.0.0-rc.1' },
      name: 'new-name',
    });

    updatePackageJson(pkg, { a: '^2.0.0', c: '^3.0.0' });
    expect(JSON.parse(fs.readFileSync(pkg, 'utf8'))).toMatchObject({
      dependencies: { a: '^2.0.0', b: '1.0.0-rc.1' },
      devDependencies: { c: '^3.0.0' },
    });
  });

  it('merges package json files with sorted dependency-like keys', () => {
    const target = path.join(work, 'target.json');
    const extra = path.join(work, 'extra.json');
    fs.writeFileSync(target, JSON.stringify({
      dependencies: { z: '1.0.0' },
      name: 'target',
      scripts: { test: 'jest' },
    }));
    fs.writeFileSync(extra, JSON.stringify({
      dependencies: { a: '1.0.0' },
      devDependencies: { b: '1.0.0' },
      name: 'extra',
      scripts: { build: 'tsc' },
    }));

    mergePackageJson(target, extra);

    expect(JSON.parse(fs.readFileSync(target, 'utf8'))).toEqual({
      dependencies: { a: '1.0.0', z: '1.0.0' },
      devDependencies: { b: '1.0.0' },
      name: 'target',
      scripts: { build: 'tsc', test: 'jest' },
    });
  });

  it('builds configured steps and hook-returned steps', async () => {
    const first = templateDir('first');
    const second = templateDir('second');
    const targetDir = path.join(work, 'project');
    const preHook = jest.fn(() => [{ from: second, to: 'generated', variables: { appName: 'Hook' } }]);
    const postHook = jest.fn();

    await ProjectBuilder.create({
      checkEmpty: false,
      packageName: 'demo',
      targetDir,
      version: '1.2.3',
    })
      .addStep({ from: first, variables: { appName: 'Root' }, preHook, postHook })
      .build();

    expect(fs.existsSync(path.join(targetDir, 'Root.txt'))).toBe(true);
    expect(fs.existsSync(path.join(targetDir, 'generated', 'Hook.txt'))).toBe(true);
    expect(preHook).toHaveBeenCalled();
    expect(postHook).toHaveBeenCalled();
  });

  it('converts a builder to actions and a single action', async () => {
    const targetDir = path.join(work, 'project');
    const builder = ProjectBuilder.create({ checkEmpty: true, targetDir });
    builder.addStep({ async postHook() {} });

    const actions = builder.toActions();
    expect(actions.map(action => action.name)).toEqual(['check-target-directory', 'template-step-1']);

    const single = builder.toSingleAction('custom-builder');
    await expect(single.execute({
      devMode: false,
      environment: 'development',
      logger: { clear() {}, error() {}, info() {}, logFile: null, message() {}, warn() {} },
      projectRoot: work,
    })).resolves.toEqual({
      crucialOutputPaths: [targetDir],
      result: undefined,
    });
  });

  it('rejects invalid steps and empty builders', async () => {
    expect(() => ProjectBuilder.create({ targetDir: work }).addStep({})).toThrow('source or hook');
    await expect(ProjectBuilder.create({ targetDir: work }).build()).rejects.toThrow('No template steps');
    expect(() => ProjectBuilder.create({ targetDir: work }).toSingleAction()).toThrow('No template steps');
  });

  it('honors non-empty directory cancellation', async () => {
    const targetDir = path.join(work, 'non-empty');
    fs.mkdirSync(targetDir);
    fs.writeFileSync(path.join(targetDir, 'existing.txt'), 'x');
    (p.select as jest.Mock).mockResolvedValueOnce('no');

    await expect(
      ProjectBuilder.create({ targetDir })
        .addStep({ from: templateDir() })
        .build(),
    ).rejects.toBeInstanceOf(UserCancelledError);
  });

  it('copies into non-empty directory after confirmation', async () => {
    const targetDir = path.join(work, 'non-empty-yes');
    fs.mkdirSync(targetDir);
    fs.writeFileSync(path.join(targetDir, 'existing.txt'), 'x');
    (p.select as jest.Mock).mockResolvedValueOnce('yes');

    await ProjectBuilder.create({ targetDir })
      .addStep({ from: templateDir(), variables: { appName: 'Confirmed' } })
      .build();

    expect(fs.existsSync(path.join(targetDir, 'Confirmed.txt'))).toBe(true);
  });
});
