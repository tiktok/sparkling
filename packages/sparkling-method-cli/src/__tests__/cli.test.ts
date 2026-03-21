// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import fs from 'fs-extra';
import os from 'os';
import path from 'path';
import * as p from '@clack/prompts';
import { runInit } from '../create';
import { runCodegen } from '../codegen';

jest.mock('@clack/prompts', () => ({
  text: jest.fn(),
  select: jest.fn(),
  confirm: jest.fn(),
  multiselect: jest.fn(),
  isCancel: jest.fn(() => false),
}));

beforeEach(() => {
  jest.clearAllMocks();
});

async function withTempDir(fn: (dir: string) => Promise<void>): Promise<void> {
  const tempDir = await fs.mkdtemp(path.join(os.tmpdir(), 'sparkling-method-cli-test-'));
  const originalCwd = process.cwd();
  process.chdir(tempDir);
  try {
    await fn(tempDir);
  } finally {
    process.chdir(originalCwd);
    await fs.remove(tempDir);
  }
}

describe('sparkling-method cli', () => {
  it('initializes a project from the template', async () => {
    await withTempDir(async (cwd) => {
      const templateDir = path.join(cwd, 'template');
      await fs.ensureDir(path.join(templateDir, 'src'));
      await fs.writeFile(path.join(templateDir, 'src', 'method.d.ts'), '// placeholder');

      (p.text as jest.Mock)
        .mockResolvedValueOnce('com.example.toast')
        .mockResolvedValueOnce('Demo');
      (p.select as jest.Mock).mockResolvedValueOnce('kts');

      await runInit('toast-module', { template: templateDir });

      const projectDir = path.join(cwd, 'toast-module');
      const pkgJson = await fs.readJson(path.join(projectDir, 'package.json'));
      expect(pkgJson.name).toBe('toast-module');

      const moduleConfig = await fs.readJson(path.join(projectDir, 'module.config.json'));
      expect(moduleConfig.packageName).toBe('com.example.toast');
      expect(moduleConfig.moduleName).toBe('Demo');
      expect(moduleConfig.androidDsl).toBe('kts');

      const androidDir = path.join(projectDir, 'android', 'src', 'main', 'java', 'com', 'example', 'toast', 'Demo');
      const iosDir = path.join(projectDir, 'ios', 'Source', 'Core', 'Demo');
      await expect(fs.pathExists(androidDir)).resolves.toBe(true);
      await expect(fs.pathExists(iosDir)).resolves.toBe(true);
    });
  });

  it('prompts for project name when omitted', async () => {
    await withTempDir(async (cwd) => {
      const templateDir = path.join(cwd, 'template');
      await fs.ensureDir(path.join(templateDir, 'src'));
      await fs.writeFile(path.join(templateDir, 'src', 'method.d.ts'), '// placeholder');

      // promptProjectName calls text once, then promptModuleInfo calls text twice + select once
      (p.text as jest.Mock)
        .mockResolvedValueOnce('toast-module')
        .mockResolvedValueOnce('com.example.toast')
        .mockResolvedValueOnce('Demo');
      (p.select as jest.Mock).mockResolvedValueOnce('kts');

      await runInit(undefined, { template: templateDir });

      const projectDir = path.join(cwd, 'toast-module');
      await expect(fs.pathExists(projectDir)).resolves.toBe(true);
    });
  });

  it('generates metadata and native stubs from definitions', async () => {
    await withTempDir(async (cwd) => {
      await fs.writeJson(path.join(cwd, 'module.config.json'), {
        packageName: 'com.example.toast',
        moduleName: 'demo'
      });

      const srcDir = path.join(cwd, 'src');
      await fs.ensureDir(srcDir);
      await fs.writeFile(
        path.join(srcDir, 'method.d.ts'),
        `interface ShowToastRequest {\n  message: string;\n  /** @default 2000 */\n  duration?: number;\n}\n\ninterface ShowToastResponse { success: boolean; }\n\ndeclare function showToast(params: ShowToastRequest, callback: (res: ShowToastResponse) => void): void;`
      );

      await runCodegen({ src: 'src' });

      const metadataPath = path.join(cwd, 'generated', 'metadata', 'showToast.json');
      const metadata = await fs.readJson(metadataPath);
      expect(metadata.name).toBe('showToast');
      expect(metadata.moduleName).toBe('demo');

      const kotlinPath = path.join(
        cwd,
        'android',
        'src',
        'main',
        'java',
        'com',
        'example',
        'toast',
        'demo',
        'showtoast',
        'AbsShowToastMethodIDL.kt'
      );
      const swiftPath = path.join(cwd, 'ios', 'Source', 'Core', 'Demo', 'ShowToast', 'ShowToastIDL.swift');

      await expect(fs.pathExists(kotlinPath)).resolves.toBe(true);
      await expect(fs.pathExists(swiftPath)).resolves.toBe(true);

      const kotlinContent = await fs.readFile(kotlinPath, 'utf8');
      expect(kotlinContent).toContain('AbsShowToastMethodIDL');
      expect(kotlinContent).toContain('duration');

      const swiftContent = await fs.readFile(swiftPath, 'utf8');
      expect(swiftContent).toContain('class SPKShowToastMethodResultModel');
    });
  });
});
